package org.nustaq.reallive.server;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FilebasedRemoveLog extends Actor<FilebasedRemoveLog> implements RemoveLog {
    DataOutputStream writer;
    File file;
    Lock l = new ReentrantLock();

    public IPromise init(String dir, String tableName ) {
        try {
            file = new File(dir + File.separator + tableName + ".removelog");
            writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
        } catch (Exception e) {
            return reject(e);
        }
        return resolve();
    }

    @Override
    public void add(long timeStamp, String recordKey) {
        try {
            l.lock();
            writer.writeLong(timeStamp);
            writer.writeUTF(recordKey);
            debounce(TimeUnit.SECONDS.toMillis(5), "flush", () -> {
                try {
                    writer.flush();
                } catch (IOException e) {
                    Log.Error(this,e);
                }
            });
        } catch (IOException e) {
            Log.Warn(this,e);
        } finally {
            l.unlock();
        }
    }

    public void prune(long maxAge) {
        try {
            l.lock();
            long now = System.currentTimeMillis();
            writer.flush();
            List<RemoveLogEntry> buffer = new ArrayList<>();
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file))) ) {
                try {
                    while (in.available() > 0) {
                        long ts = in.readLong();
                        String s = in.readUTF();
                        if ( now - ts < maxAge )
                            buffer.add(new RemoveLogEntry(ts, s));
                    }
                } catch (Exception e) {
                    Log.Error(this,e);
                }
                writer.close();
                file.delete();
                writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, false)));
                buffer.forEach( en -> add(en.getTime(),en.getKey()) );
                writer.flush();
            }
        } catch (IOException e) {
            Log.Error(this,e);
        } finally {
            l.unlock();
        }
    }

    @Override
    public void query(long from, long to, Callback<RemoveLogEntry> en) {
        try {
            l.lock();
            writer.flush();
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            while ( in.available() > 0 ) {
                long ts = in.readLong();
                String s = in.readUTF();
                if (ts >= from && ts < to) {
//                    System.out.println("rec "+new Date(ts)+" "+new Date(from));
                    en.pipe(new RemoveLogEntry(ts, s));
                }
            }
            in.close();
            en.finish();
        } catch (Exception e) { // as we read while log is appended, there could be decoding errors when reading data in write
//            Log.Error(this,e);
            en.complete(null, e);
        } finally {
            l.unlock();
        }
    }

    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            Log.Error(this,e);
        }
    }
}
