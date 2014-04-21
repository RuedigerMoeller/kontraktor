package de.ruedigermoeller.disruptorbench;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;

import java.io.OutputStream;
import java.util.Date;

/**
* Created by ruedi on 21.04.14.
*/
public class TestRequest {

    static FSTConfiguration confRead = FSTConfiguration.createDefaultConfiguration();
    static FSTConfiguration confWrite = FSTConfiguration.createDefaultConfiguration();

    // used for partitioning encoding/decoding
    public int decPartition;
    public int encPartition;

    byte [] rawRequest;
    LoadFeeder.Request req; // after decoding
    LoadFeeder.Response resp =  new LoadFeeder.Response(null,0);

    // can be multithreaded
    public void decode() throws Exception {
//            decount.incrementAndGet(); // debug
        final FSTObjectInput objectInput = confRead.getObjectInput(rawRequest);
        req = (LoadFeeder.Request) objectInput.readObject();
    }

    // single threaded or need synchronization in the SharedData implementation
    public void process(SharedData data) throws Exception {
        Integer result = data.lookup(req.data);
        if ( result == null )
            result = 0;
        // just mimic some simple business logic involving some alloc
        for (int i = 0; i < 20; i++) {
            Date d = new Date(result);
            result = (int) d.getTime();
        }
        resp.data = req.data;
    }

    // can be multithreaded
    public void encode(LoadFeeder serv) throws Exception {
        FSTObjectOutput out = confWrite.getObjectOutput((OutputStream) null);
        out.writeObject(resp);
        serv.response(out.getCopyOfWrittenBuffer());
        out.flush();
//            decount.incrementAndGet(); // debug
    }

}
