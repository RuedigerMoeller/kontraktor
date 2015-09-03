package org.nustaq.reallive.impl.tablespace;

import java.util.ArrayList;

/**
 * Created by moelrue on 03.09.2015.
 */
public class ConsistentHash<T> {

    final int SEGMENTS = 1013;

    Object primary[] = new Object[SEGMENTS];

    ArrayList<T> servers = new ArrayList<>();

    public ArrayList<MoveEntry<T>> addServer( T serverId ) {
        ArrayList<MoveEntry<T>> res = new ArrayList<>();
        if ( !servers.contains(serverId) )
            servers.add(serverId);
        else
            return res;
        int curServers = servers.size();
        int step = curServers*2;
        int count = 0;
        int i = (int) (step * Math.random());
        int max = SEGMENTS / servers.size();
        while (count < max) {
            T old = (T) primary[i];
            if ( ! serverId.equals(old) ) {
                primary[i] = serverId;
                if (old != null)
                    res.add(new MoveEntry<T>(i, old, serverId));
                count++;
            }
            i += (int)(step * Math.random());
            if ( i >= SEGMENTS ) {
                i-= SEGMENTS;
            }
        }
        return res;
    }

    public void dump() {
        System.out.println("-- "+servers.size());
        for (int i = 0; i < primary.length; i++) {
            Object o = primary[i];
            System.out.println("["+i+"] "+o);
        }
    }

    public void dumpDist() {
        System.out.println("-- "+servers.size());
        for (int i = 0; i < servers.size(); i++) {
            T t = servers.get(i);
            System.out.println(""+t+": "+countEntries(t));
        }
    }

    public int countEntries(T serverId) {
        int res = 0;
        for (int i = 0; i < primary.length; i++) {
            if ( serverId.equals(primary[i]) ) {
                res++;
            }
        }
        return res;
    }

    public static class MoveEntry<T> {
        int segment;
        T prevServer;
        T newServer;

        public MoveEntry(int segment, T prevServer, T newServer) {
            this.segment = segment;
            this.prevServer = prevServer;
            this.newServer = newServer;
        }

        public int getSegment() {
            return segment;
        }

        public T getPrevServer() {
            return prevServer;
        }

        public T getNewServer() {
            return newServer;
        }
    }

    public static void main(String[] args) {
        ConsistentHash ch = new ConsistentHash();
        String names[] = { "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "ELEVEN", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN" };
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            int moved = ch.addServer(name).size();
            System.out.print("MOVED "+moved);
            ch.dumpDist();
        }
    }

}
