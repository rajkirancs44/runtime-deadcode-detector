package com.runtime.deadcode;

import com.runtime.deadcode.config.DeadCodeAgentProperties;
import com.runtime.deadcode.config.DeadCodeConfigRestLoader;
import com.runtime.deadcode.rest.DeadCodeRestClient;

public class AgentBootstrap {

    public static DeadCodeAgentProperties init() {
        // Get API base URL and unique onboarding ID from system properties or environment
        String apiBaseUrl = System.getProperty("deadcode.config.api", "http://localhost:8081/api/onboarding");
        String configId = System.getProperty("deadcode.config.id", ""); // must be provided!

        if (configId.isEmpty()) {
            throw new IllegalArgumentException("Missing required config id (set system property deadcode.config.id=...)");
        }

        DeadCodeAgentProperties props = DeadCodeConfigRestLoader.loadFromApi(apiBaseUrl, configId);

        if (props.isStaticScan()) {
            DeadCodeRestClient.triggerStaticScan(props);
        } else {
            System.out.println("[Agent] Static scan skipped — proceeding with runtime tracking only.");
        }

        return props;
    }
}
