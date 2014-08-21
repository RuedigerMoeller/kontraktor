package org.nustaq.kontraktor.remoting.http;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by ruedi on 13.08.2014.
 */
public class KontraktorHttpRequestImpl implements KontraktorHttpRequest // avoid clash with servlet api
{

    byte[] bytes;
    StringBuilder text = new StringBuilder(1000);
    StringBuilder methodString = new StringBuilder();
    StringBuilder pathString = new StringBuilder();
    String splitPath[];
    int contentStart;
    int contentLength;
    String accept = "text/json";
    boolean isComplete = false;

    /**
     * WARNING: the buffer must be processed synchronous or copied as the buffer is reused.
     *
     * @param buffer
     * @param len
     */
    public KontraktorHttpRequestImpl(ByteBuffer buffer, int len) {
        // FIXME: whole class should be pooled
        bytes = new byte[len];
        buffer.get(bytes);
        checkComplete();
    }

    public void append(ByteBuffer buf, int len) {
        System.out.println("PARTIAL READ");
        if ( !hadHeader()) {
            System.out.println("..complete header");
            byte[] newbytes = new byte[bytes.length + len];
            System.arraycopy(bytes, 0, newbytes, 0, bytes.length);
            buf.get(newbytes, bytes.length, len);
            bytes = newbytes;
            checkComplete();
            System.out.println("..complete:" + isComplete());
        } else {
            System.out.println("..complete body");
            byte[] tmp = new byte[len];
            buf.get(tmp);
            try {
                text.append(new String(tmp,"UTF-8"));
                isComplete = text.length() >= contentLength;
                System.out.println("..complete:"+isComplete()+" text:"+text.length()+" cont:"+contentLength);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                isComplete = true;
            }
        }
    }

    protected boolean hadHeader() {
        return contentStart != 0;
    }

    private void checkComplete() {
        for (int i = 4; i < bytes.length; i++) {
            if ( (bytes[i] == '\n' && bytes[i-1] == '\n') || (bytes[i] == 0xa && bytes[i-1] == 0xd && bytes[i-2] == 0xa && bytes[i-3] == 0xd) ) {
                contentStart = i+1;
                parseHeader();
                try {
                    text.append(new String(bytes,contentStart,bytes.length-contentStart,"UTF-8"));
                    isComplete = text.length() >= contentLength;
                    bytes = null; // free headerbytes
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    isComplete = true;
                }
                return;
            }
        }
    }

    public boolean isComplete() {
        return isComplete;
    }

    private void parseHeader() {
        int idx = 0;
        int b = 0;
        // FIXME: could be reused, we are singlethreaded
        StringBuffer keyString = new StringBuffer();
        StringBuffer valString = new StringBuffer();

        boolean key = true;
        boolean method = true;
        boolean path = false;
        boolean protocol = false;

        while( true ) {
            while( (b = bytes[idx++]) == 0 || b == 0xd );
            if ( key ) {
                if ( b=='\n') {
                    if ( method ) {
//                        System.out.println("Method/Path "+methodString+" / "+pathString);
                        keyString.setLength(0);
                        method = false;
                        path = false;
                    } else
                        break;
                } else if (b != ':' || path) {
                    if (method) {
                        if ( Character.isWhitespace(b) && !path ) {
                            path = true;
                        } else {
                            if ( path ) {
                                if ( Character.isWhitespace(b) )
                                    protocol = true; // ignore http version
                                else if ( ! protocol )
                                    pathString.append((char)b);
                            } else {
                                methodString.append((char)Character.toUpperCase(b));
                            }
                        }
                    } else
                        keyString.append((char) Character.toUpperCase(b));
                } else {
                    key = false;
                }
            } else {
                if ( b != '\n' ) {
                    valString.append((char)b);
                } else {
                    if (keyString.toString().equalsIgnoreCase("CONTENT-LENGTH")) {
                        contentLength = Integer.parseInt(valString.toString().trim());
                    } else if ( keyString.toString().equalsIgnoreCase("ACCEPT")) {
                        accept = valString.toString().toLowerCase().trim();
                    }
                    keyString.setLength(0);
                    valString.setLength(0);
                    key = true;
                }
            }
        }
    }

    public String getAccept() {
        return accept;
    }

    public StringBuilder getText() {
        return text;
    }

    public boolean isGET() {
        return methodString.charAt(0) == 'G';
    }

    public String getPath(int i) {
        if (splitPath == null ) {
            splitPath = pathString.toString().split("/");
        }
        if ( i+1 < splitPath.length ) {
            try {
                return java.net.URLDecoder.decode(splitPath[i+1], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return "url decoding failed";
            }
        }
        return "";
    }

    public boolean isPOST() {
        return methodString.charAt(0) == 'P';
    }
    public int getPathLen() {
        return splitPath.length-1;
    }
}
