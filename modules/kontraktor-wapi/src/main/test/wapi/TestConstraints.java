package wapi;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.service.DenialReason;
import org.nustaq.kontraktor.remoting.service.ServiceConstraints;
import org.nustaq.kontraktor.util.RateMeasure;

import javax.xml.bind.DatatypeConverter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 06.03.17.
 */
public class TestConstraints implements ServiceConstraints {

    ConcurrentHashMap<String,RateMeasure> token2Rate = new ConcurrentHashMap<>();

    public DenialReason registerToken(String token, String uname) {
        if ( token == null || uname == null )
            return DenialReason.INVALID_TOKEN;
        if ( uname.equals("test") ) {
            String secret = "MA6cia2FyhHKy4pD8hY+8Q==";
            boolean succ = parseJWT(token,secret);
            if ( ! succ )
                return DenialReason.INVALID_TOKEN;
        } else
            return DenialReason.INVALID_TOKEN;
        RateMeasure measure = new RateMeasure("CALLS");
        token2Rate.put(token,measure);
        return null;
    }

    public boolean parseJWT(String jwt,String secret) {
        try {
            //This line will throw an exception if it is not a signed JWS (as expected)
            Claims claims = Jwts.parser()
               .setSigningKey(DatatypeConverter.parseBase64Binary(secret))
               .parseClaimsJws(jwt).getBody();
            System.out.println("ID: " + claims.getId());
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issuer: " + claims.getIssuer());
            System.out.println("Expiration: " + claims.getExpiration());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public DenialReason isCallValid(Object token, RemoteCallEntry rce) {
        RateMeasure rateMeasure = token2Rate.get(token);
        if ( rateMeasure == null ) {
            return DenialReason.INVALID_TOKEN;
        }
        long count = rateMeasure.count();
        System.out.println("count:"+ count +" "+rateMeasure.getOpenCount());
        if ( count > 10 || rateMeasure.getOpenCount() > 10 * 2 ) {
            return DenialReason.RATE_LIMIT_EXCEEDED;
        }
        return null;
    }

}
