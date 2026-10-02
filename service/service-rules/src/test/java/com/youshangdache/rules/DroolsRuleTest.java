package com.youshangdache.rules;

import com.youshangdache.model.form.rules.FeeRuleRequest;
import com.youshangdache.model.form.rules.ProfitsharingRuleRequest;
import com.youshangdache.model.form.rules.RewardRuleRequest;
import com.youshangdache.model.vo.rules.FeeRuleResponse;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponse;
import com.youshangdache.model.vo.rules.RewardRuleResponse;
import com.youshangdache.rules.config.DroolsConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 规则引擎测试
 *
 * <p>这些用例同时起到两个作用：</p>
 * <ol>
 *     <li>保证三个 .drl 文件可以被 Drools 正常编译加载（包名/import/global 写错时会在 @BeforeAll 直接失败）；</li>
 *     <li>锁定计价、奖励、分账三组规则的核心算法，后续调整规则时能被及时发现。</li>
 * </ol>
 */
class DroolsRuleTest {

    private static KieContainer kieContainer;

    @BeforeAll
    static void initKieContainer() {
        //规则文件加载失败或规则本身编译不过时，这里会直接抛异常
        kieContainer = new DroolsConfig().kieContainer();
        assertNotNull(kieContainer, "KieContainer 初始化失败");
    }

    @Test
    @DisplayName("计价规则：07:00-23:59 起步价19元含5公里，超出部分3元/公里")
    void shouldCalculateFeeRule() {
        //距离 10 公里、白天时段、无等候 → 19 + (10-5)*3 = 34 元
        FeeRuleResponse response = fireFeeRule(new BigDecimal("10"), "08:00:00", 0);

        assertEquals(0, new BigDecimal("34").compareTo(response.getTotalAmount()),
                "白天时段 10 公里应为 34 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("计价规则：等候超过10分钟后按1元/分钟计费")
    void shouldCalculateWaitFee() {
        //距离 5 公里（未超出起步里程）、等候 15 分钟 → 19 + (15-10)*1 = 24 元
        FeeRuleResponse response = fireFeeRule(new BigDecimal("5"), "08:00:00", 15);

        assertEquals(0, new BigDecimal("24").compareTo(response.getTotalAmount()),
                "等候 15 分钟应为 24 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("计价规则：行程超过12公里后每公里加收1元远途费")
    void shouldCalculateLongDistanceFee() {
        //距离 15 公里、白天时段 → 19 + (15-5)*3 + (15-12)*1 = 52 元
        FeeRuleResponse response = fireFeeRule(new BigDecimal("15"), "08:00:00", 0);

        assertEquals(0, new BigDecimal("52").compareTo(response.getTotalAmount()),
                "15 公里应为 52 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("奖励规则：白天时段完成10单以上每单奖励2元（依赖 startTime 正确传入）")
    void shouldCalculateRewardRuleWithStartTime() {
        KieSession session = kieContainer.newKieSession();
        try {
            RewardRuleResponse response = new RewardRuleResponse();
            session.setGlobal("rewardRuleResponse", response);
            session.insert(RewardRuleRequest.builder()
                    .startTime("08:00:00")
                    .orderNum(11L)
                    .build());
            session.fireAllRules();

            assertEquals(0, new BigDecimal("2.0").compareTo(response.getRewardAmount()),
                    "白天时段完成11单应奖励2元，实际：" + response.getRewardAmount());
        } finally {
            session.dispose();
        }
    }

    @Test
    @DisplayName("分账规则：订单金额100元、当日5单，平台抽成20%、司机个税10%")
    void shouldCalculateProfitsharingRule() {
        KieSession session = kieContainer.newKieSession();
        try {
            ProfitsharingRuleResponse response = new ProfitsharingRuleResponse();
            session.setGlobal("profitsharingRuleResponse", response);
            session.insert(ProfitsharingRuleRequest.builder()
                    .orderAmount(new BigDecimal("100"))
                    .orderNum(5L)
                    .build());
            session.fireAllRules();

            //通道费 100*0.006 = 0.60；抽成 (100-0.60)*0.2 = 19.88；司机税前 79.52；个税 7.95；司机到手 71.57
            assertEquals(0, new BigDecimal("0.60").compareTo(response.getPaymentFee()),
                    "通道费应为 0.60，实际：" + response.getPaymentFee());
            assertEquals(0, new BigDecimal("19.88").compareTo(response.getPlatformIncome()),
                    "平台抽成应为 19.88，实际：" + response.getPlatformIncome());
            assertEquals(0, new BigDecimal("7.95").compareTo(response.getDriverTaxFee()),
                    "司机个税应为 7.95，实际：" + response.getDriverTaxFee());
            assertEquals(0, new BigDecimal("71.57").compareTo(response.getDriverIncome()),
                    "司机到手应为 71.57，实际：" + response.getDriverIncome());
        } finally {
            session.dispose();
        }
    }

    /**
     * 执行计价规则
     *
     * @param distance   里程（公里）
     * @param startTime  开始时间 HH:mm:ss
     * @param waitMinute 等候分钟数
     * @return 计价结果
     */
    private FeeRuleResponse fireFeeRule(BigDecimal distance, String startTime, Integer waitMinute) {
        KieSession session = kieContainer.newKieSession();
        try {
            FeeRuleResponse response = new FeeRuleResponse();
            session.setGlobal("feeRuleResponse", response);
            session.insert(FeeRuleRequest.builder()
                    .distance(distance)
                    .startTime(startTime)
                    .waitMinute(waitMinute)
                    .build());
            session.fireAllRules();
            return response;
        } finally {
            session.dispose();
        }
    }
}
