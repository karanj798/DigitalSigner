package client;

import java.security.*;

/**
 * This class is used by Client app, for signing the document using cryptography.
 */
public class CryptoSign {
    private byte[] signedDocument;

    /**
     * This method signs the {@code byte[]} using client's Private Key.
     * @param bytes content of the file.
     * @param privateKey {@code PrivateKey} object containing user's private key.
     */
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

    /**
     * This method retrieves the signed document in form of bytes.
     * @return bytes containing the signed document.
     */
    public byte[] getSignedDocument() {
        return this.signedDocument;
    }
}
