package org.nustaq.reactsample;

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

    // need to patch resourcepath as browserify expects node modules at same level as local imports
    @Override
    protected String[] getResourcePathElements(BasicWebAppConfig cfg) {
        return new String[]{
            cfg.getClientRoot(),
            cfg.getClientRoot() + "/node_modules",
//            cfg.getClientRoot() + "/bower_components"
        };
    }

    public static void main(String[] args) throws IOException {

        if ( !new File("./run/etc/app.kson").exists() ) {
            System.out.println("please run with working dir set to project root (react-with-babel)");
            System.exit(1);
        }

        ReactAppConfig cfg = ReactAppConfig.read();
        new ReactAppMain().reactMainHelper(false, ReactApp.class, cfg);

    }

}
