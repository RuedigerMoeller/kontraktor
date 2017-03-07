package org.nustaq.kontraktor.wapi;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by ruedi on 07.03.17.
 */
public interface UserConstraintRegistry {

    default String createSecret(String ukey) {
        // create new key
        SecretKey secretKey = null;
        try {
            secretKey = KeyGenerator.getInstance("AES").generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String secr = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        return secr;
    }

    void storeSecret(String ukey, String secr);
    String getSecret(String ukey);

    default Claims verifyToken(String jwt, String ukey) {
        try {
            String secret = getSecret(ukey);
            if ( secret == null )
                return null;
            //This line will throw an exception if it is not a signed JWS (as expected)
            Claims claims = Jwts.parser()
               .setSigningKey(DatatypeConverter.parseBase64Binary(secret))
               .parseClaimsJws(jwt).getBody();
            if ( claims.getExpiration() != null && claims.getExpiration().getTime() < System.currentTimeMillis() )
                return null;
            return claims;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
