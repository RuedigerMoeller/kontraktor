package org.nustaq.kontraktor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

public class JWTUtil {

    public static String createSecret() {
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

    public static String encrypt(String subject,String secret) {
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public static String decrypt(String jwt, String secret) {
        if ( secret == null )
            return null;
        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = getClaims(jwt, secret);
        return claims.getSubject();
    }

    public static Claims getClaims(String jwt, String secret) {
        return Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(secret))
                .parseClaimsJws(jwt).getBody();
    }

    public static String readSecret(String fi) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(fi));
        return new String(bytes,"UTF-8");
    }

    public static void writeSecret(String secret, String fi) throws IOException {
        byte[] bytes = secret.getBytes("UTF-8");
        Files.write(Paths.get(fi),bytes);
    }


}
