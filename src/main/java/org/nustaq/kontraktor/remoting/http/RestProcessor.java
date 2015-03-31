package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Callback;

/**
* Created by ruedi on 28/03/15.
*/
public class RestProcessor {

    private RestActorServer restActorServer;

    public RestProcessor(RestActorServer restActorServer) {this.restActorServer = restActorServer;}

    public boolean processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response) {
        if ( req.isGET() ) {
            String actor = req.getPath(0);
            final RestActorServer.PublishedActor target = restActorServer.publishedActors.get(actor);
            if ( target == null ) {
                return false;
            } else {
                if ( restActorServer.remoteCallInterceptor != null && !restActorServer.remoteCallInterceptor.apply(target.actor,req.getPath(1)) ) {
                    response.complete(RequestResponse.MSG_403, null);
                    response.complete(null, RestActorServer.FINISHED);
                    return true;
                } else
                    restActorServer.enqueueCall(target, req, response);
            }
        } else if (req.isPOST() ) {
            String actor = req.getPath(0);
            final RestActorServer.PublishedActor target = restActorServer.publishedActors.get(actor);
            if ( target == null ) {
                return false;
            } else {
                restActorServer.enqueueCall(target, req.getText().toString(), req, response);
            }
        } else {
            return false;
        }
        return true;
    }
}
