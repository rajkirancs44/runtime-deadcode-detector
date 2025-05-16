package com.runtime.deadcode.rest;

import com.runtime.deadcode.config.DeadCodeAgentProperties;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DeadCodeRestClient {

    public static void triggerStaticScan(DeadCodeAgentProperties config) {
        try {
            String baseUrl = config.getAnalyzerBaseUrl() + "/start-scan";
            String params = String.format("?repo=%s&branch=%s&packages=%s",
                    URLEncoder.encode(config.getRepo(), StandardCharsets.UTF_8),
                    URLEncoder.encode(config.getBranch(), StandardCharsets.UTF_8),
                    URLEncoder.encode(String.join(",", config.getBasePackages()), StandardCharsets.UTF_8));

            URL url = new URL(baseUrl + params);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            System.out.println("[Agent] Static scan trigger response: " + responseCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}