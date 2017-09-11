package microservice.impl;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.List;

public class MyServiceArgs {
    @Parameter(names="-url",required = true)
    List<String> connectUrls;
}
