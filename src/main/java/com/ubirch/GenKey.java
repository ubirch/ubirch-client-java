package com.ubirch;

import com.ubirch.crypto.GeneratorKeyFactory;
import com.ubirch.crypto.PrivKey;
import com.ubirch.crypto.utils.Curve;
import org.apache.commons.codec.binary.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Arrays;

public class GenKey {
    public static void main(String[] args) {
        try {

            PrivKey pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519);
            byte[] pkBytes = Arrays.copyOfRange(pk.getRawPrivateKey().getBytes(), 0, 32);
            String pkB64 = Base64.encodeBase64String(pkBytes);

            byte[] clientKeyBytes = java.util.Base64.getDecoder().decode(pkB64);

            System.out.println("privateKey: " + pkB64);
        } catch (NoSuchAlgorithmException  e) {
            e.printStackTrace();
        }
    }
}
