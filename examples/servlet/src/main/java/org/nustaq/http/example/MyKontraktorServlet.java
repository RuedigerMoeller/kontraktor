package org.nustaq.http.example;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.servlet.KontraktorServlet;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

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
