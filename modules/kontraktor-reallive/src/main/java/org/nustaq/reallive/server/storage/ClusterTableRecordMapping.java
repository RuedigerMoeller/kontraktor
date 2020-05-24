package org.nustaq.reallive.server.storage;

import java.io.Serializable;
import java.util.BitSet;
import java.util.UUID;

public class ClusterTableRecordMapping implements Serializable {

    BitSet bs = new BitSet();
//    public static final int NUM_BUCKET = 512;
//    public static final long BUCKET_SHIFT = (32-9);
    public static final int NUM_BUCKET = 1<<5;
    public static final long BUCKET_SHIFT = (32-5);

    public boolean matches(int hashKey) {
        int bucket = getBucket(hashKey);
        return bs.get(bucket);
    }

    public int getBucket(int hashKey) {
        return hashKey>>>BUCKET_SHIFT;
    }

    public void setBucket(int index, boolean b) {
        bs.set(index,b);
    }

    ////////// perf exploration

    public static void checkBucket(ClusterTableRecordMapping mappings[], int num) {
        for (int i = 0; i < mappings.length; i++) {
            ClusterTableRecordMapping mapping = mappings[i];
            mapping.matches(num);
        }
    }

    public static void main(String[] args) {
        ClusterTableRecordMapping mappings[] = new ClusterTableRecordMapping[31];
        for (int i = 0; i < mappings.length; i++) {
            mappings[i] = new ClusterTableRecordMapping();
            mappings[i].setBucket( (int) (Math.random()*NUM_BUCKET), true );
        }
//        while( true ) {
//            int hash = UUID.randomUUID().toString().hashCode();
//            int bucket = mappings[0].getBucket(hash);
//            System.out.println(bucket);
//            if ( bucket <0 || bucket >= NUM_BUCKET )
//                throw new RuntimeException("pok");
//        }
        int hash = UUID.randomUUID().toString().hashCode();
        while( true ) {
            long now = System.currentTimeMillis();
            for ( int i = 0; i < 1_000_000; i++) {
                checkBucket(mappings, hash);
            }
            System.out.println("tim "+(System.currentTimeMillis()-now));
        }
    }

    @Override
    public String toString() {
        return "ClusterRecordMapping{" +
            "bs=" + bs +
            '}';
    }

    public BitSet getBitset() {
        return bs;
    }
}
