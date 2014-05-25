package org.nustaq.machnetz;

import de.ruedigermoeller.kontraktor.Actor;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.webserver.ClientSession;

/**
 * Created by ruedi on 25.05.14.
 */
public class MNClientSession extends Actor<MNClientSession> implements ClientSession {
    MachNetz server; // FIXME: iface

    public void init(MachNetz machNetz) {
        server = machNetz;
    }

    public void onOpen(ChannelHandlerContext ctx) {

    }

    public void onClose(ChannelHandlerContext ctx) {

    }

    public void onTextMessage(ChannelHandlerContext ctx, String text) {

    }

    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {

    }

}
