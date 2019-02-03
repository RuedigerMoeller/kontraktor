package org.nustaq.kontraktor.apputil;

import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.Record;

import java.util.UUID;

/**
 * associates a link with a record in Table links + built in support for registration handling.
 * Assumes implementation by ServerActor
 *
 * @param <SELF>
 */
public interface LinkMapperMixin<SELF extends Actor<SELF>> {

    String LinkTableName = "links";

    static void auto(BldFourK bld, Object linkMapper) {
        bld.httpHandler(LinkTableName, httpServerExchange ->  {
            httpServerExchange.dispatch();
            ((LinkMapperMixin)linkMapper).handleLinkHttp(httpServerExchange);
        });
    }

    @CallerSideMethod @Local DataClient getDClient();

    /**
     * @param linkId
     * @param linkRecord
     * @return htmlpage to render
     */
    @CallerSideMethod @Local String handleLinkSuccess(String linkId, Record linkRecord );

    /**
     * @param linkId
     * @return htmlpage to render
     */
    @CallerSideMethod @Local String handleLinkFailure(String linkId);

    SELF getActor();

    /**
     * return uuid to use as link
     * @param rec
     * @return
     */
    default IPromise<String> putRecord(Record rec /*e.g. maprecord*/ ) {
        String key  = UUID.randomUUID().toString();
        rec.key(key);
        getDClient().tbl(LinkTableName).setRecord(rec.key(key));
        return Actors.resolve(key);
    }

    /**
     * assume registration on builder with e.g.
     *
     * .httpHandler("link", httpServerExchange ->  {
     *    httpServerExchange.dispatch();
     *    app.handleLinkHttp(httpServerExchange);
     * })
     *
     * @param httpServerExchange
     */
    default void handleLinkHttp(HttpServerExchange httpServerExchange) {
        String path = httpServerExchange.getRelativePath();
        if ( path.startsWith("/") )
            path = path.substring(1);
        String finalPath = path;
        getDClient().tbl(LinkTableName).get(path).then( (rec, err) -> {
           if ( rec != null ) {
               httpServerExchange.setResponseCode(200).getResponseSender().send(((LinkMapperMixin)getActor()).handleLinkSuccess(finalPath,rec));
           } else {
               httpServerExchange.setResponseCode(200).getResponseSender().send(((LinkMapperMixin)getActor()).handleLinkFailure(finalPath));
           }
        });
    }

}