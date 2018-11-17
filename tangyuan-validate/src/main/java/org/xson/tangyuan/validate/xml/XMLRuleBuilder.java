package org.xson.tangyuan.validate.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xson.tangyuan.TangYuanContainer;
import org.xson.tangyuan.log.Log;
import org.xson.tangyuan.log.LogFactory;
import org.xson.tangyuan.util.StringUtils;
import org.xson.tangyuan.util.TangYuanUtil;
import org.xson.tangyuan.validate.Checker;
import org.xson.tangyuan.validate.Rule;
import org.xson.tangyuan.validate.RuleDef;
import org.xson.tangyuan.validate.RuleEnum;
import org.xson.tangyuan.validate.RuleGroup;
import org.xson.tangyuan.validate.RuleGroupItem;
import org.xson.tangyuan.validate.TypeEnum;
import org.xson.tangyuan.validate.ValidateComponent;
import org.xson.tangyuan.xml.XPathParser;
import org.xson.tangyuan.xml.XmlNodeWrapper;
import org.xson.tangyuan.xml.XmlParseException;

public class XMLRuleBuilder {

	private Log						log					= LogFactory.getLog(getClass());
	private XPathParser				parser				= null;
	private String					ns					= "";
	private XmlNodeWrapper			root				= null;
	private Map<String, RuleDef>	localDefMap			= new HashMap<String, RuleDef>();
	private Map<String, RuleDef>	globleDefMap		= null;
	private Map<String, RuleGroup>	ruleGroupMap		= null;
	private Map<String, Checker>	customCheckerMap	= null;

	public XMLRuleBuilder(InputStream inputStream, Map<String, RuleDef> globleDefMap, Map<String, RuleGroup> ruleGroupMap,
			Map<String, Checker> customCheckerMap) {
		this.globleDefMap = globleDefMap;
		this.ruleGroupMap = ruleGroupMap;
		this.customCheckerMap = customCheckerMap;
		this.parser = new XPathParser(inputStream);
		this.root = this.parser.evalNode("/validate");
		this.ns = this.root.getStringAttribute("ns", "");
	}

	public void parseDefNode() {
		buildDefNodes(this.root.evalNodes("def"));
	}

	private void addDefNode(RuleDef ruleDef) {
		localDefMap.put(ruleDef.getId(), ruleDef);
		globleDefMap.put(getFullId(ruleDef.getId()), ruleDef);
		log.info("Add <def> :" + ruleDef.getId());
	}

	public void parseRuleGroupNode() {
		buildRuleGroupNodes(this.root.evalNodes("ruleGroup"));
	}

	private void addRuleGroup(RuleGroup ruleGroup) {
		ruleGroupMap.put(getFullId(ruleGroup.getId()), ruleGroup);
		log.info("Add <ruleGroup> :" + ruleGroup.getId());
	}

	private void buildDefNodes(List<XmlNodeWrapper> contexts) {
		// List<RuleDef> list = new ArrayList<RuleDef>();
		for (XmlNodeWrapper context : contexts) {
			String id = StringUtils.trim(context.getStringAttribute("id")); // xml validation
			checkDefId(id);
			List<XmlNodeWrapper> ruleNodes = context.evalNodes("rule");
			if (ruleNodes.size() < 1) {
				throw new XmlParseException("<def> node is empty: " + id);
			}
			List<Rule> ruleList = new ArrayList<>();
			for (XmlNodeWrapper ruleNode : ruleNodes) {
				Rule rule = null;
				String checkerId = StringUtils.trim(ruleNode.getStringAttribute("checker"));
				if (null == checkerId) {
					String name = StringUtils.trim(ruleNode.getStringAttribute("name"));
					RuleEnum ruleEnum = RuleEnum.getEnum(name);
					if (null == ruleEnum) {
						throw new XmlParseException("Invalid rule: " + name);
					}
					String value = StringUtils.trim(ruleNode.getStringAttribute("value"));
					if (null == value) {
						value = getNodeBody(ruleNode);
					}
					if (null == value) {
						throw new XmlParseException("Invalid rule value.");
					}
					// rule = new Rule(name, value);
					rule = new Rule(ruleEnum.getEnValue(), value);
				} else {
					// checkerId
					Checker checker = this.customCheckerMap.get(checkerId);
					if (null == checker) {
						throw new XmlParseException("Reference checker does not exist: " + checkerId);
					}
					rule = new Rule(checker);
				}
				ruleList.add(rule);
			}
			RuleDef ruleDef = new RuleDef(id, ruleList);
			addDefNode(ruleDef);
		}
	}

	private String getNodeBody(XmlNodeWrapper ruleNode) {
		NodeList children = ruleNode.getNode().getChildNodes();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < children.getLength(); i++) {
			XmlNodeWrapper child = ruleNode.newXMlNode(children.item(i));
			if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
				String data = child.getStringBody("").trim();
				if (data.length() > 0) {
					builder.append(data);
				}
			}
		}
		String value = builder.toString();
		if (value.length() > 0) {
			return value;
		}
		return null;
	}

	private void buildRuleGroupNodes(List<XmlNodeWrapper> contexts) {
		for (XmlNodeWrapper context : contexts) {
			String id = StringUtils.trim(context.getStringAttribute("id")); // xml validation
			checkRuleGroupId(id);

			String groupDesc = StringUtils.trim(context.getStringAttribute("desc"));
			String groupMessage = StringUtils.trim(context.getStringAttribute("message"));
			if (null == groupMessage || 0 == groupMessage.length()) {
				groupMessage = ValidateComponent.getInstance().getErrorMessage();
			}
			String _groupCode = StringUtils.trim(context.getStringAttribute("code"));
			int groupCode = ValidateComponent.getInstance().getErrorCode();
			if (null != _groupCode) {
				groupCode = Integer.parseInt(_groupCode);
			}

			List<XmlNodeWrapper> itemNodes = context.evalNodes("item");
			if (itemNodes.size() < 1) {
				throw new XmlParseException("<item> node is empty: " + id);
			}

			List<RuleGroupItem> ruleGroupItemList = new ArrayList<RuleGroupItem>();

			for (XmlNodeWrapper itemNode : itemNodes) {
				String fieldName = StringUtils.trim(itemNode.getStringAttribute("name"));// xml validation
				String _type = StringUtils.trim(itemNode.getStringAttribute("type"));// xml validation
				TypeEnum fieldType = TypeEnum.getEnum(_type);// xml validation
				if (null == fieldType) {
					throw new XmlParseException("Invalid fieldType: " + _type);
				}
				String _require = StringUtils.trim(itemNode.getStringAttribute("require"));
				boolean require = true;
				if (null != _require) {
					require = Boolean.parseBoolean(_require);
				}

				List<Rule> ruleList = new ArrayList<Rule>();

				List<XmlNodeWrapper> ruleNodes = itemNode.evalNodes("rule");
				if (ruleNodes.size() > 0) {
					for (XmlNodeWrapper ruleNode : ruleNodes) {
						Rule rule = null;
						String checkerId = StringUtils.trim(ruleNode.getStringAttribute("checker"));
						if (null == checkerId) {
							String name = StringUtils.trim(ruleNode.getStringAttribute("name"));
							RuleEnum ruleEnum = RuleEnum.getEnum(name);
							if (null == ruleEnum) {
								throw new XmlParseException("Invalid rule: " + name);
							}
							String value = StringUtils.trim(ruleNode.getStringAttribute("value"));
							if (null == value) {
								value = getNodeBody(ruleNode);
							}
							if (null == value) {
								throw new XmlParseException("Invalid rule value.");
							}
							// rule = new Rule(name, value);
							rule = new Rule(ruleEnum.getEnValue(), value);
							rule.parseValue(fieldType, ruleEnum, id + ":" + name);
						} else {
							// checkerId
							Checker checker = this.customCheckerMap.get(checkerId);
							if (null == checker) {
								throw new XmlParseException("Reference checker does not exist: " + checkerId);
							}
							rule = new Rule(checker);
						}
						ruleList.add(rule);
					}
				}

				String ref = StringUtils.trim(itemNode.getStringAttribute("ref"));
				if (null != ref) {
					List<RuleDef> ruleDefList = getRuleDefs(ref, id);
					copyRule(ruleList, ruleDefList, fieldType);// 复制RULE
				}

				// 允许没有规则的验证

				String defaultValue = StringUtils.trim(itemNode.getStringAttribute("defaultValue"));
				String itemDesc = StringUtils.trim(itemNode.getStringAttribute("desc"));
				String itemMessage = StringUtils.trim(itemNode.getStringAttribute("message"));
				String _itemCode = StringUtils.trim(itemNode.getStringAttribute("code"));

				if (null == itemMessage || 0 == itemMessage.length()) {
					itemMessage = groupMessage;
				}

				int itemCode = groupCode;
				if (null != _itemCode) {
					itemCode = Integer.parseInt(_itemCode);
				}

				RuleGroupItem ruleGroupItem = new RuleGroupItem(fieldName, fieldType, ruleList, require, defaultValue, itemDesc, itemMessage,
						itemCode);
				ruleGroupItemList.add(ruleGroupItem);
			}
			RuleGroup ruleGroup = new RuleGroup(id, ruleGroupItemList, groupDesc, groupMessage, groupCode);
			addRuleGroup(ruleGroup);
		}
	}

	private List<RuleDef> getRuleDefs(String ref, String id) {
		List<RuleDef> list = new ArrayList<RuleDef>();
		String[] array = ref.split(",");
		for (String temp : array) {
			String refId = temp.trim();
			RuleDef ruleDef = null;
			if (temp.indexOf(TangYuanContainer.getInstance().getNsSeparator()) == -1) {
				ruleDef = this.localDefMap.get(refId);
			} else {
				ruleDef = this.globleDefMap.get(refId);
			}
			if (null == ruleDef) {
				throw new XmlParseException("Ref does not exist: " + refId + ", in ruleGroup: " + id);
			}
			list.add(ruleDef);
		}
		if (list.size() == 0) {
			throw new XmlParseException("Ref does not exist: " + ref + ", in ruleGroup: " + id);
		}
		return list;
	}

	private String getFullId(String id) {
		return TangYuanUtil.getQualifiedName(this.ns, id, null, TangYuanContainer.getInstance().getNsSeparator());
	}

	private void checkDefId(String id) {
		if (null != this.localDefMap.get(id)) {
			throw new XmlParseException("Duplicate <def>: " + id);
		}
		if (null != this.globleDefMap.get(getFullId(id))) {
			throw new XmlParseException("Duplicate <def>: " + id);
		}
	}

	private void checkRuleGroupId(String id) {
		if (null != this.ruleGroupMap.get(getFullId(id))) {
			throw new XmlParseException("Duplicate <ruleGroup>: " + id);
		}
	}

	private void copyRule(List<Rule> ruleList, List<RuleDef> ruleDefList, TypeEnum fieldType) {
		for (RuleDef ruleDef : ruleDefList) {
			List<Rule> srcRuleList = ruleDef.getRule();
			for (Rule rule : srcRuleList) {
				Rule newRule = rule.copy();
				if (null == newRule.getChecker()) {
					newRule.parseValue(fieldType, RuleEnum.getEnum(newRule.getName()), ruleDef.getId());
				}
				ruleList.add(newRule);
			}
		}
	}

	public void clear() {
		this.localDefMap = null;
		this.globleDefMap = null;
		this.ruleGroupMap = null;
		this.customCheckerMap = null;
	}
}
