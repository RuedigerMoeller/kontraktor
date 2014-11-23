package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.http.rest.HtmlString;

import java.util.HashMap;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RequestResponse {

    public static final RequestResponse MSG_403 = new RequestResponse("HTTP/1.0 403 forbidden\n\n403 no no") {
        public int getStatusCode() { return 403; }
    };
    public static final RequestResponse MSG_404 = new RequestResponse("HTTP/1.0 404 not found\n\n404 nope") {
        public int getStatusCode() { return 404; }
    };
    public static final RequestResponse MSG_500 = new RequestResponse("HTTP/1.0 500 internal error\n\n500 something went wrong somewhere")  {
        public int getStatusCode() { return 500; }
    };
    public static final RequestResponse MSG_200 = new RequestResponse("HTTP/1.0 200 OK\nAccess-Control-Allow-Origin: *\n\n")  {
        public int getStatusCode() { return 200; }
    };
    public static final RequestResponse MSG_302(String s) {
        // need double location as some webservers do not allow to directly send full response. For those
        // a translation to a separate header + attribute + content is required.
        RequestResponse requestResponse = new RequestResponse("HTTP/1.0 302 FOUND\nLocation: " + s + "\n\n") {
            public int getStatusCode() {
                return 302;
            }
        };
        requestResponse.setLocation(s);
        return requestResponse;
    }

    String data;
    HtmlString htmlData; // allow for plain html responses to ease quirksing
    byte binary[];
    String location; // used for redirect. avoid hashmap for headers, as kontraktor uses a small subset of http

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

    public int getStatusCode() {
        return -1;
    }

    @Override
    public String toString() {
        return binary != null ? new String(binary,0) : htmlData != null ? htmlData.getString() : data;
    }

    /**
     * location property used for 302
     * @return
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
