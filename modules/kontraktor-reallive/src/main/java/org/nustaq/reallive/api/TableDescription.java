package org.nustaq.reallive.api;

import java.io.Serializable;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableDescription implements Serializable, Cloneable {

    public static final String TEMP = "TEMP";
    public static final String PERSIST = "PERSIST";
    public static final String CACHED = "CACHED";

    String name;
    int sizeMB = 100;
    String filePath = TableSpace.USE_BASE_DIR;
    int numEntries=100_000;
    int shardNo;
    int keyLen = 48;
    String storageType = CACHED;
    String hashIndexed[] = {};

    public TableDescription() {}

    public TableDescription(String name) {
        this.name = name;
    }

    public TableDescription name(final String name) {
        this.name = name;
        return this;
    }

    public String[] getHashIndexed() {
        return hashIndexed;
    }

    public TableDescription storageType(final String st) {
        this.storageType = st.toString();
        return this;
    }

    public String getStorageType() {
        return storageType;
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
