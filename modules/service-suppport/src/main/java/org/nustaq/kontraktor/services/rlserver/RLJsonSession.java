package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.services.rlclient.DataClient;

public class RLJsonSession extends Actor<RLJsonSession> implements RemotedActor {

    private RLJsonServer server;
    private DataClient dClient;

    public void init(RLJsonServer server, DataClient dataClient ) {
        this.server = server;
        this.dClient = dataClient;
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {

    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {

    }

}
