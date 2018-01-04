package org.nustaq.reallive.api;

import java.io.Serializable;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableDescription implements Serializable, Cloneable {

    public enum StorageType {
        TEMP,
        PERSIST,
        CACHED
    }

    String name;
    int sizeMB = 1000;
    String filePath = TableSpace.USE_BASE_DIR;
    int numEntries=100_000;
    int shardNo;
    int keyLen = 48;
    String storageType = StorageType.CACHED.toString();

    public TableDescription() {}

    public TableDescription(String name) {
        this.name = name;
    }

    public TableDescription name(final String name) {
        this.name = name;
        return this;
    }

    public TableDescription storageType(final StorageType st) {
        this.storageType = st.toString();
        return this;
    }

    public StorageType getStorageType() {
        return StorageType.valueOf(storageType);
    }

    public TableDescription sizeMB(final int sizeMB) {
        this.sizeMB = sizeMB;
        return this;
    }

    public TableDescription filePath(final String filePath) {
        this.filePath = filePath;
        return this;
    }

    public TableDescription numEntries(final int numEntries) {
        this.numEntries = numEntries;
        return this;
    }

    public String getName() {
        return name;
    }

    public int getSizeMB() {
        return sizeMB;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getNumEntries() {
        return numEntries;
    }


    public int getShardNo() {
        return shardNo;
    }

    public TableDescription shardNo(final int shardNo) {
        this.shardNo = shardNo;
        return this;
    }

    public TableDescription keyLen(final int keyLen) {
        this.keyLen = keyLen;
        return this;
    }

    public int getKeyLen() {
        if ( keyLen <= 8 ) {
            throw new RuntimeException("keylen too short");
        }
        return keyLen;
    }

    @Override
    public TableDescription clone() {
        try {
            return (TableDescription) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
