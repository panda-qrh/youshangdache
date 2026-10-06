package com.youshangdache.rules;

import com.youshangdache.model.form.rules.FeeRuleRequest;
import com.youshangdache.model.form.rules.ProfitsharingRuleRequest;
import com.youshangdache.model.form.rules.RewardRuleRequest;
import com.youshangdache.model.vo.rules.FeeRuleResponse;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponse;
import com.youshangdache.model.vo.rules.RewardRuleResponse;
import com.youshangdache.rules.enums.RuleType;
import com.youshangdache.rules.utils.DroolsUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static DroolsUtils droolsUtils;

    @BeforeAll
    static void initDroolsUtils() {
        KieServices kieServices = KieServices.Factory.get();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath*:rules/*.drl");
            Map<String, KieContainer> containerMap = Stream.of(resources)
                    .collect(Collectors.toMap(
                            Resource::getFilename,
                            r -> {
                                KieFileSystem fs = kieServices.newKieFileSystem();
                                fs.write(ResourceFactory.newClassPathResource("rules/" + r.getFilename()));
                                KieBuilder builder = kieServices.newKieBuilder(fs).buildAll();
                                if (builder.getResults().hasMessages(Message.Level.ERROR)) {
                                    throw new RuntimeException(
                                            "Drools 编译错误 [" + r.getFilename() + "]: " + builder.getResults());
                                }
                                KieModule module = builder.getKieModule();
                                return kieServices.newKieContainer(module.getReleaseId());
                            }
                    ));

            droolsUtils = new DroolsUtils(containerMap);
            assertNotNull(droolsUtils, "DroolsUtils 初始化失败");
        } catch (Exception e) {
            throw new RuntimeException("无法加载 Drools 规则文件: classpath*:rules/*.drl", e);
        }
    }

    @Test
    @DisplayName("计价规则：07:00-23:59 起步价19元含5公里，超出部分3元/公里")
    void shouldCalculateFeeRule() {
        //距离 10 公里、白天时段、无等候 → 19 + (10-5)*3 = 34 元
        FeeRuleResponse response = droolsUtils.execute(
                FeeRuleRequest.builder()
                        .distance(new BigDecimal("10"))
                        .startTime("08:00:00")
                        .waitMinute(0)
                        .build(),
                RuleType.FEE,
                FeeRuleResponse.class);

        assertEquals(0, new BigDecimal("34").compareTo(response.getTotalAmount()),
                "白天时段 10 公里应为 34 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("计价规则：等候超过10分钟后按1元/分钟计费")
    void shouldCalculateWaitFee() {
        //距离 5 公里（未超出起步里程）、等候 15 分钟 → 19 + (15-10)*1 = 24 元
        FeeRuleResponse response = droolsUtils.execute(
                FeeRuleRequest.builder()
                        .distance(new BigDecimal("5"))
                        .startTime("08:00:00")
                        .waitMinute(15)
                        .build(),
                RuleType.FEE,
                FeeRuleResponse.class);

        assertEquals(0, new BigDecimal("24").compareTo(response.getTotalAmount()),
                "等候 15 分钟应为 24 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("计价规则：行程超过12公里后每公里加收1元远途费")
    void shouldCalculateLongDistanceFee() {
        //距离 15 公里、白天时段 → 19 + (15-5)*3 + (15-12)*1 = 52 元
        FeeRuleResponse response = droolsUtils.execute(
                FeeRuleRequest.builder()
                        .distance(new BigDecimal("15"))
                        .startTime("08:00:00")
                        .waitMinute(0)
                        .build(),
                RuleType.FEE,
                FeeRuleResponse.class);

        assertEquals(0, new BigDecimal("52").compareTo(response.getTotalAmount()),
                "15 公里应为 52 元，实际：" + response.getTotalAmount());
    }

    @Test
    @DisplayName("奖励规则：白天时段完成10单以上每单奖励2元（依赖 startTime 正确传入）")
    void shouldCalculateRewardRuleWithStartTime() {
        RewardRuleResponse response = droolsUtils.execute(
                RewardRuleRequest.builder()
                        .startTime("08:00:00")
                        .orderNum(11L)
                        .build(),
                RuleType.REWARD,
                RewardRuleResponse.class);

        assertEquals(0, new BigDecimal("2.0").compareTo(response.getRewardAmount()),
                "白天时段完成11单应奖励2元，实际：" + response.getRewardAmount());
    }

    @Test
    @DisplayName("分账规则：订单金额100元、当日5单，平台抽成20%、司机个税10%")
    void shouldCalculateProfitsharingRule() {
        ProfitsharingRuleResponse response = droolsUtils.execute(
                ProfitsharingRuleRequest.builder()
                        .orderAmount(new BigDecimal("100"))
                        .orderNum(5L)
                        .build(),
                RuleType.PROFITSHARING,
                ProfitsharingRuleResponse.class);

        //通道费 100*0.006 = 0.60；抽成 (100-0.60)*0.2 = 19.88；司机税前 79.52；个税 7.95；司机到手 71.57
        assertEquals(0, new BigDecimal("0.60").compareTo(response.getPaymentFee()),
                "通道费应为 0.60，实际：" + response.getPaymentFee());
        assertEquals(0, new BigDecimal("19.88").compareTo(response.getPlatformIncome()),
                "平台抽成应为 19.88，实际：" + response.getPlatformIncome());
        assertEquals(0, new BigDecimal("7.95").compareTo(response.getDriverTaxFee()),
                "司机个税应为 7.95，实际：" + response.getDriverTaxFee());
        assertEquals(0, new BigDecimal("71.57").compareTo(response.getDriverIncome()),
                "司机到手应为 71.57，实际：" + response.getDriverIncome());
    }
}
