package org.nustaq.kontraktor.remoting.http.servlet;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.util.Log;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ruedi on 19.06.17.
 */
@WebServlet(
    name = "KontraktorServler",
    urlPatterns = {"/ep/*"},
    asyncSupported = true
)
public abstract class KontraktorServlet extends HttpServlet {

    protected Actor facade;
    protected ServletActorConnector connector;

    @Override
    public void init(ServletConfig config) throws ServletException {
        createAndInitFacadeApp(config);
        createAndInitConnector();
        super.init(config);

    }

    protected void createAndInitConnector() {
        connector = new ServletActorConnector(facade,this, new Coding(SerializerType.JsonNoRef), fail -> handleDisconnect(fail) );
    }

    protected void handleDisconnect(Actor fail) {
        Log.Warn(this,"");
    }

    protected abstract void createAndInitFacadeApp(ServletConfig config); /** {
        facade = Actors.AsActor(ServletApp.class);
        ((ServletApp) facade).init();
    }**/

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ServletOutputStream out = resp.getOutputStream();
        out.write("hello kontraktor".getBytes());
        out.flush();
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext aCtx = req.startAsync(req, resp);
        ServletRequest contextRequest = aCtx.getRequest();
        ServletInputStream inputStream = contextRequest.getInputStream();
        inputStream.setReadListener(new ReadListener() {
            byte buffer[];
            int index = 0;

            @Override
            public void onDataAvailable() throws IOException {
                if ( buffer == null ) {
                    int available = aCtx.getRequest().getContentLength();
                    buffer = new byte[available];
                }
                int c;
                while( inputStream.isReady() && (c=inputStream.read()) != -1 ) {
                    buffer[index++] = (byte) c;
                }
                if ( index == buffer.length ) {
                    facade.execute( () -> {
                        connector.requestReceived(aCtx,buffer);
                    });
                }
            }

            @Override
            public void onAllDataRead() throws IOException {
                // not called reliably by tomcat 8.5.x
            }

            @Override
            public void onError(Throwable throwable) {
                Log.Error(KontraktorServlet.this,throwable);
            }
        });
    }

}
