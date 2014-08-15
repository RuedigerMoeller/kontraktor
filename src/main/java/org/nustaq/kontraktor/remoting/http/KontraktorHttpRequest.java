package org.nustaq.kontraktor.remoting.http;

import java.nio.ByteBuffer;

/**
 * Created by ruedi on 13.08.2014.
 */
public class KontraktorHttpRequest // avoid clash with servlet api
{
    final byte[] bytes;
    String text;
    StringBuffer methodString = new StringBuffer();
    StringBuffer pathString = new StringBuffer();
    String splitPath[];

    /**
     * WARNING: the buffer must be processed synchronous or copied as the buffer is reused.
     *
     * @param buffer
     * @param len
     */
    public KontraktorHttpRequest(ByteBuffer buffer, int len) {
        // FIXME: whole class should be pooled
        bytes = new byte[len];
        buffer.get(bytes);
        parseHeader();
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
                        System.out.println("Method/Path "+methodString+" / "+pathString);
                        keyString.setLength(0);
                        method = false;
                    } else
                        break;
                } else if (b != ':') {
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
                    System.out.println("=> "+keyString+":"+valString);
                    keyString.setLength(0);
                    valString.setLength(0);
                    key = true;
                }
            }
        }
        text = new String(bytes,0, idx,bytes.length-idx); // fixme utf-8
    }

    public String getText() {
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
            return splitPath[i+1];
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
