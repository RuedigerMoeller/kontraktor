package de.ruedigermoeller.disruptorbench;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 20.04.14.
 */
public class LoadFeeder {


    public LoadFeeder(int numberOfPreallocedRequests) {
        try {
            setup(numberOfPreallocedRequests);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static class Request implements Serializable {

        String dummy;
        int data;

        public Request(String dummy, int data) {
            this.dummy = dummy;
            this.data = data;
        }
    }

    public static class Response implements Serializable {

        String dummy;
        int data;

        public Response(String dummy, int data) {
            this.dummy = dummy;
            this.data = data;
        }

        public Response() {
        }

        public String getDummy() {
            return dummy;
        }

        public void setDummy(String dummy) {
            this.dummy = dummy;
        }

        public int getData() {
            return data;
        }

        public void setData(int data) {
            this.data = data;
        }
    }

    public static interface Service {
        public void processRequest(byte[] b);
        void shutdown();
    }

    byte[][] requests;

    public void setup( int numberOfRequests ) throws IOException, ClassNotFoundException {
        requests = new byte[numberOfRequests][];
        for (int i = 0; i < requests.length; i++) {
            final FSTObjectOutput objectOutput = FSTConfiguration.getDefaultConfiguration().getObjectOutput();
            objectOutput.writeObject(new Request("hello"+i,i));
            requests[i] = objectOutput.getCopyOfWrittenBuffer();
            objectOutput.flush();
        }
    }

    HashMap sharedDate = new HashMap();
    public Object accessToSharedData(int data) {
        sharedDate.put(data,data);
        return null;
    }

    AtomicInteger respCount = new AtomicInteger(0);
    public void response(byte[] copyOfWrittenBuffer) {
        respCount.incrementAndGet();
    }

    public void run(Service service, int num2Send) {
        long tim = System.currentTimeMillis();
        int reqIndex = 0;
        for ( int i=0; i < num2Send; i++ ) {
            service.processRequest(requests[reqIndex++]);
            if ( reqIndex == requests.length )
                reqIndex = 0;
        }
        while (respCount.get()<num2Send)
            LockSupport.parkNanos(10000);
        System.out.println("tim " + (System.currentTimeMillis() - tim));
        service.shutdown();
    }

}
