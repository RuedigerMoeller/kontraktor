package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.ActorExecutorService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by moelrue on 5/4/15.
 */
public class AsyncFile {

    AsynchronousFileChannel fileChannel;
    AsyncFileIOEvent event = null;

    /**
     * create an unitialized AsyncFile. Use open to actually open a file
     */
    public AsyncFile() {
    }

    byte tmp[];
    /*
     * return a pseudo-blocking input stream. Note: due to limitations of the current await implementation (stack based),
     * when reading many files concurrently from a single actor thread don't mix high latency file locations (e.g. remote file systems vs. local)
     * with low latency ones. If this is required, fall back to the more basic read/write methods returning futures.
     */
    public InputStream asInputStream() {

        if ( tmp != null )
            throw new RuntimeException("can create Input/OutputStream only once");
        tmp = new byte[1];
        return new InputStream() {

            @Override
            public void close() throws IOException {
                AsyncFile.this.close();
            }


            @Override
            public int read() throws IOException {
                // should rarely be called, super slow
                int read = read(tmp, 0, 1);
                if ( read < 1 ) {
                    return -1;
                }
                return (tmp[0]+256)&0xff;
            }

            @Override
            public int read(byte b[], int off, int len) throws IOException {
                if ( event == null ) {
                    event = new AsyncFileIOEvent(0,0, ByteBuffer.allocate(len));
                }
                if ( event.getBuffer().capacity() < len ) {
                    event.buffer = ByteBuffer.allocate(len);
                }
                ByteBuffer buffer = event.buffer;
                event.reset();
                event = AsyncFile.this.read(event.getNextPosition(), len, buffer).await();
                int readlen = event.getRead();
                if ( readlen > 0 )
                    buffer.get(b,off,readlen);
                return readlen;
            }
        };
    }

    /**
     * return a pseudo-blocking output stream. Note: due to limitations of the current await implementation (stack based),
     * when writing many files concurrently from a single actor thread don't mix high latency file locations (e.g. remote file systems vs. local)
     * with low latency ones. If this is required, fall back to the more basic read/write methods returning futures.
     *
     * @return
     */
    public OutputStream asOutputStream() {
        if ( tmp != null )
            throw new RuntimeException("can create Input/OutputStream only once");
        tmp = new byte[1];
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                tmp[0] = (byte) b;
                write(tmp, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if ( event == null ) {
                    event = new AsyncFileIOEvent(0,0, ByteBuffer.allocate(len));
                }
                if ( event.getBuffer().capacity() < len ) {
                    event.buffer = ByteBuffer.allocate(len);
                }
                ByteBuffer buffer = event.buffer;
                event.reset();
                buffer.put(b,off,len);
                buffer.flip();
                event = AsyncFile.this.write(event.getNextPosition(), buffer).await();
                if ( event.getRead() != len )
                    throw new RuntimeException("unexpected. Pls report");
            }

            @Override
            public void close() throws IOException {
                AsyncFile.this.close();
            }

        };
    }

    /**
     * create an async file and open for read
     * @param file
     * @throws IOException
     */
    public AsyncFile(String file) throws IOException {
        open(Paths.get(file), StandardOpenOption.READ);
    }

    /**
     * create an async file and open with given options (e.g. StandardOptions.READ or 'StandardOpenOption.WRITE, StandardOpenOption.CREATE')
     * @param file
     * @throws IOException
     */
    public AsyncFile(String file, OpenOption... options) throws IOException {
        open(Paths.get(file), options);
    }

    public AsyncFile(Path file, OpenOption... options) throws IOException {
        open(file, options);
    }

    static FileAttribute[] NO_ATTRIBUTES = new FileAttribute[0];
    public void open(Path file, OpenOption... options) throws IOException {
        if ( fileChannel != null )
            throw new RuntimeException("can only open once");
        Actor sender = Actor.current();
        Set<OpenOption> set = new HashSet<OpenOption>(options.length);
        Collections.addAll(set, options);
        fileChannel = AsynchronousFileChannel.open(file, set, new ActorExecutorService(sender), NO_ATTRIBUTES);
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
        Actor sender = Actor.current();
        Promise p = new Promise();
        if (target == null) {
            target = ByteBuffer.allocate(chunkSize);
        }
        final long bufferStartPos = target.position();
        final ByteBuffer finalTarget = target;
        fileChannel.read(target, position, target, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                // FIXME: how to handle incomplete read. (currently burden on reader)
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

    public IPromise<AsyncFileIOEvent> write(long filePosition, ByteBuffer source) {
        if (fileChannel == null)
            throw new RuntimeException("file not opened");
        Actor sender = Actor.current();
        Promise p = new Promise();
        final long bufferStartPos = source.position();
        final ByteBuffer finalTarget = source;
        fileChannel.write(source, filePosition, source, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if ( source.remaining() > 0 ) {
                    // just retry (will enqueue new message/job to actor mailbox)
                    fileChannel.write(source,filePosition,source,this);
                } else {
                    long newPos = filePosition + finalTarget.limit() - bufferStartPos;
                    if (result < 0)
                        newPos = -1;
                    attachment.flip();
                    p.resolve(new AsyncFileIOEvent(newPos, result, finalTarget));
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                p.reject(exc);
            }
        });
        return p;
    }

    public void close() {
        try {
            fileChannel.close();
            fileChannel = null;
        } catch (IOException e) {
            Actors.throwException(e);
        }
    }
}
