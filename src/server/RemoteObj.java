package server;

import com.google.gson.Gson;
import common.CommonService;
import common.FileUtils;
import common.model.Request;
import common.model.Response;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisStringCommands;

import java.io.*;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RemoteObj implements CommonService {
    PublisherReplica pReplica;
    SubscriberReplica sReplica;
    FileUtils fileUtils;
    String nodeType;

    public RemoteObj (String pubAddr, String subAddr, String nodeType){
        this.nodeType = nodeType;
        this.fileUtils = new FileUtils();
        if(pubAddr != null){
            this.pReplica = new PublisherReplica(pubAddr, nodeType);
            pReplica.start();
        }

        if(subAddr != null){
            this.sReplica = new SubscriberReplica(subAddr, nodeType);
            sReplica.start();
        }
    }

    @Override
    public void uploadFile(String fileName, byte[] fileContent) throws RemoteException {
        try {
            String directoryPath = fileUtils.getResourcesPath() + this.nodeType;
            File directory = new File(directoryPath);
            if (! directory.exists()) directory.mkdir(); //Create a directory if it does not exist

            File file = new File(directoryPath + "/" + fileName);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile()));
            output.write(fileContent, 0, fileContent.length);
            output.flush();
            output.close();

            System.out.println("Successfully received the file " + fileName + " from the Client to sign...");

            pReplica.publishNewFile(fileName, fileContent);
        } catch (Exception e) {
            System.out.println("RemoteObj: " + e.getMessage());
        }
    }

    @Override
    public void insertKey(String userName, String publicKey) throws RemoteException {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6379).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisStringCommands<String, String> sync = connection.sync();
        RedisAsyncCommands<String, String> async = connection.async();

        if (sync.get(userName) == null) {
            async.set(userName, publicKey);
        }

        connection.close();
        redisClient.shutdown();
    }

    public void signDocument(String fileName, List<String> signatures, List<Long> timestamps) throws RemoteException{
        String directoryPath = fileUtils.getResourcesPath() + this.nodeType;
        PDFHandler pdfHandler = new PDFHandler(new File(directoryPath + "/" + fileName));
        pdfHandler.loadDocument();

        // replace after ->
//        List<String> signatures = new ArrayList<>();
//        signatures.add("Karan");
//        signatures.add("John");

//        List<Timestamp> timestamps = new ArrayList<>();

//        for (int i = 1; i <= 2; i++) {
//            timestamps.add(new Timestamp(System.currentTimeMillis()));
//        }
        // <- replace after

        pdfHandler.addSignature(signatures, timestamps);
        File signedFile = pdfHandler.savePDFFile();

        // Replicate on the backup server
        try {
            pReplica.publishNewFile(signedFile.getName(), Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String request(Request request, byte[] fileContent) throws RemoteException {
        uploadFile(request.getFileName(), fileContent);
        return cacheRequest(request);
    }

    public String cacheRequest(Request request) {
        String uuid = UUID.nameUUIDFromBytes(request.getFileName().getBytes()).toString();

        // Write to Request Redis Instance
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6381).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisStringCommands<String, String> sync = connection.sync();

        if (sync.get(uuid) == null) {
            sync.set(uuid, request.toString());
        }

        connection.close();
        redisClient.shutdown();
        return uuid;
    }

    @Override
    public List<String> getFilesForSigning(String userName) throws RemoteException {

        // Read from Request Redis Instance
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6381).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisKeyCommands<String, String> syncKeyCmd = connection.sync();
        RedisStringCommands<String, String> syncStringCmd = connection.sync();

        List<String> keySet = syncKeyCmd.keys("*");

        List<String> documentList = new ArrayList<>();

        for (String k : keySet) {
            Gson gson = new Gson();
            Request request = gson.fromJson(syncStringCmd.get(k), Request.class);
            if (request.getSignerList().contains(userName)) {
                documentList.add(request.getFileName());
            }
        }

        connection.close();
        redisClient.shutdown();

        return documentList;
    }

    @Override
    public byte[] downloadFile(String fileName)  throws RemoteException {
        try {
            String directoryPath = fileUtils.getResourcesPath() + this.nodeType;

            File file = new File(directoryPath + "/" + fileName);

            byte[] buffer = new byte[(int)file.length()];
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            input.read(buffer,0,buffer.length);
            input.close();
            return buffer;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void verifySignature(String userName, byte[] originalDocument, byte[] signedDocument, String fileName) throws RemoteException {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6379).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisStringCommands<String, String> sync = connection.sync();

        String publicKeyAsString = sync.get(userName);

        connection.close();
        redisClient.shutdown();

        String publicKeyPEM = publicKeyAsString
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replace("-----END RSA PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(publicKey);
            sign.update(originalDocument, 0, originalDocument.length);
            if (sign.verify(signedDocument)) {
                System.out.println("Document is signed correctly...");
                cacheResponse(fileName, userName, System.currentTimeMillis());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    public void cacheResponse(String fileName, String userName, long timestamp) {
        String uuid = UUID.nameUUIDFromBytes(fileName.getBytes()).toString();

        // Read from Request Redis Instance
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6383).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisStringCommands<String, String> sync = connection.sync();

        if (sync.get(uuid) == null) {
            System.out.println("None found...");
            sync.set(uuid, new Response(fileName, Collections.singletonList(userName), Collections.singletonList(timestamp)).toString());
        } else {
            System.out.println("Found ... ");
            String entry = sync.get(uuid);
//            System.out.println(entry);
            Gson gson = new Gson();
            Response response = gson.fromJson(entry, Response.class);
            response.getSignerList().add(userName);
            response.getTimeStampList().add(timestamp);
            sync.set(uuid, response.toString());
        }
        connection.close();
        redisClient.shutdown();
    }

    @Override
    public boolean isFinished(String uuid) throws RemoteException {
        // Read from Response
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1", 6383).build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisStringCommands<String, String> sync = connection.sync();

        String responseRow = sync.get(uuid);
        connection.close();
        redisClient.shutdown();

        // Base case
        if (responseRow == null) {
            return false;
        }

        redisURI = RedisURI.Builder.redis("127.0.0.1", 6381).build();
        redisClient = RedisClient.create(redisURI);
        connection = redisClient.connect();
        sync = connection.sync();

        String requestRow = sync.get(uuid);
        Gson gson = new Gson();
        Response response = gson.fromJson(responseRow, Response.class);
        Request request = gson.fromJson(requestRow, Request.class);

        List<String> copyOfResponseSignerList = response.getSignerList().stream().collect(Collectors.toList());

        Collections.sort(copyOfResponseSignerList);
        Collections.sort(request.getSignerList());

        connection.close();
        redisClient.shutdown();

        if (request.getSignerList().equals(copyOfResponseSignerList)) {
            System.out.println("Done...");
            signDocument(response.getFileName(), response.getSignerList(), response.getTimeStampList());
            return true;
        }
        return false;
    }
}