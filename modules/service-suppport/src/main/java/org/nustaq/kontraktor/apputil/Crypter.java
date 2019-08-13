package org.nustaq.kontraktor.apputil;

public interface Crypter {

    byte[] encrypt(byte b[]);
    byte[] decrypt(byte b[]);

}
