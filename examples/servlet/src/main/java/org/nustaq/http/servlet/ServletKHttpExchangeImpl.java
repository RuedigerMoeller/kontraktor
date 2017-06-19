package org.nustaq.http.servlet;

import org.nustaq.kontraktor.remoting.http.KHttpExchange;
import org.nustaq.kontraktor.util.Log;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletKHttpExchangeImpl implements KHttpExchange {

    KontraktorServlet servlet;
    AsyncContext aCtx;

    public ServletKHttpExchangeImpl(KontraktorServlet servlet, AsyncContext aCtx) {
        this.servlet = servlet;
        this.aCtx = aCtx;
    }

    @Override
    public void endExchange() {
        aCtx.complete();
    }

    @Override
    public void setResponseContentLength(int length) {
        aCtx.getResponse().setContentLength(length);
    }

    @Override
    public void setResponseCode(int i) {
        ((HttpServletResponse) aCtx.getResponse()).setStatus(i);
    }

    @Override
    public void send(String s) {
        try {
            aCtx.getResponse().getWriter().write(s);
        } catch (IOException e) {
            Log.Warn(this,e);
        }
    }

    @Override
    public void send(byte[] b) {
        try {
            send(new String(b,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.Error(this,e);
        }
    }

    @Override
    public void sendAuthResponse(byte[] response, String sessionId) {
        try {
            send(new String(response,"UTF-8"));
            aCtx.complete();
        } catch (UnsupportedEncodingException e) {
            Log.Error(this,e);
        }
    }
}
