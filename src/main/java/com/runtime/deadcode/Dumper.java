package com.runtime.deadcode;


import com.google.gson.Gson;
import com.runtime.deadcode.config.DeadCodeAgentProperties;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class Dumper {
    private static final Gson gson = new Gson();

    public static void start(DeadCodeAgentProperties props) {
        int interval = props.getDumpIntervalSeconds();
        String analyzerEndpoint = props.getAnalyzerBaseUrl()
                + "/api/deadcode/runtime"
                + "?appId=" + URLEncoder.encode(props.getAppId(), StandardCharsets.UTF_8)
                + "&serviceId=" + URLEncoder.encode(props.getServiceName(), StandardCharsets.UTF_8);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                Map<String, LongAdder> counts = TrackerRegistry.getMethodCounts();
                Map<String, Long> times = TrackerRegistry.getMaxExecutionTimes();
                Map<String, Map<String, Object>> snapshot = new HashMap<>();

                counts.forEach((method, adder) -> {
                    snapshot.computeIfAbsent(method, k -> new HashMap<>())
                            .put("count", adder.longValue());
                });
                times.forEach((method, durationNs) -> {
                    snapshot.computeIfAbsent(method, k -> new HashMap<>())
                            .put("maxTimeMs", durationNs / 1_000_000.0); // ms
                });
                if (snapshot.isEmpty()) {
                    TrackerRegistry.getSnapshot().forEach((className, methods) -> {
                        methods.forEach(method -> {
                            snapshot.computeIfAbsent(className + "#" + method, k -> new HashMap<>())
                                    .put("observed", true);
                        });
                    });
                }

                String json = gson.toJson(snapshot);
                postToAnalyzer(analyzerEndpoint, json);
            } catch (Exception e) {
                System.err.println("[Dumper] Error while writing snapshot: " + e.getMessage());
            }
        }, 0, interval, TimeUnit.SECONDS);
    }

    public static void startScheduledStatsPosting(DeadCodeAgentProperties props) {
        if (props.isStatsRequired()) {
            int intervalSeconds = props.getStatsDumpInterval();
            String analyzerEndpoint = props.getAnalyzerBaseUrl()
                    + "/api/apps/"
                    + URLEncoder.encode(props.getAppId(), StandardCharsets.UTF_8)
                    + "/services/" + URLEncoder.encode(props.getServiceName(), StandardCharsets.UTF_8)
                    + "/stats-jvm";

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                String json = JVMStatsCollector.buildJvmOsStatsJson();
                postToAnalyzer(analyzerEndpoint, json);
            }, 0, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    private static void postToAnalyzer(String endpoint, String json) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] postData = json.getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(postData);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("[Dumper] Pushed runtime snapshot to analyzer.");
            } else {
                System.err.println("[Dumper] Failed to push snapshot: HTTP " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("[Dumper] Error posting to analyzer: " + e.getMessage());
        }
    }
}
