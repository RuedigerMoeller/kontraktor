package microservice.impl;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

public class MyServiceArgs {
    @Parameter(names="-url",required = true)
    String connectUrl;
    @Parameter(names = "-enc", description = "possible values: bin, json")
    String encoding;

    public SerializerType getEnd() {
        if ( "json".equalsIgnoreCase(encoding) )
            return SerializerType.JsonNoRef;
        if ( "bin".equalsIgnoreCase(encoding) )
            return SerializerType.FSTSer;
        throw new RuntimeException("unknown encoding:"+encoding+" valid:json,bin");
    }
}
