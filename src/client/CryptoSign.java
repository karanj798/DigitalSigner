package client;

import java.security.*;

public class CryptoSign {
    private byte[] signedDocument;

    public void signDocument(byte[] bytes, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(bytes, 0, bytes.length);
            signedDocument = sign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    public byte[] getSignedDocument() {
        return this.signedDocument;
    }
}
