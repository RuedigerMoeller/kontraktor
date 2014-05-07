package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.annotations.*;

/**
 * Created by moelrue on 07.05.2014.
 */
public class RuedisPlayground {

    public static interface SomeCallbackHandler {
        public void callbackReceived( Object callback );
    }

    public static class ServiceActor extends Actor {

        public void getString( SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

        public void getStringAnnotated( @de.ruedigermoeller.kontraktor.annotations.InThread SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

    }

    public static class MyActor extends Actor {

        ServiceActor service;

        public void init(ServiceActor service) {
            this.service = service;
        }

        public void callbackTest() {
            final Thread callerThread = Thread.currentThread();
            service.getString(Actors.InThread(new SomeCallbackHandler() {
                @Override
                public void callbackReceived(Object callback) {
                    if (callerThread != Thread.currentThread()) {
                        throw new RuntimeException("Dammit");
                    } else {
                        System.out.println("Alles prima");
                    }
                }
            }));
            service.getStringAnnotated(new SomeCallbackHandler() {
                @Override
                public void callbackReceived(Object callback) {
                    if (callerThread != Thread.currentThread()) {
                        throw new RuntimeException("Dammit 1");
                    } else {
                        System.out.println("Alles prima 1");
                    }
                }
            });
        }

    }


    public static void main(String arg[]) throws InterruptedException {

        ServiceActor service = Actors.AsActor(ServiceActor.class);
        MyActor cbActor = Actors.AsActor(MyActor.class);
        cbActor.init(service);
        cbActor.callbackTest();

        Thread.sleep(10000);
    }

}
