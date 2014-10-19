package org.nustaq.kontraktor.remoting.http;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RequestResponse {

    public static final RequestResponse MSG_404 = new RequestResponse("HTTP/1.0 404 not found\n\n404 nope");
    public static final RequestResponse MSG_500 = new RequestResponse("HTTP/1.0 500 internal error\n\n500 something went wrong somewhere");
    public static final RequestResponse MSG_200 = new RequestResponse("HTTP/1.0 200 OK\nAccess-Control-Allow-Origin: *\n\n");;
    String data;

    public RequestResponse(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }
}
