package org.nustaq.kontraktor.barebone;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.nustaq.kontraktor.barebone.serializers.BBActorRefSerializer;
import org.nustaq.kontraktor.barebone.serializers.BBCallbackRefSerializer;
import org.nustaq.kontraktor.barebone.serializers.BBTimeoutSerializer;
import org.nustaq.serialization.FSTConfiguration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 05/06/15.
 */
public class RemoteActor {

    protected static CloseableHttpAsyncClient asyncHttpClient;
    public static CloseableHttpAsyncClient getClient() {
        synchronized (RemoteActor.class) {
            if (asyncHttpClient == null ) {
                asyncHttpClient = HttpAsyncClients.custom()
                    .setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                            .setIoThreadCount(1)
                            .setSoKeepAlive(true)
                            .build()
                    ).build();
                asyncHttpClient.start();
            }
            return asyncHttpClient;
        }
    }

    FSTConfiguration conf;
    ScheduledExecutorService myThread = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService longPoller = Executors.newSingleThreadScheduledExecutor();

    // placeholder for serialized kontraktor classes
    static class _Actor {}
    static class _CallbackWrapper {}
    static class _Spore {}
    public static class _Timeout {}

    void initConf() {
        // code below need to resemble the exact configuration order of Kontraktor's RemoteRegistry
        conf = FSTConfiguration.createJsonConfiguration();
		conf.registerSerializer(_Actor.class, new BBActorRefSerializer(this), true);
		conf.registerSerializer(_CallbackWrapper.class, new BBCallbackRefSerializer(this), true);
		conf.registerSerializer(_Spore.class, new BBActorRefSerializer(this), true);
		conf.registerClass(BBRemoteCallEntry.class);
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", BBRemoteCallEntry.class.getName()},
            {"cbw", _CallbackWrapper.class.getName()}
        });
		conf.registerSerializer(_Timeout.class, new BBTimeoutSerializer(), false);
		// fixme: if app specific serializers have been configured they ought to go here ..
//        if (code.getConfigurator()!=null) {
//            code.getConfigurator().accept(conf);
//        }
    }

    public BBPromise connect(String url, boolean longPoll) {
        final BBPromise res = new BBPromise();
        if ( longPoll ) {
            final AtomicReference<Runnable> lp = new AtomicReference<>();
            lp.set(new Runnable() {
                @Override
                public void run() {
                    longPoller.schedule(lp.get(), 1000, TimeUnit.MILLISECONDS);
                }
            });
            longPoller.schedule(lp.get(), 1000, TimeUnit.MILLISECONDS);
        }
        myThread.submit(new Runnable() {
            @Override
            public void run() {
                res.complete(null,null);
            }
        });
        return res;
    }

    public void tell( String methodName, Object ... arguments ) {
    }

    public <T> BBPromise<T> ask( String methodName, Object ... arguments ) {
        BBPromise<T> res = new BBPromise<>();
        return res;
    }

}
