package org.nustaq.http.example;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.servlet.KontraktorServlet;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

@WebServlet(
    name = "MyKontraktorServlet",
    urlPatterns = {"/*"},
    asyncSupported = true
)
public class MyKontraktorServlet extends KontraktorServlet {

    protected String[] getResourcePathElements() {
        return new String[]{"src/main/webapp/client/", "src/main/webapp/lib/"};
    }

    @Override
    protected boolean isDevMode() {
        return true;
    }

    @Override
    protected Actor createAndInitFacadeApp(ServletConfig config) {
        Actor facade = Actors.AsActor(ServletApp.class);
        ((ServletApp) facade).init(4);
        return facade;
    }

}
