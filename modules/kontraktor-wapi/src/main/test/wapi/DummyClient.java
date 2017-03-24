package wapi;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.nustaq.kontraktor.barebone.Callback;
import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.kontraktor.barebone.RemoteActorConnection;
import org.nustaq.kontraktor.wapi.UserConstraintRegistry;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 10.03.17.
 */
public class DummyClient {
    RemoteActorConnection actorConnection;
    RemoteActor facade;
    String jwt;
    String id;

    public void init(String jwt, String id) {
        this.jwt = jwt;
        this.id = id;
    }

    public boolean isConnected() {
        return actorConnection != null && facade != null;
    }

    public void connect() {
        System.out.println("try connecting ..");
        actorConnection = new RemoteActorConnection( s -> {
            disconnect();
        });
        try {
            actorConnection.jwt(jwt).id("moru");
            facade = actorConnection.connect("http://localhost:7777/dummyservice", true).await();
            run();
        } catch (Throwable e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private void disconnect() {
        System.out.println("connection closed");
        actorConnection = null;
        facade = null;
        new Thread(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connect();
        }).start();
    }

    public void run() {
        facade.ask("service", "hello" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("foreign", new ForeignClass(1,2,3) ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("service", "holla" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.tell("subscribe", new ForeignClass(1,2,3),
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
    }

    public static String createJWT(String secret, String id, String issuer, String subject, long ttlMillis) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secret);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(id)
                                    .setIssuedAt(now)
                                    .setSubject(subject)
                                    .setIssuer(issuer)
                                    .signWith(signatureAlgorithm, signingKey);

        //if it has been specified, let's add the expiration
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
                Date exp = new Date(expMillis);
                builder.setExpiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        DummyClient dummyClient = new DummyClient();
        String secret = UserConstraintRegistry.readSecret("/home/ruedi/projects/kontraktor/modules/kontraktor-wapi/src/main/script/secret.txt");
        String jwt = createJWT(secret,"moru","wapi", "dummyservice", TimeUnit.DAYS.toMillis(10));
        dummyClient.init(jwt,"moru");
        dummyClient.connect();
    }
}
