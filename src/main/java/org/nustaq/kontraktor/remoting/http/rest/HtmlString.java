package org.nustaq.kontraktor.remoting.http.rest;

/**
 * Created by ruedi on 23.11.14.
 *
 * Special class recognized by server impls. Allows to return a html page instead of a JSon object.
 * Do not use for common file serving but occassional (e.g. when generating clickable email links).
 *
 * example async method:
 *
 * public Future<HtmlString> $linkResponse( String id ) {
 *     return new Promise(new HtmlString("<html>not found</html>"));
 * }
 *
 * can be accessed (e.g. 4k server maps facade actor to 'rest') GET rest/$linkResponse/102938
 *
 */
public class HtmlString {
    String string;

    public HtmlString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
