package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.http.rest.HtmlString;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RequestResponse {

    public static final RequestResponse MSG_403 = new RequestResponse("HTTP/1.0 403 forbidden\n\n403 no no");
    public static final RequestResponse MSG_404 = new RequestResponse("HTTP/1.0 404 not found\n\n404 nope");
    public static final RequestResponse MSG_500 = new RequestResponse("HTTP/1.0 500 internal error\n\n500 something went wrong somewhere");
    public static final RequestResponse MSG_200 = new RequestResponse("HTTP/1.0 200 OK\nAccess-Control-Allow-Origin: *\n\n");;
    String data;
    HtmlString htmlData; // allow for plain html responses to ease quirksing
    byte binary[];

    public RequestResponse(String data) {
        this.data = data;
    }
    public RequestResponse(HtmlString data) {
        this.htmlData = data;
    }

    public RequestResponse(byte[] data) {
        this.binary = data;
    }

    public byte[] getBinary() {
        if ( binary != null )
            return binary;
        else
            return data.getBytes();
    }

    public boolean isHtml() {
        return htmlData != null;
    }

    @Override
    public String toString() {
        return binary != null ? new String(binary,0) : htmlData != null ? htmlData.getString() : data;
    }
}
