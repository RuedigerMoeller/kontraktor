package org.nustaq.kontraktor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PerformanceTracker {

    static int siz = (int) (TimeUnit.DAYS.toMillis(1) / TimeUnit.MINUTES.toMillis(1));
    static long dayLenMillis = siz * TimeUnit.DAYS.toMillis(1);

    static class MethodStats {
        String method;
        String clazz;
        int[] callCount;
        int[] duration;
        int[] exception;
        int[] resolves;

        public MethodStats(String method, String clazz) {
            this.method = method;
            this.clazz = clazz;
            callCount = new int[siz];
            duration = new int[siz];
            exception = new int[siz];
            resolves = new int[siz];
        }

        public void registerEntry(int index) {
            callCount[index]++;
        }

        public void registerExit(int index, long dur) {
            duration[index] += (int)dur;
        }
    }

    public void registerEntry( String methodName, String targetClass ) {
        getStats(methodName,targetClass).registerEntry(getIndex(System.currentTimeMillis()));
    }

    public void registerExit( String methodName, String targetClass, long duration ) {
        getStats(methodName,targetClass).registerExit(getIndex(System.currentTimeMillis()),duration);
    }

    public void registerException(String methodName, String targetClass) {}
    public void registerResult(String methodName, String targetClass, boolean resolveElseReject) {}

    ConcurrentHashMap<String,Map<String,MethodStats>> entryMap = new ConcurrentHashMap<String, Map<String, MethodStats>>();

    MethodStats getStats(String methodName, String targetClass) {
        Map<String, MethodStats> stringMethodStatsMap = entryMap.get(targetClass);
        if ( stringMethodStatsMap == null ) {
            stringMethodStatsMap = new ConcurrentHashMap<>();
            entryMap.put(targetClass,stringMethodStatsMap);
        }
        MethodStats methodStats = stringMethodStatsMap.get(methodName);
        if ( methodStats == null ) {
            methodStats = new MethodStats(methodName,targetClass);
            stringMethodStatsMap.put(methodName,methodStats);
        }
        return methodStats;
    }

    int getIndex(long currenttim) {
        long timeOfDay = currenttim % dayLenMillis;
        int minuteOfDay = (int) (timeOfDay / TimeUnit.MINUTES.toMillis(1));
        return minuteOfDay % siz;
    }

    public static void main(String[] args) throws InterruptedException {
        PerformanceTracker pt = new PerformanceTracker();
        System.out.println("size:"+siz);
        while( true ) {
            System.out.println(pt.getIndex(System.currentTimeMillis()));
            Thread.sleep(10_000);
        }
    }


}
