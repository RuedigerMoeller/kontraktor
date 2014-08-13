/* ---------------------------------------------------------------------------------------- */
/*                                                                                          */
/*    Copyright [2010] [System & Method A/S]                                                */
/*                                                                                          */
/*    Licensed under the Apache License, Version 2.0 (the "License");                       */
/*    you may not use this file except in compliance with the License.                      */
/*    You may obtain a copy of the License at                                               */
/*                                                                                          */
/*        http://www.apache.org/licenses/LICENSE-2.0                                        */
/*                                                                                          */
/*    Unless required by applicable law or agreed to in writing, software                   */
/*    distributed under the License is distributed on an "AS IS" BASIS,                     */
/*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.              */
/*    See the License for the specific language governing permissions and                   */
/*    limitations under the License.                                                        */
/*                                                                                          */
/*    Design - Niels Liisberg                                                               */
/*                                                                                          */
/* ---------------------------------------------------------------------------------------- */
package icebreakrest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.net.URLDecoder;

/**
 * Super tiny HTTP serverside protocol for monolitic RESTservice applications
 * Simply drop the IceBreakRestServer jar file in your project ( classpath) and you are golden.
 *
 * A simple server looks like this:
 *
 *
 * <pre>
 * {@code
 * // Drop this jar-file into you project
 * import IceBreakRestServer.*;
 * import java.io.IOException;
 * public class Simple {
 *
 *   public static void main(String[] args) {
 *
 *     // Declare the IceBreak HTTP REST server class
 *     IceBreakRestServer rest;
 *
 *
 *     try {
 *
 *       // Instantiate it once
 *       rest  = new IceBreakRestServer();
 *
 *       while (true) {
 *
 *         // Now wait for any HTTP request
 *         // the "config.properties" file contains the port we are listening on
 *         rest.getHttpRequest();
 *
 *         // If we reach this point, we have received a request
 *         // now we can pull out the parameters from the query-string
 *         // if not found we return the default "N/A"
 *         String name = rest.getQuery("name", "N/A");
 *
 *         // we can now produce the response back to the client.
 *         // That might be XML, HTML, JSON or just plain text like here:
 *         rest.write("Hello world - the 'name' parameter is: " + name );
 *       }
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *     }
 *   }
 * }
 * }
 * </pre>
 */


public class IceBreakRestServer {

    private ServerSocket providerSocket = null;
    private Socket connection = null;
    private PrintWriter pw;
    private String ContentType;
    private String Status;
    private StringBuilder resp  = new StringBuilder(1024);
    private boolean doFlush = false;
    private int Port ;
    private int Queue ;
    private InputStream in;

    /** This is the complete querysting including the resource. Just as you write it in your browser - you have to URL decode it or rather use getQuery to get paramter */
    public  String request;
    /** This is the contents sent by a POST  */
    public  String payload;
    /** This is the request type GET, POST, HEAD - your application have to responde coretly to this ( ore simply ignore it */
    public  String method ;
    /** This is the complete querysting after the resource as you write it in your browser - you have to URL decode it or rather use getQuery to get paramter */
    public  String queryStr;
    /** This is the name of the resource to run or get i.e. http://x/myApp.aspx/p1=abc it will return /myApp.aspx  */
    public  String resource;
    /** This is the version of the HTTP protocol requested   */
    public  String httpVer;
    /** Set this to true to get some system.out.print */
    public  Boolean debug = false;

    /** This is the HTTP headers in the request. Use normal "Map" methods  */
    public  Map<String, String> header = new HashMap<String, String>();
    /** This is the HTTP quesystring parameters as map. Use normal "Map" methods or getQuery() method  */
    public  Map<String, String> parms  = new HashMap<String, String>();

    private void loadProps ( ) {
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException ex) {
            // ex.printStackTrace();
        }
        Port  = Integer.parseInt(prop.getProperty("restserver.port","65000"));
        Queue = Integer.parseInt(prop.getProperty("restserver.queuesize", "10"));
    }
    /**
     * Contructor, returns an instance of the rest server
     */
    public IceBreakRestServer() {
        loadProps ();
    }

    /**
     * Set the contents type of the HTTP contens. It has to conform
     * the mime type. By default it has the value of "text/plain; charset=utf-8"
     * @param contents type string to set
     */
    public void setContentType(String s) {
        ContentType = s;
    }
    /**
     * Set the status of HTTP contens.
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP status codes</a>
     * @param status string . by default the is "200 OK"
     */
    public void setStatus(String s) {
        Status = s;
    }

    /**
     * Set the TCP/IP port that you server is listening on. This is by default port 65000 and you can
     * set this value in the config.prperties file. Or you can set it programatically here but before issuing
     * a "getRequest()".
     * @param port TCP/IP port to listen on
     */
    public void setPort(int port) {
        Port = port;
    }

    /**
     * Set the TCP/IP queue depth for your HTTP server . This is by default port 10 and you can
     * set this value in the config.prperties file. Or you can set it programatically here but before issuing
     * a "getRequest()".
     * @param port TCP/IP port to listen on
     */
    public  void setQueue(int queue) {
        Queue = queue;
    }

    /**
     * Returns the parameter from the querystring with the name of "key". if the querystring
     * parameter was not fount it will return the default paramter
     * Note: key is case sensitive!!
     * @param Key - to return value for in the querystring
     * @param Default - when key is not found this wil be the default value
     * @return value of the querystring parameter
     */
    public  String getQuery(String Key , String Default) {
        String temp = parms.get(Key);
        if (temp == null) return Default;
        return temp;
    }

    /**
     * Returns the parameter from the querystring with the name of "key". if the querystring
     * parameter was not fount it will <code>null</code>
     * Note: key is case sensitive!!
     * @param Key - to return value for in the querystring
     * @return value of the querystring parameter
     */
    public  String getQuery(String Key) {
        return parms.get(Key);
    }

    /**
     * Just return a simple string with current timestamp in hh:mm:ss format
     * @return current time in hh:mm:ss format
     */
    public String now () {
        String s;
        Format formatter;
        Date date = new Date();
        formatter = new SimpleDateFormat("hh:mm:ss");
        s = formatter.format(date);
        return s;
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)  {
            int p = param.indexOf('=');
            if (p >= 0) {
                String name = param.substring( 0, p);
                String value = param.substring( p+1);
                String s = URLDecoder.decode(value);
                map.put(name, s);
            }
        }
        return map;
    }

    // This handles both windows <CR><LF> and mac/aix/linux <CR>
    // and returns both end of header and end of line sequence
    private int isEol(byte [] buf , int i) {
        if (buf[i] == 0x0d &&buf[i+1] == 0x0a) {
            if (buf[i+2] == 0x0d &&buf[i+3] == 0x0a) {
                return -4; // End Of header
            }
            return 2;
        }
        if (buf[i] == 0x0d ) {
            if (buf[i+1] == 0x0d ) {
                return -2; // End Of header
            }
            return 1;
        }
        if (buf[i] == 0x0a ) {
            if (buf[i+1] == 0x0a ) {
                return -2; // End Of header
            }
            return 1;
        }
        return 0;
    }


    private void unpackRequest() throws IOException {

        byte buf [] = new byte[32768];
        in = connection.getInputStream();
        int read = in.read(buf);
        int len =0, pos =0, eol=0;
        header.clear();
        parms.clear();
        request = payload = method = queryStr = httpVer = resource = null;
        for (int i = 0; i < read && eol >= 0; i++) {
            eol  = isEol(buf , i);
            if (eol > 0) {
                // First line is the request. Now parse that partial
                if (request == null) {
                    request =  new String(buf, pos  , len);
                    String [] temp = request.split(" ");
                    method = temp[0];
                    queryStr = temp[1];
                    httpVer = temp[2];
                    int p = queryStr.indexOf('?');
                    if (p>=0) {
                        resource = queryStr.substring( 0, p);
                        parms = getQueryMap(queryStr.substring( p+1));
                    } else {
                        resource = queryStr;
                    }
                    // Following lines are the header - put them into a map
                } else {
                    String param  =  new String(buf, pos  , len);
                    int p = param.indexOf(':');
                    String name = param.substring( 0, p);
                    String value = param.substring( p+1);
                    header.put(name, value.trim());
                }
                len = 0;
                pos = i + eol;
                i+=eol-1;
            } else if (eol < 0) {
                pos = i + (-eol);
                payload = new String(buf, pos , read - pos);
            } else {
                len ++;
            }
        }

        // this is only for debugging
        if (debug) {
            System.out.println("resource: " + request);
            System.out.println("method: " + method);
            System.out.println("resource: " + resource);
            System.out.println("queryStr: " + queryStr);
            System.out.println("httpVer: " + httpVer);
            System.out.println("header  : " + header   );
            System.out.println("parms : " + parms  );
        }

    }

    private void sendResponse () {

        pw.print("HTTP/1.1 " + Status + "\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Accept: multipart/form-data\r\n"+
                "Accept-Encoding: multipart/form-data\r\n" +
                "Server: IceBreak Java Services\r\n" +
                "cache-control: no-store\r\n" +
                "Content-Length: " + Integer.toString(resp.length()) + "\r\n" +
                "Content-Type: " + ContentType + "\r\n" +
                "\r\n" + resp.toString());
        pw.flush();
    }

    /**
     * This waits for the next HTTP request from the client
     *
     */
    public void getHttpRequest () throws IOException {
        if (providerSocket == null) {
            providerSocket = new ServerSocket(Port , Queue);
        }
        if (doFlush) flush();

        connection = providerSocket.accept();
        pw = new PrintWriter(connection.getOutputStream());
        resp.setLength(0);
        unpackRequest();
        ContentType = "text/plain; charset=utf-8";
        Status = "200 OK";
        doFlush = true;
    }

    /**
     * write a string back a tring to the client. The complete result will be
     * send back to the client when you issue a "flush" or do the next "getHttpRequest()"
     * @param String - to send back to the client
     */
    public void write(String s) {
        resp.append(s);
    }

    /**
     * send back the complete response to the client. Now we are ready to wait for the next request by issue a "getHttpRequest()"
     */
    public void flush() throws IOException {
        sendResponse ();
        connection.close();
        doFlush = false;
    }
}