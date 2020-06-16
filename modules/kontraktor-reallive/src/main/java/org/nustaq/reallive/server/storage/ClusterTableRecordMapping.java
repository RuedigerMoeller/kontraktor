package org.nustaq.reallive.server.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class ClusterTableRecordMapping implements Serializable {

    BitSet bs = new BitSet();
//    public static final int NUM_BUCKET = 512;
//    public static final long BUCKET_SHIFT = (32-9);
    public static final int NUM_BUCKET = 1<<5;

    public static ClusterTableRecordMapping Copy(ClusterTableRecordMapping old) {
        ClusterTableRecordMapping nm = new ClusterTableRecordMapping();
        nm.addBuckets(old.getBucketsAsIA());
        return nm;
    }

    public boolean matches(int hashKey) {
        int bucket = getBucket(hashKey);
        return bs.get(bucket);
    }

    public int getBucket(int hashKey) {
        return (hashKey&0x7fffffff)%NUM_BUCKET;
    }

    public void addBuckets( int[] buckets ) {
        for (int i = 0; i < buckets.length; i++) {
            bs.set(buckets[i],true);
        }
    }

    public int[] getBucketsAsIA() {
        int res[] = new int[bs.cardinality()];
        int resC = 0;
        for ( int i=0; i < NUM_BUCKET; i++ ) {
            if ( bs.get(i) ) {
                res[resC++] = i;
            }
        }
        return res;
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

    public void remove(int[] hashShards2Move) {
        for (int i = 0; i < hashShards2Move.length; i++) {
            int shard = hashShards2Move[i];
            setBucket(shard,false);
        }
    }
}
