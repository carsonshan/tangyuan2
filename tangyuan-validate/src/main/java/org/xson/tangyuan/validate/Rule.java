package org.xson.tangyuan.validate;

import org.xson.tangyuan.validate.rule.ArrayLengthIntervalChecker;
import org.xson.tangyuan.validate.rule.ArrayLengthMaxChecker;
import org.xson.tangyuan.validate.rule.ArrayLengthMinChecker;
import org.xson.tangyuan.validate.rule.BigDecimalIntervalChecker;
import org.xson.tangyuan.validate.rule.BigDecimalMaxChecker;
import org.xson.tangyuan.validate.rule.BigDecimalMinChecker;
import org.xson.tangyuan.validate.rule.BigIntegerIntervalChecker;
import org.xson.tangyuan.validate.rule.BigIntegerMaxChecker;
import org.xson.tangyuan.validate.rule.BigIntegerMinChecker;
import org.xson.tangyuan.validate.rule.CollectionLengthIntervalChecker;
import org.xson.tangyuan.validate.rule.CollectionLengthMaxChecker;
import org.xson.tangyuan.validate.rule.CollectionLengthMinChecker;
import org.xson.tangyuan.validate.rule.DoubleEnumChecker;
import org.xson.tangyuan.validate.rule.DoubleIntervalChecker;
import org.xson.tangyuan.validate.rule.DoubleMaxChecker;
import org.xson.tangyuan.validate.rule.DoubleMinChecker;
import org.xson.tangyuan.validate.rule.FloatEnumChecker;
import org.xson.tangyuan.validate.rule.FloatIntervalChecker;
import org.xson.tangyuan.validate.rule.FloatMaxChecker;
import org.xson.tangyuan.validate.rule.FloatMinChecker;
import org.xson.tangyuan.validate.rule.IntegerEnumChecker;
import org.xson.tangyuan.validate.rule.IntegerIntervalChecker;
import org.xson.tangyuan.validate.rule.IntegerMaxChecker;
import org.xson.tangyuan.validate.rule.IntegerMinChecker;
import org.xson.tangyuan.validate.rule.LongEnumChecker;
import org.xson.tangyuan.validate.rule.LongIntervalChecker;
import org.xson.tangyuan.validate.rule.LongMaxChecker;
import org.xson.tangyuan.validate.rule.LongMinChecker;
import org.xson.tangyuan.validate.rule.StringEnumChecker;
import org.xson.tangyuan.validate.rule.StringFilterChecker;
import org.xson.tangyuan.validate.rule.StringLengthIntervalChecker;
import org.xson.tangyuan.validate.rule.StringLengthMaxChecker;
import org.xson.tangyuan.validate.rule.StringLengthMinChecker;
import org.xson.tangyuan.validate.rule.StringMatchChecker;
import org.xson.tangyuan.validate.rule.StringNoMatchChecker;

public class Rule {

	private String	name;
	private Object	value;
	// 自定义的
	private Checker	checker;

	public Rule(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public Rule(Checker checker) {
		this.checker = checker;
	}

	public Checker getChecker() {
		return checker;
	}

	public Checker findChecker(TypeEnum type) {
		if (null == checker) {
			// return ValidateComponent.getChecker((type.getValue() + "_" + name).toUpperCase());
			return ValidateComponent.getInstance().getChecker((type.getValue() + "_" + name).toUpperCase());
		}
		return checker;
	}

	public Rule copy() {
		if (null == this.checker) {
			return new Rule(this.name, (String) this.value);
		} else {
			return new Rule(checker);
		}
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * 解析规则值
	 */
	public void parseValue(TypeEnum fieldType, RuleEnum ruleType, String relatedId) {

		if (null == this.value) {
			return;
		}

		String val = (String) this.value;
		if (fieldType == TypeEnum.INTEGER) {
			if (ruleType == RuleEnum.ENUM) {
				this.value = IntegerEnumChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL) {
				this.value = IntegerIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = IntegerMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = IntegerMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [INTEGER AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.LONG) {
			if (ruleType == RuleEnum.ENUM) {
				this.value = LongEnumChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL) {
				this.value = LongIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = LongMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = LongMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [LONG AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.FLOAT) {
			if (ruleType == RuleEnum.ENUM) {
				this.value = FloatEnumChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL) {
				this.value = FloatIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = FloatMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = FloatMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [FLOAT AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.DOUBLE) {
			if (ruleType == RuleEnum.ENUM) {
				this.value = DoubleEnumChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL) {
				this.value = DoubleIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = DoubleMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = DoubleMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [DOUBLE AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.STRING) {
			if (ruleType == RuleEnum.ENUM) {
				this.value = StringEnumChecker.parseValue(val);
			} else if (ruleType == RuleEnum.FILTER) {
				this.value = StringFilterChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL_LENGTH) {
				this.value = StringLengthIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX_LENGTH) {
				this.value = StringLengthMaxChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN_LENGTH) {
				this.value = StringLengthMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MATCH) {
				this.value = StringMatchChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MISMATCH) {
				this.value = StringNoMatchChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [STRING AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.ARRAY) {
			if (ruleType == RuleEnum.MAX_LENGTH) {
				this.value = ArrayLengthMaxChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN_LENGTH) {
				this.value = ArrayLengthMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL_LENGTH) {
				this.value = ArrayLengthIntervalChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [ARRAY AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.COLLECTION) {
			if (ruleType == RuleEnum.MAX_LENGTH) {
				this.value = CollectionLengthMaxChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN_LENGTH) {
				this.value = CollectionLengthMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.INTERVAL_LENGTH) {
				this.value = CollectionLengthIntervalChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [COLLECTION AND " + ruleType + "]" + relatedId);
			}
		}

		else if (fieldType == TypeEnum.BIGINTEGER) {
			if (ruleType == RuleEnum.INTERVAL) {
				this.value = BigIntegerIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = BigIntegerMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = BigIntegerMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [BIGINTEGER AND " + ruleType + "]" + relatedId);
			}
		} else if (fieldType == TypeEnum.BIGDECIMAL) {
			if (ruleType == RuleEnum.INTERVAL) {
				this.value = BigDecimalIntervalChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MIN) {
				this.value = BigDecimalMinChecker.parseValue(val);
			} else if (ruleType == RuleEnum.MAX) {
				this.value = BigDecimalMaxChecker.parseValue(val);
			} else {
				throw new RuntimeException("Unsupported validate mode: [BIGDECIMAL AND " + ruleType + "]" + relatedId);
			}
		}
	}
}
