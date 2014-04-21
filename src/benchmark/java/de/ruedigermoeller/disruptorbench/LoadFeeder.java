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

    private static final boolean BIGMSG = false;

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

        protected String dummy;
        protected int data;
        HashMap encodingWork; // give en/decoding something todo in case

        public Request(String dummy, int data) {
            this.dummy = dummy;
            this.data = data;
            if ( BIGMSG ) {
                encodingWork = new HashMap();
                for (int i = 0; i < 10; i++)
                    encodingWork.put(i, "hallo " + i);
            }
        }
    }

    public static class Response extends Request {

        public Response(String dummy, int data) {
            super(dummy,data);
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
