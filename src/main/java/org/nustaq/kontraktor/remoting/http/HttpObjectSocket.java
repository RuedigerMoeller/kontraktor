package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.kson.KsonDeserializer;
import org.nustaq.kson.KsonStringCharInput;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ruedi on 14.08.2014.
 */
public class HttpObjectSocket implements ObjectSocket {

    public static final int MAX_BATCHED_REQUESTS = 500;

    Class actorClz;
    int port = 9999;
    String host;
    String actorPath;

    Kson kson;
    ArgTypesResolver resolver;
    LinkedBlockingQueue<String> httpReqQueue;
    LinkedBlockingQueue<Object> httpRespQueue;

    public HttpObjectSocket(Class actorClz, int port, String host, String actorPath) {
        this.actorClz = actorClz;
        this.port = port;
        this.host = host;
        this.actorPath = actorPath;
        init();
    }

    protected void init() {
        kson = new Kson()
                .map("call", RemoteCallEntry.class)
                .map("calls", RemoteCallEntry[].class)
                .map("rcb", HttpRemotedCB.class);
        kson.getMapper().setUseSimplClzName(false);
        resolver = new ArgTypesResolver(actorClz);

        httpReqQueue = new LinkedBlockingQueue<>();
        httpRespQueue = new LinkedBlockingQueue<>();
        new Thread( ()->{
            ArrayList<String> calls = new ArrayList<>();
            StringBuilder sb = new StringBuilder(100);
            while(true) {
                try {
                    calls.add(httpReqQueue.take());
                    httpReqQueue.drainTo(calls,MAX_BATCHED_REQUESTS);
                    Socket socket = null;
                    while( calls.size() > 0 ) {
                        try {
                            socket = post(calls); //fixme: retry immediately in case of failure (no take)
                            calls.clear();
                        } catch (Exception ex) {
                            // fixme: add sequencing to avoid double execution
                            Thread.sleep(100); // retry after 100 millis
                        }
                    }

                    BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
                    String head;
                    while( (head = read.readLine()).trim().length() != 0 ) {
                        //
                    }
                    // fixme: check error
                    int ch;
                    sb.setLength(0);
                    while ( (ch=read.read()) > 0 ) {
                        sb.append((char)ch);
                    }
                    final String ksonString = sb.toString();
//                    System.out.println(ksonString);
                    if (ksonString.length() > 0 ) {
                        int resp = 0;
                        KsonStringCharInput in = new KsonStringCharInput(ksonString);
                        final KsonDeserializer deserializer = new KsonDeserializer(in, kson.getMapper());
                        while (in.position()<ksonString.length()) {
                            Object o = deserializer.readObject(RemoteCallEntry.class, String.class, null);
                            if ( o != null ) {
                                httpRespQueue.put(o);
                                resp++;
                            }
                            deserializer.skipWS();
                        }
//                        if (resp > 1)
//                            System.out.println("bundled "+resp);
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.Warn(this, e, "");
                }
            }
        }, "HttpObjectSocket:"+host+":"+port+actorPath ).start();
    }

    public Kson getKson() {
        return kson;
    }

    @Override
    public Object readObject() throws Exception {
        return httpRespQueue.take();
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        // expect callentry
        String sendString = kson.writeObject(toWrite,false);
        httpReqQueue.put(sendString);
    }

    @Override
    public void flush() throws IOException {
    }

    private Socket post(ArrayList<String> requests) throws IOException {
        InetAddress addr = InetAddress.getByName(host);

        String post = "[\n"; // fixme json
        int min = requests.size();
        for (int i = 0; i < min; i++) {
            post += requests.get(i);
        }
        post += "]";

        Socket socket = new Socket(addr, port);
        String path = actorPath;
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        bw.write("POST " + path + " HTTP/1.0\n");
        bw.write("Content-Length: " + post.length() + "\n");
        bw.write("Accept: text/kson\n"); // always use kson for internal traffic
        bw.write("Content-Type: application/kson\n");
        bw.write("\n");
        bw.write(post);
        bw.flush();
        return socket;
    }

    @Override
    public void setLastError(Exception ex) {

    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

    @Override
    public FSTConfiguration getConf() {
        return null;
    }
}
