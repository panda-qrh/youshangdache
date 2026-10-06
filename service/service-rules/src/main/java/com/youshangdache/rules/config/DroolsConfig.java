package com.youshangdache.rules.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);
    private static final String RULES_PATH = "classpath*:rules/*.drl";

    /**
     * 为每个 .drl 文件创建独立的 KieContainer。
     * 返回的 Map 以文件名（不含路径）为 key，调用方通过 RuleType.drlFile() 取值。
     */
    @Bean
    public Map<String, KieContainer> kieContainerMap() {
        KieServices kieServices = KieServices.Factory.get();

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(RULES_PATH);
        } catch (Exception e) {
            throw new RuntimeException("无法扫描 Drools 规则文件: " + RULES_PATH, e);
        }

        Map<String, KieContainer> map = Stream.of(resources)
                .collect(Collectors.toMap(
                        Resource::getFilename,
                        r -> {
                            KieFileSystem fs = kieServices.newKieFileSystem();
                            fs.write(ResourceFactory.newClassPathResource("rules/" + r.getFilename()));
                            KieBuilder builder = kieServices.newKieBuilder(fs).buildAll();
                            if (builder.getResults().hasMessages(Message.Level.ERROR)) {
                                throw new RuntimeException("Drools 编译错误 [" + r.getFilename() + "]: " + builder.getResults());
                            }
                            KieModule module = builder.getKieModule();
                            KieContainer container = kieServices.newKieContainer(module.getReleaseId());
                            log.info("Drools KieContainer 加载完成: {} (releaseId={})", r.getFilename(), module.getReleaseId());
                            return container;
                        }
                ));

        if (map.isEmpty()) {
            throw new RuntimeException("未找到任何 Drools 规则文件，路径: " + RULES_PATH);
        }
        return map;
    }
}
