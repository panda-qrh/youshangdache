package com.qrh.youshangdache.rules.utils;

import jakarta.annotation.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

@Component
public class DroolsUtils {

    @Resource
    private KieContainer kieContainer;

    public <T> T execute(Object fact, String globalName, Class<T> responseType) {
        KieSession kieSession = kieContainer.newKieSession();
        try {
            T response = responseType.getDeclaredConstructor().newInstance();
            kieSession.setGlobal(globalName, response);
            kieSession.insert(fact);
            kieSession.fireAllRules();
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Drools rule 执行失败", e);
        } finally {
            kieSession.dispose();
        }
    }
}
