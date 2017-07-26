package org.nustaq.reactsample;

/**
 * Created by ruedi on 21.07.17.
 */

import org.nustaq.kontraktor.babel.BabelOpts;
import org.nustaq.kontraktor.babel.JSXTranspiler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.weblication.*;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * configure + start server. Requires working dir in project root ([..]examples/react)
 */
public class ReactAppMain extends UndertowWebServerMain {

    public static void main(String[] args) throws IOException {

//        BasicWebAppConfig cfg = BasicWebAppConfig.read();
        ReactAppConfig cfg = ReactAppConfig.read();
        new ReactAppMain().reactMainHelper(ReactApp.class,cfg);

    }

}
