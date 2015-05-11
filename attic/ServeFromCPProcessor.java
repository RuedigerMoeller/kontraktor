package org.nustaq.kontraktor.remoting.http_old;

import org.nustaq.kontraktor.Callback;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by ruedi on 20.10.14.
 */
public class ServeFromCPProcessor implements RequestProcessor {

    public static boolean ENABLED = false;

    @Override
    public boolean processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response) {
        if ( ! ENABLED )
            return false;
        if ( req.isGET() ) {
            String path = req.getFullPath();
            URL resource = Object.class.getResource(path);
            if ( resource != null ) {
                try {
                    String f = resource.getFile();
                    Object content = null;
                    File file = new File(f);
                    if ( file.exists() ) {
                        byte[] bytes = new byte[(int) file.length()];
                        try (FileInputStream fileInputStream = new FileInputStream(file)) {
                            fileInputStream.read(bytes);
                            response.settle(RequestResponse.MSG_200, null);
                            response.settle(new RequestResponse(bytes), FINISHED);
                        }
                        return true;
                    } else {
                        content = resource.getContent();
                    }
                    // Warning: this is not built to act as a high volume file server
                    // can be used to serve some text/images/js files on initial page load of an SPA
                    if ( content instanceof InputStream ) {
                        Scanner s = new java.util.Scanner((InputStream) content).useDelimiter("\\A");
                        response.settle(RequestResponse.MSG_200, null);
                        response.settle(new RequestResponse(s.hasNext() ? s.next() : ""), FINISHED);
                        ((InputStream) content).close();
                    } else {
                        response.settle(RequestResponse.MSG_200, null);
                        response.settle(new RequestResponse(content.toString()), FINISHED);
                    }
                } catch (Exception e) {
                    response.settle(RequestResponse.MSG_500, FINISHED);
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

//    public RequestResponse serveFile(ChannelHandlerContext ctx, FullHttpRequest req, NettyWSHttpServer.HttpResponseSender sender) {
//        String resource = req.getUri().toString();
//        if (resource==null||resource.trim().length()==0||resource.trim().equals("/"))
//            resource = "/index.html";
//        File target = new File(contentRoot, File.separator + resource);
//	    target = mapFileName(target);
//        if ( target.exists() && target.isFile() ) {
//            FileChannel inChannel = null;
//            RandomAccessFile aFile = null;
//            try {
//                aFile = new RandomAccessFile(target, "r");
//                inChannel = aFile.getChannel();
//                long fileSize = inChannel.size();
//                ByteBuffer buffer = ByteBuffer.allocate((int) fileSize); // FIXME: reuse ..
//                while( inChannel.read(buffer) > 0 ) {}
//                buffer.flip();
//                ByteBuf content = Unpooled.wrappedBuffer(buffer);
//                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
//                String accept = req.headers().get("Accept");
//                if ( accept.indexOf("text/html") >= 0 )
//                    res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
//                else {
//                    String[] split = accept.split(";");
//                    boolean exit = false;
//                    for (int i = 0; i < split.length; i++) {
//                        String s = split[i];
//                        String ss[] = s.split(",");
//                        for (int j = 0; j < ss.length; j++) {
//                            String s1 = ss[j];
//                            if ( s1.indexOf('*') < 0 ) {
//                                res.headers().set(CONTENT_TYPE, s1+"; charset=UTF-8");
//                                exit = true;
//                                break;
//                            }
//                        }
//                        if ( exit )
//                            break;
//                    }
//                }
//                setContentLength(res, content.readableBytes());
//                sender.sendHttpResponse(ctx, req, res);
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if ( inChannel != null )
//                        inChannel.close();
//                    if ( aFile != null )
//                            aFile.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
//            sender.sendHttpResponse(ctx, req, res);
//        } else {
//            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
//            sender.sendHttpResponse(ctx, req, res);
//        }
//    }
//
}
