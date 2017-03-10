package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import org.nustaq.kontraktor.remoting.service.DenialReason;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

import java.util.HashMap;

/**
 * Created by ruedi on 09.03.17.
 */
public class WapiServer extends Actor<WapiServer> {

    static int REGPORT = 6665;
    static int PORT = 7777;

    WapiRegistry registry;

    HashMap<String,Actor> serviceMap;

    @Local
    public void init() {
        serviceMap = new HashMap<>();

        WapiRegistry serviceRegistry = Actors.AsActor(WapiRegistry.class);
        serviceRegistry.init();
        new TCPNIOPublisher(serviceRegistry, REGPORT).publish(actor -> {
            Log.Info(null, actor + " has disconnected");
        });
        // log service activity
        serviceRegistry.subscribe((pair, err) -> {
            if ( pair.car().equals(WapiRegistry.AVAILABLE)) {
                WapiDescription desc = pair.cdr();
//                Forwarder forwarder = Actors.AsActor(Forwarder.class,getScheduler());
//                forwarder.remoteRef(desc.getRemoteRef());
//                HttpPublisher pub = new HttpPublisher(forwarder,"localhost", desc.getName().toLowerCase(), PORT)
//                    .coding(new Coding(SerializerType.JsonNoRef, new Class[] {DenialReason.class} ));
//                pub.publish( act -> {
//                        System.out.println("DISCON "+desc);
//                    } //, new TestConstraints()
//                );
                // put to servicemap
                int debug = 1;
            } else if ( pair.car().equals( WapiRegistry.TIMEOUT ) ) {
                // remove
            }
            Log.Info(serviceRegistry.getClass(), pair.car() + " " + pair.cdr());
        });
    }

    public static void main(String[] args) {
        WapiServer wsTest = Actors.AsActor(WapiServer.class);
        wsTest.init();

        HttpPublisher pub = new HttpPublisher(wsTest,"localhost","api", PORT)
            .coding(new Coding(SerializerType.JsonNoRef, new Class[] {DenialReason.class} ));
        pub.publish( act -> {
                System.out.println("DISCON");
            } //, new TestConstraints()
        );
    }

}
