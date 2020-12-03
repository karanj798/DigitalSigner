package client;

import common.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Profile {

    String name;
    FileUtils fileUtils;
    public Profile(String name) {
        this.name = name;
        this.fileUtils = new FileUtils();
        mkdir();
    }

    private void mkdir() {
        if (!Files.exists(Paths.get(fileUtils.getResourcesPath() + this.name))) {
            try {
                Files.createDirectories(Path.of(fileUtils.getResourcesPath() + this.name));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean keysExists() {
        String resourcePath = fileUtils.getResourcesPath() + this.name + "/";
        return new File(resourcePath + this.name + ".key").exists() &&
                new File(resourcePath + this.name + ".pub").exists();
    }

    public void generatePrivatePublicKeysPair() {
        if (!keysExists()) {
            KeyPairGenerator keyPairGenerator = null;
            KeyPair keyPair = null;
            try {
                keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                keyPair = keyPairGenerator.generateKeyPair();

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            saveKeys(keyPair);
        }
    }

    private void saveKeys(KeyPair keyPair) {
        String resourcePath = fileUtils.getResourcesPath() + this.name + "/";

        try {
            System.out.println("Writing keys to file...");

            Files.write(Paths.get(resourcePath + this.name + ".key"), keyPair.getPrivate().getEncoded());
            Files.write(Paths.get(resourcePath + this.name + ".pub"), keyPair.getPublic().getEncoded());

            System.out.println("Successfully wrote keys to file...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;
        if(this.keysExists()) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] privKey = Files.readAllBytes(Paths.get(fileUtils.getResourcesPath() + this.name + "/"+ this.name + ".key"));
                PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privKey);
                privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                e.printStackTrace();
            }
        }
        return privateKey;
    }

    public PublicKey getPublicKey() {
        PublicKey publicKey = null;
        if(this.keysExists()) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] pubKey = Files.readAllBytes(Paths.get(fileUtils.getResourcesPath() + this.name + "/"+ this.name + ".pub"));
                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKey);
                publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                e.printStackTrace();
            }
        }
        return publicKey;
    }

    public String getPublicKeyAsString() {
        return "-----BEGIN RSA PUBLIC KEY-----\n" +
                (Base64.getEncoder().encodeToString(getPublicKey().getEncoded())) +
                "\n-----END RSA PUBLIC KEY-----\n";
    }
}
