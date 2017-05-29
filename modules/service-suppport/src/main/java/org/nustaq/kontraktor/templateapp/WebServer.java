package org.nustaq.kontraktor.templateapp;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpSyncActorAdaptorHandler;
import org.nustaq.kontraktor.remoting.http.builder.BldFourK;
import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.PlainService;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.services.web.IRegistration;
import org.nustaq.kontraktor.services.web.IWebServer;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.interfaces.Record;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by ruedi on 29.05.17.
 */
public class WebServer extends Actor<WebServer> implements IWebServer, IRegistration {

    PlainService service;

    public IPromise<WebServer> init(ServiceArgs options) {
        Promise p = new Promise();
        PlainService.createService("WebServer", null, options )
            .then( (r,e) -> {
                if ( r != null ) {
                    service = r;
                    p.resolve(self());
                } else {
                    p.reject(e);
                }
            });
        return p;
    }

    // IDataConnected
    @Override @CallerSideMethod
    public DataClient getDataClient() {
        return getActor().getDataClient();
    }

    /**
     * handles direct links (see interceptor below)
     *
     * here redirects /direct/register/regid to the IRegistration
     *
     * @param exchange
     */
    @Override public void handleDirectRequest(HttpServerExchange exchange) {
        Log.Info(this,"direct request received "+exchange);
        String requestPath = null;
        try {
            requestPath = URLDecoder.decode(exchange.getRequestPath(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.Error(this,e);
        }
        String[] tokens = requestPath.split("/");
        if ( tokens.length > 3 && "register".equals(tokens[1]) ) {
            handleRegistrationConfirmation(tokens,exchange);
            return;
        }
        exchange.setResponseCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        exchange.getResponseSender().send(getResponse(exchange));
        exchange.endExchange();
    }

    ///////////////////////////////// registration /////////////////////////////

    @Override
    public void sendConfirmationMail(String wapp, String email, String confId) {
        Log.Info(this,"sending mail to "+email+" confimation: "+confId);
    }

    ///////////////////////////////// .. registration /////////////////////////////



    /**
     * start as a cluster member expecting datastorage and service registry exists
     * @param args
     */
    public static void mainClustered(String[] args) {
        DispatcherThread.DUMP_CATCHED = true;
        WebServerArgs options = (WebServerArgs) ServiceArgs.parseCommandLine(args, new WebServerArgs());
        WebServer serv = Actors.AsActor(WebServer.class);
        serv.init( options );

        BldFourK builder = Http4K.Build(options.webHost, options.webPort)
            .fileRoot("/files", "run/files")
                .httpCachedEnabled(options.prod)
//            .httpHandler("/direct", new HttpSyncActorAdaptorHandler(serv))
            .resourcePath("/")
                .elements(
                    "src/main/web/client",
                    "src/main/web/bower_components"
                )
                .allDev(!options.prod)
                .handlerInterceptor( exchange -> { // can be used to intercept (e.g. redirect or direct respond) all requests coming in on this resourcepath
                    String requestPath = exchange.getRequestPath();
                    if ( requestPath == null || !requestPath.startsWith("/direct/") ) {
                        return false;
                    }
                    exchange.dispatch();
                    serv.handleDirectRequest(exchange);
                    return true;
                })
                .build()
            .httpAPI("/app/ep", serv)
                .coding(new Coding(SerializerType.JsonNoRef, ServiceRegistry.JSONCLASSES))
                .setSessionTimeout(5*60_000)
                .build();

        builder.build();
    }

    /**
     * "single process cluster"
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        DispatcherThread.DUMP_CATCHED = true;
        ServiceRegistry.main( new String[0] );
        DataShard.main( new String[] {"-sn","0", "-host", "localhost"} );

        Thread.sleep(2000);
        WebServer.mainClustered(new String[0]);
    }

}
