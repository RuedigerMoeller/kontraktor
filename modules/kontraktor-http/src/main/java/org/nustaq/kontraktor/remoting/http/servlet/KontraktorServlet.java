package org.nustaq.kontraktor.remoting.http.servlet;

import io.undertow.server.handlers.resource.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldResPath;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.DynamicResourceManager;
import org.nustaq.kontraktor.webapp.javascript.HtmlImportShim;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;
import org.nustaq.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by ruedi on 19.06.17.
 */
public abstract class KontraktorServlet extends HttpServlet {

    protected Actor facade;
    protected ServletActorConnector connector;
    protected DynamicResourceManager dynamicResourceManager;
    protected String realRoot;
    protected long deployTime = System.currentTimeMillis();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        realRoot = findAppRoot();
        facade = createAndInitFacadeApp(config);
        connector = createAndInitConnector();
        dynamicResourceManager = createDependencyResolver(getResourcePathConfig());
    }

    protected String[] getResourcePathElementsAbsolute() {
        String[] rpe = getResourcePathElements();
        String res[] = new String[rpe.length];
        for (int i = 0; i < rpe.length; i++) {
            res[i] = realRoot+File.separator+rpe[i];
        }
        return res;
    }

    protected String findAppRoot() {
        String realPath = getServletContext().getRealPath("/");
        return realPath;
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        if ( isDevMode() )
            return super.getLastModified(req);
        return deployTime;
    }

    protected BldResPath getResourcePathConfig() {
        return new BldResPath(null, "/")
            .elements(getResourcePathElementsAbsolute())
            .transpile("jsx", new JSXIntrinsicTranspiler(isDevMode()))
            .allDev(isDevMode());
    }

    protected boolean isDevMode() {
        return true;
    }

    protected String[] getResourcePathElements() {
        return new String[]{"src/main/webapp/client/", "src/main/webapp/lib/"};
    }

    protected DynamicResourceManager createDependencyResolver(BldResPath dr) {
        DynamicResourceManager drm = new DynamicResourceManager(
            new File(realRoot),
            !dr.isCacheAggregates(),
            dr.getUrlPath(),
            dr.isMinify(),
            dr.getBaseDir(),
            dr.getResourcePath()
        );
        HtmlImportShim shim = new HtmlImportShim(dr.getUrlPath());
        shim
            .minify(dr.isMinify())
            .inline(dr.isInline())
            .stripComments(dr.isStripComments());
        drm.setImportShim(shim);
        drm.setTranspilerMap(dr.getTranspilers());
        return drm;
    }

    protected String getApiPath() {
        return "/api";
    }

    protected ServletActorConnector createAndInitConnector() {
        if ( facade  != null )
            return new ServletActorConnector(facade,this, new Coding(SerializerType.JsonNoRef), fail -> handleDisconnect(fail) );
        return null;
    }

    protected void handleDisconnect(Actor fail) {
        Log.Warn(this,"");
    }

    protected abstract Actor createAndInitFacadeApp(ServletConfig config); /** {
        facade = Actors.AsActor(ServletApp.class);
        ((ServletApp) facade).init();
    }**/

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ( pathInfo == null ) {
            String dbg = req.getRequestURI();
            resp.sendRedirect(dbg +"/");
            return;
        }
        Resource resource = null;
        if ( "".equals(pathInfo) || "/".equals(pathInfo) ) {
            resource = dynamicResourceManager.getResource(pathInfo + "index.html");
        } else {
            resource = dynamicResourceManager.getResource(pathInfo);
        }
        if ( resource != null) {
            Long contentLength = resource.getContentLength();
            if ( contentLength != null ) {
                resp.setContentLength((int)contentLength.longValue());
            }
            byte bytes[] = null;
            if ( resource instanceof DynamicResourceManager.MyResource) {
                bytes = ((DynamicResourceManager.MyResource) resource).getBytes();
            } else if ( resource.getFile() != null ) {
                bytes = FileUtil.readFully(resource.getFile());
            }
            // FIXME: mimetype, lastModified / 304
            if ( bytes != null ) {
                ServletOutputStream out = resp.getOutputStream();
                out.write(bytes);
                out.flush();
                out.close();
                return;
            }
        }
        Log.Error(this,"Unhandled resource");
        unhandledGet(req,resp);
    }

    protected void unhandledGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ( facade == null ) {
            nonAPIPost(req,resp);
            return;
        }
        String pathInfo = req.getPathInfo();
        if ( !pathInfo.startsWith(getApiPath()+"/") && ! pathInfo.equals(getApiPath()) ) {
            nonAPIPost(req, resp);
            return;
        }
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
                        connector.requestReceived(getApiPath(),aCtx,buffer);
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

    protected void nonAPIPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Log.Warn(this,"unhandled post "+req.getPathInfo());
        super.doPost(req,resp);
    }

}
