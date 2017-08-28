package org.nustaq.intrinsicreact;

/**
 * Created by ruedi on 21.07.17.
 */

import org.nustaq.kontraktor.weblication.*;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * configure + start server. Requires working dir in project root ([..]examples/react)
 */
public class ReactAppMain extends UndertowWebServerMain {

    public static void main(String[] args) throws IOException {

        if ( !new File("./run/etc/app.kson").exists() ) {
            System.out.println("please run with working dir set to project root (react-no-babel)");
            System.exit(1);
        }

        ReactAppConfig cfg = ReactAppConfig.read();
        new ReactAppMain().reactMainHelper(true, ReactApp.class, cfg);

    }

}
