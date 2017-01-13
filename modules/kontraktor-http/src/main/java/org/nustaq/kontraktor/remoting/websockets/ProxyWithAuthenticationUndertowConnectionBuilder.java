package org.nustaq.kontraktor.remoting.websockets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xnio.Cancellable;
import org.xnio.ChannelListener;
import org.xnio.FutureResult;
import org.xnio.IoFuture;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.http.HttpUpgrade;

import io.undertow.UndertowMessages;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.client.WebSocketClientHandshake;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.core.WebSocketChannel;

public class ProxyWithAuthenticationUndertowConnectionBuilder extends ConnectionBuilder {

    /**
     * This class is only needed to connect to a proxy which requires user authentication.
     * Very ugly! This is just a copy of "WebSocketClient.ConnectionBuilder" plus some extra code.
     * If there is another way to set the proxy-authorization for websocket connection delete this class!
     */
    private String proxyUser;
    private String proxyPwd;

    public ProxyWithAuthenticationUndertowConnectionBuilder(XnioWorker worker, Pool<ByteBuffer> bufferPool, URI uri, String proxyUser, String proxyPwd) {
        super(worker, bufferPool, uri);
        this.proxyUser = proxyUser;
        this.proxyPwd = proxyPwd;
    }

    @Override
    public IoFuture<WebSocketChannel> connect() {
        final URI uri = getUri();
        final FutureResult<WebSocketChannel> ioFuture = new FutureResult<>();
        final String scheme = uri.getScheme().equals("wss") ? "https" : "http";
        final URI newUri;
        try {
            newUri = new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort() == -1 ? (uri.getScheme().equals("wss") ? 443 : 80) : uri.getPort(), uri.getPath().isEmpty() ? "/" : uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final WebSocketClientHandshake handshake = WebSocketClientHandshake.create(getVersion(), newUri, getClientNegotiation(), getClientExtensions());
        final Map<String, String> originalHeaders = handshake.createHeaders();
        originalHeaders.put(Headers.ORIGIN_STRING, scheme + "://" + uri.getHost());
        final Map<String, List<String>> headers = new HashMap<>();
        for(Map.Entry<String, String> entry : originalHeaders.entrySet()) {
            List<String> list = new ArrayList<>();
            list.add(entry.getValue());
            headers.put(entry.getKey(), list);
        }
        if (getClientNegotiation() != null) {
            getClientNegotiation().beforeRequest(headers);
        }
        InetSocketAddress toBind = getBindAddress();
        String sysBind = System.getProperty(WebSocketClient.BIND_PROPERTY);
        if(toBind == null && sysBind != null) {
            toBind = new InetSocketAddress(sysBind, 0);
        }
        final InetSocketAddress finalToBind = toBind;
        URI proxyUri = getProxyUri();
        if(proxyUri != null) {
           UndertowClient.getInstance().connect(new ClientCallback<ClientConnection>() {
                @Override
                public void completed(final ClientConnection connection) {
                    int port = uri.getPort() > 0 ? uri.getPort() : uri.getScheme().equals("https") || uri.getScheme().equals("wss") ? 443 : 80;
                    ClientRequest cr = new ClientRequest()
                            .setMethod(Methods.CONNECT)
                            .setPath(uri.getHost() + ":" + port)
                            .setProtocol(Protocols.HTTP_1_1);
                    
                    // start of added code 
                    String basicAuthPlainUserPass = proxyUser + ":" + proxyPwd;
                    byte[] bytes = basicAuthPlainUserPass.getBytes(StandardCharsets.UTF_8);
                    String basicAuthEncodedUserPass = new String(java.util.Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
                    cr.getRequestHeaders().add(Headers.PROXY_AUTHORIZATION, "Basic " + basicAuthEncodedUserPass);
                    // end of added code 
                    
                    connection.sendRequest(cr, new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            result.setResponseListener(new ClientCallback<ClientExchange>() {
                                @Override
                                public void completed(ClientExchange response) {
                                    if (response.getResponse().getResponseCode() == 200) {
                                        try {
                                            StreamConnection targetConnection = connection.performUpgrade();
                                            if(uri.getScheme().equals("wss") || uri.getScheme().equals("https")) {
                                                handleConnectionWithExistingConnection(((UndertowXnioSsl)getSsl()).wrapExistingConnection(targetConnection, getOptionMap()));
                                            } else {
                                                handleConnectionWithExistingConnection(targetConnection);
                                            }
                                        } catch (IOException e) {
                                            ioFuture.setException(e);
                                        } catch (Exception e) {
                                            ioFuture.setException(new IOException(e));
                                        }
                                    } else {
                                        ioFuture.setException(UndertowMessages.MESSAGES.proxyConnectionFailed(response.getResponse().getResponseCode()));
                                    }
                                }

                                private void handleConnectionWithExistingConnection(StreamConnection targetConnection) {
                                    final IoFuture<?> result;

                                    result = HttpUpgrade.performUpgrade(targetConnection, newUri, headers, new MyWebsocketConnectionListener(handshake, newUri, ioFuture), handshake.handshakeChecker(newUri, headers));

                                    result.addNotifier(new IoFuture.Notifier<Object, Object>() {
                                        @Override
                                        public void notify(IoFuture<?> res, Object attachment) {
                                            if (res.getStatus() == IoFuture.Status.FAILED) {
                                                ioFuture.setException(res.getException());
                                            }
                                        }
                                    }, null);
                                    ioFuture.addCancelHandler(new Cancellable() {
                                        @Override
                                        public Cancellable cancel() {
                                            result.cancel();
                                            return null;
                                        }
                                    });
                                }

                                @Override
                                public void failed(IOException e) {
                                    ioFuture.setException(e);
                                }
                            });
                        }
                        @Override
                        public void failed(IOException e) {
                            ioFuture.setException(e);
                        }
                    });
                }
                @Override
                public void failed(IOException e) {
                    ioFuture.setException(e);
                }
            }, getBindAddress(), proxyUri, getWorker(), getProxySsl(),  getBufferPool(), getOptionMap());

        } else {
            final IoFuture<?> result;
            if (getSsl() != null) {
                result = HttpUpgrade.performUpgrade(getWorker(), getSsl(), toBind, newUri, headers, new MyWebsocketConnectionListener(handshake, newUri, ioFuture), null, getOptionMap(), handshake.handshakeChecker(newUri, headers));
            } else {
                result = HttpUpgrade.performUpgrade(getWorker(), toBind, newUri, headers, new MyWebsocketConnectionListener(handshake, newUri, ioFuture), null, getOptionMap(), handshake.handshakeChecker(newUri, headers));
            }
            result.addNotifier(new IoFuture.Notifier<Object, Object>() {
                @Override
                public void notify(IoFuture<?> res, Object attachment) {
                    if (res.getStatus() == IoFuture.Status.FAILED) {
                        ioFuture.setException(res.getException());
                    }
                }
            }, null);
            ioFuture.addCancelHandler(new Cancellable() {
                @Override
                public Cancellable cancel() {
                    result.cancel();
                    return null;
                }
            });
        }
        return ioFuture.getIoFuture();
    }
    
    class MyWebsocketConnectionListener implements ChannelListener<StreamConnection> {
        private final WebSocketClientHandshake handshake;
        private final URI newUri;
        private final FutureResult<WebSocketChannel> ioFuture;

        public MyWebsocketConnectionListener(WebSocketClientHandshake handshake, URI newUri, FutureResult<WebSocketChannel> ioFuture) {
            this.handshake = handshake;
            this.newUri = newUri;
            this.ioFuture = ioFuture;
        }

        @Override
        public void handleEvent(StreamConnection channel) {
            WebSocketChannel result = handshake.createChannel(channel, newUri.toString(), getBufferPool());
            ioFuture.setResult(result);
        }
    }
}
