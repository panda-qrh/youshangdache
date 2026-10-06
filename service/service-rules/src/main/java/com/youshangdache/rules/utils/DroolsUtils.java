package com.youshangdache.rules.utils;

import com.youshangdache.rules.enums.RuleType;
import jakarta.annotation.Resource;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DroolsUtils {

    private static final Logger log = LoggerFactory.getLogger(DroolsUtils.class);

    /** key = 文件名（如 "FeeRule.drl"），value = 该文件专属的 KieContainer */
    @Resource
    private Map<String, KieContainer> kieContainerMap;

    /** Spring 注入用的无参构造 */
    public DroolsUtils() {
    }

    /** 测试用：直接传入 KieContainer Map */
    public DroolsUtils(Map<String, KieContainer> kieContainerMap) {
        this.kieContainerMap = kieContainerMap;
    }

    public <T> T execute(Object fact, RuleType ruleType, Class<T> responseType) {
        KieContainer container = kieContainerMap.get(ruleType.drlFile());
        if (container == null) {
            throw new IllegalArgumentException("未找到规则文件对应的 KieContainer: " + ruleType.drlFile());
        }

        KieSession kieSession = container.newKieSession();
        try {
            T response = responseType.getDeclaredConstructor().newInstance();
            kieSession.setGlobal(ruleType.globalName(), response);
            kieSession.insert(fact);
            kieSession.fireAllRules();
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Drools 规则执行失败 [" + ruleType.drlFile() + "]", e);
        } finally {
            kieSession.dispose();
        }
    }
}
