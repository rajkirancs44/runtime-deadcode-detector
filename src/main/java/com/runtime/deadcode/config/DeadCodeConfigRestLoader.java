package com.runtime.deadcode.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DeadCodeConfigRestLoader {

    public static DeadCodeAgentProperties loadFromApi(String apiBaseUrl, String uniqueId) {
        DeadCodeAgentProperties config = new DeadCodeAgentProperties();
        try {
            String endpoint = apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/";
            URL url = new URL(endpoint + uniqueId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStream in = conn.getInputStream();
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject deadcode = root.getAsJsonObject("deadcode");

                    config.setStaticScan(deadcode.has("staticScan") && deadcode.get("staticScan").getAsBoolean());
                    config.setRepo(deadcode.has("repo") ? deadcode.get("repo").getAsString() : null);
                    config.setBranch(deadcode.has("branch") ? deadcode.get("branch").getAsString() : "master");

                    // basePackages: JsonArray -> List<String>
                    List<String> basePackages = new ArrayList<>();
                    if (deadcode.has("basePackages") && deadcode.get("basePackages").isJsonArray()) {
                        JsonArray pkgs = deadcode.getAsJsonArray("basePackages");
                        for (int i = 0; i < pkgs.size(); ++i)
                            basePackages.add(pkgs.get(i).getAsString());
                    }
                    config.setBasePackages(basePackages);

                    config.setTrackExecutionTime(deadcode.has("trackExecutionTime") && deadcode.get("trackExecutionTime").getAsBoolean());
                    config.setTrackInvocationCount(deadcode.has("trackInvocationCount") && deadcode.get("trackInvocationCount").getAsBoolean());
                    config.setAnalyzerBaseUrl(deadcode.has("analyzerBaseUrl") ? deadcode.get("analyzerBaseUrl").getAsString() : "http://localhost:8081");
                    config.setDumpIntervalSeconds(deadcode.has("dumpIntervalTime") ? deadcode.get("dumpIntervalTime").getAsInt() : 10);
                    config.setStatsRequired(deadcode.has("statsScan") && deadcode.get("statsScan").getAsBoolean());
                    config.setStatsDumpInterval(deadcode.has("statsDumpIntervalTime") ? deadcode.get("statsDumpIntervalTime").getAsInt() : 120);
                    config.setAppId(deadcode.has("appId") ? deadcode.get("appId").getAsString() : "1");
                    config.setServiceName(deadcode.has("serviceName") ? deadcode.get("serviceName").getAsString() : "Test-App");
                }
            } else {
                System.err.println("[Agent] Failed to load config from API. HTTP " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("[Agent] Exception loading config from API: " + e.getMessage());
            e.printStackTrace();
        }
        return config;
    }
}
