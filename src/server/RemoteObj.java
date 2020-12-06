package server;

import com.google.gson.Gson;
import common.CommonService;
import common.FileUtils;
import common.model.Request;
import common.model.Response;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.*;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is class handles incoming RMI requests from Client.
 */
public class RemoteObj implements CommonService {
    PublisherReplica pReplica;
    SubscriberReplica sReplica;
    FileUtils fileUtils;
    String nodeType;

    /**
     * Initializes pub/sub for file replication.
     * @param pubAddr Port for publishing
     * @param subAddr Port for subscribing
     * @param nodeType Name of the node (Master/BackUp)
     */
    public RemoteObj(String pubAddr, String subAddr, String nodeType) {
        this.nodeType = nodeType;
        this.fileUtils = new FileUtils();
        if (pubAddr != null) {
            this.pReplica = new PublisherReplica(pubAddr, nodeType);
            pReplica.start();
        }

        if (subAddr != null) {
            this.sReplica = new SubscriberReplica(subAddr, nodeType);
            sReplica.start();
        }
    }

    /**
     * This method inserts the Public Key of the client in the Redis Instance.
     * @param userName Name of the user
     * @param publicKey Public key in form of String
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public void insertKey(String userName, String publicKey) throws RemoteException {
        RedisRequester redisRequester = new RedisRequester();
        String reply = redisRequester.get(6379, userName);

        if (reply.equals("null")) redisRequester.set(6379, userName, publicKey);

        redisRequester.close();
    }

    /**
     * This method handles the request of the client.
     * @param request Object that contains name of the file and a {@code List<String>} object containing people who need to sign.
     * @param fileContent Content of the file in form of {@code byte[]}
     * @return Universally unique identifier (UUID)
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public String request(Request request, byte[] fileContent) throws RemoteException {
        uploadFile(request.getFileName(), fileContent);
        return cacheRequest(request);
    }

    /**
     * This method looks up a list of files that client needs to sign.
     * @param userName Name of the connected user.
     * @return {@code List<String>} containing names of the file that need to be signed.
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public List<String> getFilesForSigning(String userName) throws RemoteException {
        RedisRequester redisRequester = new RedisRequester();
        List<String> keySet = Arrays.asList(redisRequester.keys(6381).replace("[", "").replace("]", "").split(", "));

        List<String> documentList = new ArrayList<>();

        for (String k : keySet) {
            Gson gson = new Gson();
            String response = redisRequester.get(6381, k);
            Request request = gson.fromJson(response, Request.class);
            if (request.getSignerList().contains(userName)) {
                documentList.add(request.getFileName());
            }
        }

        redisRequester.close();

        return documentList;
    }

    /**
     * Transfers file in form of bytes over RMI connection to the client.
     * @param fileName Name of the file
     * @return {@code byte[]} containing the content of the file.
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public byte[] downloadFile(String fileName) throws RemoteException {
        try {
            String directoryPath = fileUtils.getResourcesPath() + this.nodeType;

            File file = new File(directoryPath + "/" + fileName);

            byte[] buffer = new byte[(int) file.length()];
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            input.read(buffer, 0, buffer.length);
            input.close();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Writes the bytes into the file specified.
     * @param fileName Name of the file.
     * @param fileContent {@code byte[]} containing content of file.
     */
    public void uploadFile(String fileName, byte[] fileContent) {
        try {
            String directoryPath = fileUtils.getResourcesPath() + this.nodeType;
            File directory = new File(directoryPath);
            if (!directory.exists()) directory.mkdir(); //Create a directory if it does not exist

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

    /**
     * Opens a PDFHandler object and appends the timestamps and clients who signed the document.
     * @param fileName Name of the file.
     * @param signatures {@code List<String>} containing clients who signed the document.
     * @param timestamps {@code List<Long>} containing timestamps of when clients signed the document.
     */
    public void signDocument(String fileName, List<String> signatures, List<Long> timestamps) {
        String directoryPath = fileUtils.getResourcesPath() + this.nodeType;
        PDFHandler pdfHandler = new PDFHandler(new File(directoryPath + "/" + fileName));
        pdfHandler.loadDocument();

        pdfHandler.addSignature(signatures, timestamps);
        File signedFile = pdfHandler.savePDFFile();

        // Replicate on the backup server
        try {
            pReplica.publishNewFile(signedFile.getName(), Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores the clients request into the Redis Request Instance.
     * @param request {@code Request} object containing the name of the file and {@code List<String>} containing names
     *                of clients who need sign the document.
     * @return Universally unique identifier (UUID)
     */
    public String cacheRequest(Request request) {
        String uuid = UUID.nameUUIDFromBytes(request.getFileName().getBytes()).toString();

        RedisRequester redisRequester = new RedisRequester();
        String reply = redisRequester.get(6381, uuid);

        if (reply.equals("null")) redisRequester.set(6381,  uuid, request.toString());

        redisRequester.close();

        return uuid;
    }

    /**
     * This method utilizes cryptography to verify client's signature.
     * @param userName Name of the client.
     * @param originalDocument {@code byte[]} content of original document
     * @param signedDocument {@code byte[]} content of the signed document
     * @param fileName Name of the file
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public void verifySignature(String userName, byte[] originalDocument, byte[] signedDocument, String fileName) throws RemoteException {

        RedisRequester redisRequester = new RedisRequester();
        String publicKeyAsString = redisRequester.get(6379, userName);

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

        redisRequester.close();
    }

    /**
     * Stores the record which contains information about the document that is signed.
     * @param fileName Name of the file.
     * @param userName Name of the client
     * @param timestamp The timestamp of when the document signed.
     */
    public void cacheResponse(String fileName, String userName, long timestamp) {
        String uuid = UUID.nameUUIDFromBytes(fileName.getBytes()).toString();

        RedisRequester redisRequester = new RedisRequester();
        String uuidEntry = redisRequester.get(6383, uuid);

        if (uuidEntry.equals("null")) {
            System.out.println("None found...");
            redisRequester.set(6383, uuid, new Response(fileName, Collections.singletonList(userName), Collections.singletonList(timestamp)).toString());
        } else {
            System.out.println("Found ... ");
            Gson gson = new Gson();
            Response response = gson.fromJson(uuidEntry, Response.class);
            response.getSignerList().add(userName);
            response.getTimeStampList().add(timestamp);
            redisRequester.set(6383, uuid, response.toString());
        }

        redisRequester.close();
    }

    /**
     * This method checks if the document was signed by all the people.
     * @param uuid Universally unique identifier (UUID), used like a key to query from Redis instance
     * @return true/false depending on if the document is signed by all the peers.
     * @throws RemoteException if errors take place during RMI.
     */
    @Override
    public boolean isFinished(String uuid) throws RemoteException {
        // Read from Response
        RedisRequester redisRequester = new RedisRequester();
        String responseRow = redisRequester.get(6383, uuid);

        // Base case
        if (responseRow.equals("null")) {
            redisRequester.close();
            return false;
        }

        String requestRow = redisRequester.get(6381, uuid);

        Gson gson = new Gson();
        Response response = gson.fromJson(responseRow, Response.class);
        Request request = gson.fromJson(requestRow, Request.class);

        List<String> copyOfResponseSignerList = response.getSignerList().stream().collect(Collectors.toList());

        Collections.sort(copyOfResponseSignerList);
        Collections.sort(request.getSignerList());


        if (request.getSignerList().equals(copyOfResponseSignerList)) {
            System.out.println("Done...");
            signDocument(response.getFileName(), response.getSignerList(), response.getTimeStampList());
            redisRequester.del(6381, uuid);
            redisRequester.del(6383, uuid);
            redisRequester.close();
            return true;
        }
        redisRequester.close();
        return false;
    }
}