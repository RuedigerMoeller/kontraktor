package org.nustaq.machnetz;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.*;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.webserver.ClientSession;

/**
 * Created by ruedi on 25.05.14.
 */
public class MNClientSession<T extends MNClientSession> extends Actor<T> implements ClientSession {

    protected MachNetz server; // FIXME: iface

    int sessionId;

    public void $init(MachNetz machNetz, int sessionId) {
        server = machNetz;
    }

    @CallerSideMethod
    public int getSessionId() { return getActor().sessionId; }

    public void $onOpen(ChannelHandlerContext ctx) {

    }

    public void $onClose(ChannelHandlerContext ctx) {
        self().$stop();
    }

    public void $onTextMessage(ChannelHandlerContext ctx, String text) {
    }

    public void $onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
    }

}
