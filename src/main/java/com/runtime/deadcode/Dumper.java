package com.runtime.deadcode;

import com.google.gson.Gson;
import com.runtime.deadcode.config.DeadCodeAgentProperties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dumper {
    private static final Gson gson = new Gson();

    public static void start(DeadCodeAgentProperties props) {
        int interval = props.getDumpIntervalSeconds();
        String analyzerEndpoint = props.getAnalyzerBaseUrl() + "/api/deadcode/runtime";

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                String tmpDir = System.getProperty("java.io.tmpdir");
                File file = new File(tmpDir, "deadcode_snapshot.json");
                System.out.println("[Dumper] Writing to: " + file.getAbsolutePath());

                Map<String, ?> snapshot;
                if (!TrackerRegistry.getMethodCounts().isEmpty()) {
                    snapshot = TrackerRegistry.getMethodCounts().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().longValue()
                            ));
                } else {
                    snapshot = TrackerRegistry.getSnapshot();
                }

                String json = gson.toJson(snapshot);

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(json);
                }

                // Post to remote endpoint
                postToAnalyzer(analyzerEndpoint, json);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, interval, TimeUnit.SECONDS);
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