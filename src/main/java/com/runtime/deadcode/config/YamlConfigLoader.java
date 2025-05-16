package com.runtime.deadcode.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;

public class YamlConfigLoader {
    public static DeadCodeAgentProperties loadFromYaml(String path) {
        DeadCodeAgentProperties config = new DeadCodeAgentProperties();
        try (InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream("application.yaml");) {
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(in);
            Map<String, Object> deadcode = (Map<String, Object>) obj.get("deadcode");

            config.setStaticScan(Boolean.parseBoolean(String.valueOf(deadcode.get("static-scan"))));
            config.setRepo((String) deadcode.get("repo"));
            config.setBranch((String) deadcode.getOrDefault("branch", "master"));
            config.setBasePackages((List<String>) deadcode.get("base-packages"));
            config.setTrackExecutionTime(Boolean.parseBoolean(String.valueOf(deadcode.get("track-execution-time"))));
            config.setTrackInvocationCount(Boolean.parseBoolean(String.valueOf(deadcode.get("track-invocation-count"))));
            config.setAnalyzerBaseUrl((String)
                    deadcode.getOrDefault("analyzer-base-url", "http://localhost:9090"));
            config.setDumpIntervalSeconds(Integer.parseInt(String.valueOf(deadcode.get("dump-interval-time"))));

        } catch (Exception e) {
            System.err.println("[Agent] Failed to read or parse application.yaml, falling back to deadcode-agent.properties...");
            loadFallbackProperties(config);
        }
        return config;
    }

    private static void loadFallbackProperties(DeadCodeAgentProperties config) {
        try (FileInputStream fis = new FileInputStream("deadcode-agent.properties")) {
            Properties fileProps = new Properties();
            fileProps.load(fis);
            config.setStaticScan(false); // explicitly disable static scan in fallback
            config.setRepo(fileProps.getProperty("deadcode.repo"));
            config.setBranch(fileProps.getProperty("deadcode.branch", "master"));
            String packages = fileProps.getProperty("deadcode.base-packages", "");
            config.setBasePackages(List.of(packages.split(",")));
            config.setTrackExecutionTime(Boolean.parseBoolean(fileProps.getProperty("deadcode.track-execution-time", "false")));
            config.setTrackInvocationCount(Boolean.parseBoolean(fileProps.getProperty("deadcode.track-invocation-count", "false")));
            config.setAnalyzerBaseUrl(fileProps.getProperty("deadcode.analyzer-base-url", "http://localhost:9090"));
            config.setDumpIntervalSeconds(10);
        } catch (Exception ex) {
            System.err.println("[Agent] Failed to read fallback deadcode-agent.properties: " + ex.getMessage());
        }
    }
}