package server;

import common.CommonService;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;

import java.rmi.RemoteException;

public class RemoteObj implements CommonService {

    @Override
    public String getMessage() throws RemoteException {
        return "Hello Client!";
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