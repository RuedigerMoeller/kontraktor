package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;

/**
 * Created by moelrue on 5/4/15.
 */
public class AsyncFile {
    AsynchronousFileChannel fileChannel;

    public AsyncFile() {
    }

    public AsyncFile(String file) throws IOException {
        open(Paths.get(file), StandardOpenOption.READ);
    }

    public AsyncFile(String file, OpenOption... options) throws IOException {
        open(Paths.get(file), options);
    }

    public AsyncFile(Path file, OpenOption... options) throws IOException {
        open(file, options);
    }

    public void open(Path file, OpenOption... options) throws IOException {
        Actor sender = Actor.sender.get();
        if (sender == null)
            throw new RuntimeException("must be called from inside an actor thread");
        fileChannel = AsynchronousFileChannel.open(file, options);
    }

    public long length() {
        try {
            return fileChannel.size();
        } catch (IOException e) {
            Actors.throwException(e);
        }
        return -1;
    }

    public IPromise<AsyncFileIOEvent> readFully() {
        ByteBuffer buf = ByteBuffer.allocate((int) length());
        AsyncFileIOEvent ev = new AsyncFileIOEvent(0,0,buf);
        do {
            ev = read(ev.nextPosition, (int) ((int) length() - ev.nextPosition), buf).await();
        } while( buf.limit() != buf.capacity() && ev.getNextPosition() >= 0 );
        return new Promise<>(ev);
    }

    public IPromise<AsyncFileIOEvent> read(long position, int chunkSize, ByteBuffer target) {
        if (fileChannel == null)
            throw new RuntimeException("file not opened");
        Actor sender = Actor.sender.get();
        if (sender == null)
            throw new RuntimeException("must be called from inside an actor thread");
        Promise p = new Promise();
        if (target == null) {
            target = ByteBuffer.allocate(chunkSize);
        }
        final long bufferStartPos = target.position();
        final ByteBuffer finalTarget = target;
        fileChannel.read(target, position, target, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                long newPos = position + finalTarget.limit() - bufferStartPos;
                if ( result < 0 )
                    newPos = -1;
                attachment.flip();
                p.resolve(new AsyncFileIOEvent(newPos, result, finalTarget));
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                p.reject(exc);
            }
        });
        return p;
    }

    public IPromise<AsyncFileIOEvent> write(long position, int chunkSize, ByteBuffer source) {
        if (fileChannel == null)
            throw new RuntimeException("file not opened");
        Actor sender = Actor.sender.get();
        if (sender == null)
            throw new RuntimeException("must be called from inside an actor thread");
        Promise p = new Promise();
        if (source == null) {
            source = ByteBuffer.allocate(chunkSize);
        }
        final long bufferStartPos = source.position();
        final ByteBuffer finalTarget = source;
        fileChannel.write(source, position, source, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                long newPos = position + finalTarget.limit() - bufferStartPos;
                if (result < 0)
                    newPos = -1;
                attachment.flip();
                p.resolve(new AsyncFileIOEvent(newPos, result, finalTarget));
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                p.reject(exc);
            }
        });
        return p;
    }

}
