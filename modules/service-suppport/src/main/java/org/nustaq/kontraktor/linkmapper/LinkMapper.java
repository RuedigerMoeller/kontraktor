package org.nustaq.kontraktor.linkmapper;

import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.http.undertow.builder.AutoConfig;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.Record;

import java.util.UUID;

public interface LinkMapper extends AutoConfig {

    static void auto(BldFourK bld, Object linkMapper) {
        bld.httpHandler("link", httpServerExchange ->  {
            httpServerExchange.dispatch();
            ((LinkMapper)linkMapper).handleRawHttp(httpServerExchange);
        });
    }

    DataClient getDClient();
    String handleSuccess( String linkId, Record linkRecord );
    String handleFailure( String linkId );

    /**
     * return uuid to use as link
     * @param rec
     * @return
     */
    default IPromise<String> putRegistrationRecord(Record rec /*e.g. maprecord*/ ) {
        String key  = UUID.randomUUID().toString();
        rec.key(key);
        getDClient().tbl("links" ).setRecord(rec.key(key));
        return Actors.resolve(key);
    }

    /**
     * assume registration on builder with e.g.
     *
     * .httpHandler("link", httpServerExchange ->  {
     *    httpServerExchange.dispatch();
     *    app.handleRawHttp(httpServerExchange);
     * })
     *
     * @param httpServerExchange
     */
    default void handleRawHttp(HttpServerExchange httpServerExchange) {
        String path = httpServerExchange.getRelativePath();
        if ( path.startsWith("/") )
            path = path.substring(1);
        String finalPath = path;
        getDClient().tbl("links").get(path).then( (rec, err) -> {
           if ( rec != null ) {
               httpServerExchange.setResponseCode(200).getResponseSender().send(handleSuccess(finalPath,rec));
           } else {
               httpServerExchange.setResponseCode(200).getResponseSender().send(handleFailure(finalPath));
           }
        });
    }

}