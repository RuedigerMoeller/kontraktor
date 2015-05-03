package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 03/05/15.
 */
public class AsyncIO {

    static final AsyncIO singleton = new AsyncIO();

    public static AsyncIO get() {
        return singleton;
    }

    /**
     * stream a file asynchronously with given chunk size. The Pair in the callback class contains position and
     * the chunk data.
     *
     * Warning: as the buffer might get reused, its required to consume (copy or parse) it inside the callback.
     *
     * @param filepath
     * @param chunkSize
     * @param cb
     */
    public void streamFile(String filepath, int chunkSize, Callback<Pair<Integer, ByteBuffer>> cb) {
        Actor sender = Actor.sender.get();
        if ( sender == null )
            throw new RuntimeException("must be called from inside an actor thread");
        AsynchronousFileChannel fileChannel;
        try {
            fileChannel = AsynchronousFileChannel.open( Paths.get(filepath), EnumSet.of(StandardOpenOption.READ), null);
        } catch (IOException e) {
            cb.reject(e);
            return;
        }

        ByteBuffer buff = ByteBuffer.allocate(chunkSize);
        AtomicReference<CompletionHandler<Integer, ByteBuffer>> finalHandler = new AtomicReference<>();
        CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {
            long pos = 0l;
            @Override
            public synchronized void completed(Integer result, ByteBuffer buff) {
                if ( result < 0 ) {
                    cb.finish();
                    return;
                }
                buff.flip();
                cb.stream(new Pair(pos,buff));
                sender.yield(); // ensure buffer is consumed/copied by callback
                pos += buff.limit();
                buff.position(0);
                buff.limit(chunkSize);
                fileChannel.read(buff, pos, buff, finalHandler.get());
            }
            @Override
            public void failed(Throwable e, ByteBuffer attachment) {
                cb.reject(e);
            }
        };
        finalHandler.set(sender.getScheduler().inThread(sender, handler));
        fileChannel.read(buff, 0, buff, finalHandler.get() );
    }

}
