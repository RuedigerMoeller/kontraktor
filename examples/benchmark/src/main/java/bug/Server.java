package bug;

import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.jboss.logging.Logger;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.servlet.ServletException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.websockets.jsr.WebSocketDeploymentInfo.ATTRIBUTE_NAME;

public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class);

    @ServerEndpoint("/")
    public static class SocketProxy {

        @OnOpen
        public void onOpen() {
            LOGGER.info("onOpen");
        }

        @OnClose
        public void onClose() {
            LOGGER.info("onClose");
        }

        @OnMessage
        public void onMessage(String message) {
            LOGGER.info("onMessage:" + message);
        }

    }

    public static void main(String[] args) throws ServletException, IOException {
        final Xnio xnio = Xnio.getInstance("nio", Undertow.class.getClassLoader());
        final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());
        final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
            .addEndpoint(SocketProxy.class)
            .setWorker(xnioWorker);
        final DeploymentManager deployment = defaultContainer()
            .addDeployment(deployment()
                .setClassLoader(Server.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("embedded-websockets")
                .addServletContextAttribute(ATTRIBUTE_NAME, webSockets));

        deployment.deploy();
        Undertow.builder().
            addListener(8383, "localhost")
            .setHandler(deployment.start())
            .build()
            .start();
    }

}