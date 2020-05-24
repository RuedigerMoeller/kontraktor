package org.nustaq.reallive.server.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 17/08/15.
 */
public class StorageStats implements Serializable {

    String name = "none";

    long usedMem;
    long freeMem;
    long capacity;
    int numElems;
    List<StorageStats> subStats;
    String tableName;

    public StorageStats name(final String name) {
        this.name = name;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public StorageStats tableName(final String tableName) {
        this.tableName = tableName;
        return this;
    }

    public StorageStats usedMem(final long usedMem) {
        this.usedMem = usedMem;
        return this;
    }

    public StorageStats freeMem(final long freeMem) {
        this.freeMem = freeMem;
        return this;
    }

    public StorageStats capacity(final long capacity) {
        this.capacity = capacity;
        return this;
    }

    public StorageStats numElems(final int numElems) {
        this.numElems = numElems;
        return this;
    }

    public long getUsedMem() {
        return usedMem;
    }

    public long getFreeMem() {
        return freeMem;
    }

    public long getCapacity() {
        return capacity;
    }

    public int getNumElems() {
        return numElems;
    }

    public String getName() {
        return name;
    }

    public void addTo(StorageStats storageStats) {
        storageStats.capacity += capacity;
        storageStats.numElems += numElems;
        storageStats.freeMem += freeMem;
        storageStats.usedMem += usedMem;
        storageStats.name = name;
        storageStats.addSubstats(this);
    }

    private void addSubstats(StorageStats storageStats) {
        if ( subStats == null ) {
            subStats = new ArrayList<StorageStats>();
        }
        subStats.add(storageStats);
    }

    public List<StorageStats> getSubStats() {
        return subStats;
    }

    @Override
    public String toString() {
        return "StorageStats{" +
                   "name='" + name + '\'' +
                   ", usedMem=" + usedMem/(1024*1024) + " MB"+
                   ", freeMem=" + freeMem/(1024*1024) + " MB"+
                   ", capacity=" + capacity + " MB"+
                   ", numElems=" + numElems +
                   ", subStats=" + subStats +
                   '}';
    }

    public String toStringLean() {
        return "" +
                   "name='" + name + '\'' +
                   ", usedMem=" + usedMem/(1024*1024) + " MB"+
                   ", freeMem=" + freeMem/(1024*1024) + " MB"+
                   ", capacity=" + capacity + " MB"+
                   ", numElems=" + numElems +
                   "";
    }

}
