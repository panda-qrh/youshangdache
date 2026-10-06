package com.youshangdache.rules.enums;

/**
 * 规则集标识：每个枚举值对应一个 .drl 文件及其 global 变量名。
 */
public enum RuleType {

    FEE("FeeRule.drl", "feeRuleResponse"),
    REWARD("RewardRule.drl", "rewardRuleResponse"),
    PROFITSHARING("ProfitsharingRule.drl", "profitsharingRuleResponse");

    private final String drlFile;
    private final String globalName;

    RuleType(String drlFile, String globalName) {
        this.drlFile = drlFile;
        this.globalName = globalName;
    }

    public String drlFile() {
        return drlFile;
    }

    public String globalName() {
        return globalName;
    }
}
