package com.runtime.deadcode;


import java.lang.management.*;
import java.util.*;
import com.sun.management.OperatingSystemMXBean; // Needs JVM with Sun extensions

public class JVMStatsCollector {

    public static String buildJvmOsStatsJson() {

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"statsType\":\"jvmOsStats\",");
        sb.append("\"timestamp\":\"").append(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date())).append("\",");

        // --- OS Info
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        sb.append("\"os\":{");
        sb.append("\"arch\":\"").append(osBean.getArch()).append("\",");
        sb.append("\"name\":\"").append(osBean.getName()).append("\",");
        sb.append("\"version\":\"").append(osBean.getVersion()).append("\",");
        sb.append("\"systemLoadAverage\":").append(osBean.getSystemLoadAverage()).append(",");
        sb.append("\"availableProcessors\":").append(osBean.getAvailableProcessors()).append(",");
        sb.append("\"totalPhysicalMemorySize\":").append(osBean.getTotalPhysicalMemorySize()).append(",");
        sb.append("\"freePhysicalMemorySize\":").append(osBean.getFreePhysicalMemorySize()).append(",");
        sb.append("\"totalSwapSpaceSize\":").append(osBean.getTotalSwapSpaceSize()).append(",");
        sb.append("\"freeSwapSpaceSize\":").append(osBean.getFreeSwapSpaceSize()).append(",");
        sb.append("\"processCpuLoad\":").append(osBean.getProcessCpuLoad()).append(",");
        sb.append("\"systemCpuLoad\":").append(osBean.getSystemCpuLoad());
        sb.append("},");

        // --- Memory
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        sb.append("\"memory\":{");
        sb.append("\"heap\":{")
                .append("\"used\":").append(heapUsage.getUsed()).append(",")
                .append("\"committed\":").append(heapUsage.getCommitted()).append(",")
                .append("\"max\":").append(heapUsage.getMax())
                .append("},");
        sb.append("\"nonHeap\":{")
                .append("\"used\":").append(nonHeapUsage.getUsed()).append(",")
                .append("\"committed\":").append(nonHeapUsage.getCommitted()).append(",")
                .append("\"max\":").append(nonHeapUsage.getMax())
                .append("}");
        sb.append("},");

        // --- Threads
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        sb.append("\"threads\":{");
        sb.append("\"threadCount\":").append(threadBean.getThreadCount()).append(",");
        sb.append("\"peakThreadCount\":").append(threadBean.getPeakThreadCount()).append(",");
        sb.append("\"daemonThreadCount\":").append(threadBean.getDaemonThreadCount()).append(",");
        sb.append("\"totalStartedThreadCount\":").append(threadBean.getTotalStartedThreadCount()).append(",");
        // Deadlocked threads (as array)
        long[] deadlocked = threadBean.findDeadlockedThreads();
        sb.append("\"deadlockedThreads\":[");
        if (deadlocked != null && deadlocked.length > 0) {
            for (int i = 0; i < deadlocked.length; i++) {
                sb.append(deadlocked[i]);
                if (i < deadlocked.length - 1) sb.append(",");
            }
        }
        sb.append("]");
        sb.append("},");

        // --- Class Loading
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        sb.append("\"classLoading\":{");
        sb.append("\"loadedClassCount\":").append(classBean.getLoadedClassCount()).append(",");
        sb.append("\"totalLoadedClassCount\":").append(classBean.getTotalLoadedClassCount()).append(",");
        sb.append("\"unloadedClassCount\":").append(classBean.getUnloadedClassCount());
        sb.append("},");

        // --- Garbage Collectors
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        sb.append("\"garbageCollectors\":[");
        for (int i = 0; i < gcBeans.size(); i++) {
            GarbageCollectorMXBean gc = gcBeans.get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(gc.getName()).append("\",");
            sb.append("\"collectionCount\":").append(gc.getCollectionCount()).append(",");
            sb.append("\"collectionTime\":").append(gc.getCollectionTime());
            sb.append("}");
            if (i < gcBeans.size() - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}"); // end outer JSON
        return sb.toString();
    }


}
