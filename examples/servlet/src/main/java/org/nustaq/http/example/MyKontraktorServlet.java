package org.nustaq.http.example;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.servlet.KontraktorServlet;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import java.util.List;

@WebServlet(
    name = "MyKontraktorServlet",
    urlPatterns = {"/ep/*"},
    asyncSupported = true
)
public class MyKontraktorServlet extends KontraktorServlet {

    @Override
    protected void createAndInitFacadeApp(ServletConfig config) {
        facade = Actors.AsActor(ServletApp.class);
        ((ServletApp) facade).init(new BasicWebAppConfig());
    }

}
