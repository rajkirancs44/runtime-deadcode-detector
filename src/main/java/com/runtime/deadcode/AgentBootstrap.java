package com.runtime.deadcode;

import com.runtime.deadcode.config.DeadCodeAgentProperties;
import com.runtime.deadcode.config.YamlConfigLoader;
import com.runtime.deadcode.rest.DeadCodeRestClient;

public class AgentBootstrap {

    public static DeadCodeAgentProperties init() {
        String yamlPath = System.getProperty("app.yml.path", "application.yaml");
        DeadCodeAgentProperties props = YamlConfigLoader.loadFromYaml(yamlPath);

        if (props.isStaticScan()) {
            DeadCodeRestClient.triggerStaticScan(props);
        } else {
            System.out.println("[Agent] Static scan skipped — proceeding with runtime tracking only.");
        }

        return props;
    }
}