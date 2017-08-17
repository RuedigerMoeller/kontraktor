package reactinstrinsic.servlet;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.servlet.KontraktorServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

@WebServlet(
    name = "MyReactServlet",
    urlPatterns = {"/reactservlet/*"},
    asyncSupported = true
)
public class MyReactServlet extends KontraktorServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected boolean isDevMode() {
        return true;
    }

    /**
     * @return 'classpath' for files (html,css,js,jsx
     */
    @Override
    protected String[] getResourcePathElements() {
        return new String[]{"client/", "lib/"};
    }

    @Override
    protected Actor createAndInitFacadeApp(ServletConfig config) {
        ReactServletApp reactServletApp = Actors.AsActor(ReactServletApp.class);
        reactServletApp.init(2);
        return reactServletApp;
    }

}
