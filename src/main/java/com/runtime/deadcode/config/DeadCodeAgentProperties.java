package com.runtime.deadcode.config;

import java.util.List;

public class DeadCodeAgentProperties {
    private boolean staticScan;
    private String repo;
    private String branch = "master";
    private List<String> basePackages;
    private boolean trackExecutionTime;
    private boolean trackInvocationCount;
    private String analyzerBaseUrl = "http://localhost:8081";
    private int dumpIntervalSeconds = 10; // default
    private int statsDumpInterval=120;
    private boolean statsRequired;
    private String appId;
    private String serviceName;

    public boolean isStaticScan() {
        return staticScan;
    }

    public void setStaticScan(boolean staticScan) {
        this.staticScan = staticScan;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public boolean isTrackExecutionTime() {
        return trackExecutionTime;
    }

    public void setTrackExecutionTime(boolean trackExecutionTime) {
        this.trackExecutionTime = trackExecutionTime;
    }

    public boolean isTrackInvocationCount() {
        return trackInvocationCount;
    }

    public void setTrackInvocationCount(boolean trackInvocationCount) {
        this.trackInvocationCount = trackInvocationCount;
    }

    public String getAnalyzerBaseUrl() {
        return analyzerBaseUrl;
    }

    public void setAnalyzerBaseUrl(String analyzerBaseUrl) {
        this.analyzerBaseUrl = analyzerBaseUrl;
    }

    public int getDumpIntervalSeconds() {
        return dumpIntervalSeconds;
    }

    public void setDumpIntervalSeconds(int dumpIntervalSeconds) {
        this.dumpIntervalSeconds = dumpIntervalSeconds;
    }

    public int getStatsDumpInterval() {
        return statsDumpInterval;
    }

    public void setStatsDumpInterval(int statsDumpInterval) {
        this.statsDumpInterval = statsDumpInterval;
    }

    public boolean isStatsRequired() {
        return statsRequired;
    }

    public void setStatsRequired(boolean statsRequired) {
        this.statsRequired = statsRequired;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
