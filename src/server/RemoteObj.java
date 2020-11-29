package server;

import common.CommonService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisStringCommands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.rmi.RemoteException;

public class RemoteObj implements CommonService {
    PublisherReplica pReplica;
    SubscriberReplica sReplica;

    public RemoteObj (String pubAddr, String subAddr){
        if(pubAddr != null){
            this.pReplica = new PublisherReplica(pubAddr);
            pReplica.start();
        }

        if(subAddr != null){
            this.sReplica = new SubscriberReplica(subAddr);
            sReplica.start();
        }
    }

    @Override
    public String getMessage() throws RemoteException {
        return "Hello Client!";
    }

    @Override
    public void uploadFile(String fileName, byte[] fileContent) throws RemoteException {
        try {
            System.out.println("File : " + fileName + " is being recieved...");

            File file = new File(fileName);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(fileName));
            output.write(fileContent, 0, fileContent.length);
            output.flush();
            output.close();

            System.out.println("Successfully downloaded the file " + fileName + " from the Client");

            pReplica.publishNewFile(fileName, fileContent);
        } catch (Exception e) {
            System.out.println("FileImpl: " + e.getMessage());
            e.printStackTrace();
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
}