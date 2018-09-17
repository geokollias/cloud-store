package com.logicblox.s3lib;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGenCommand {

    private KeyPairGenerator keypairGen;
    private KeyPair keypair;
    private PublicKey publickey;
    private PrivateKey privateKey;

    public KeyGenCommand(String algo, int nbits)
            throws NoSuchAlgorithmException {
        keypairGen = KeyPairGenerator.getInstance(algo);
        keypairGen.initialize(nbits);
        keypair = keypairGen.generateKeyPair();
        publickey = keypair.getPublic();
        privateKey = keypair.getPrivate();
    }

    public void savePemKeypair(File pemf) throws IOException {
        String pem = getPemPublicKey() + "\n" + getPemPrivateKey();

        FileUtils.writeStringToFile(pemf, pem, "UTF-8");
    }

    public String getPemPrivateKey() {
        // pkcs8_der is PKCS#8-encoded binary (DER) private key
        byte[] pkcs8_der = privateKey.getEncoded();

        // DER to PEM conversion
        String pem_encoded = pemEncode(pkcs8_der,
                "-----BEGIN PRIVATE KEY-----\n",
                "-----END PRIVATE KEY-----\n");

        return pem_encoded;
    }

    public String getPemPublicKey() {
        // x509_der is X.509-encoded binary (DER) public key
        byte[] x509_der = publickey.getEncoded();

        // DER to PEM conversion
        String pem_encoded = pemEncode(x509_der,
                "-----BEGIN PUBLIC KEY-----\n",
                "-----END PUBLIC KEY-----\n");

        return pem_encoded;
    }

    private String pemEncode(byte[] keyBytes,
                             String startArmour,
                             String endArmour) {
        int lineLength = Base64.PEM_CHUNK_SIZE;
        byte[] lineSeparator = {'\n'};
        Base64 b64 = new Base64(lineLength, lineSeparator);
        String encoded = b64.encodeToString(keyBytes);

        return startArmour + encoded + endArmour;
    }

}