package org.nustaq.reactsample;

/**
 * Created by ruedi on 21.07.17.
 */

import org.nustaq.kontraktor.weblication.*;

import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * configure + start server. Requires working dir in project root ([..]examples/react)
 */
public class ReactAppMain extends UndertowWebServerMain {

    public static void main(String[] args) throws IOException {

        ReactAppConfig cfg = ReactAppConfig.read();
        new ReactAppMain().reactMainHelper(ReactApp.class,cfg);

    }

}
