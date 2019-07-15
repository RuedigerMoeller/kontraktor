package org.nustaq.kontraktor.apputil;

import com.google.common.io.Files;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.nustaq.kontraktor.Actors.resolve;

class ExposeEntry {
    File f;
    String type;

    public ExposeEntry(File f, String type) {
        this.f = f;
        this.type = type;
    }
}

public interface FileExposerMixin<SELF extends Actor<SELF>> {

    String URLPREFIX = "exposed";
    ConcurrentHashMap<String,ExposeEntry> exposedDocuments = new ConcurrentHashMap<>();

    static void auto(BldFourK bld, Object mixin) {
        bld.httpHandler(URLPREFIX, httpServerExchange ->  {
            httpServerExchange.dispatch();
            httpServerExchange
                .setResponseCode(200);
            ((FileExposerMixin)mixin).handleExposedFileAccess(httpServerExchange);
        });
    }

    default void handleExposedFileAccess(HttpServerExchange httpServerExchange) {
        try {
            String requestPath = httpServerExchange.getRequestPath();
            String[] split = requestPath.split("/");
            String docId = split[split.length - 1];
            ExposeEntry exposeEntry = takeFile(docId);
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, exposeEntry.type+"; charset=utf-8");
            httpServerExchange.getResponseSender().send(ByteBuffer.wrap(Files.toByteArray(exposeEntry.f)));
        } catch (Exception e) {
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        }
    }

    @CallerSideMethod
    default String exposeFile(File localPath, String type) {
        String uid = UUID.randomUUID().toString();
        exposedDocuments.put(uid,new ExposeEntry(localPath,type));
        return uid;
    }

    @CallerSideMethod
    default ExposeEntry takeFile(String id) {
        ExposeEntry entry = exposedDocuments.get(id);
        exposedDocuments.remove(id);
        return entry;
    }

}
