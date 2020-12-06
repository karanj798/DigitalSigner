package server;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class RedisWorker extends Thread {
    private RedisClient redisClientKeys, redisClientRequest, redisClientResponse;
    private StatefulRedisConnection<String, String> connection;

    @Override
    public void run() {
        try (ZContext context = new ZContext()) {
            Socket responder = context.createSocket(SocketType.REP);
            responder.connect("tcp://localhost:5560");

            while (!Thread.currentThread().isInterrupted()) {
                //  Wait for next request from client
                String[] msg = responder.recvStr().split("\\|");

                if (Integer.parseInt(msg[0]) == 6379) {
                    this.redisClientKeys = RedisClient.create(RedisURI.Builder.redis("127.0.0.1", 6379).build());
                    this.connection = redisClientKeys.connect();
                } else if (Integer.parseInt(msg[0]) == 6381) {
                    this.redisClientRequest = RedisClient.create(RedisURI.Builder.redis("127.0.0.1", 6381).build());
                    this.connection = redisClientRequest.connect();
                } else if (Integer.parseInt(msg[0]) == 6383) {
                    this.redisClientResponse = RedisClient.create(RedisURI.Builder.redis("127.0.0.1", 6383).build());
                    this.connection = redisClientResponse.connect();
                }

                String rsp = "";
                if (msg[1].equals("get"))  rsp = connection.sync().get(msg[2]);
                if (msg[1].equals("set"))  rsp = connection.sync().set(msg[2], msg[3]);
                if (msg[1].equals("keys")) rsp = connection.sync().keys(msg[2]).toString();
                if (msg[1].equals("del"))  {
                    System.out.println(msg[2]);
                    rsp = connection.sync().del(msg[2]).toString();
                    System.out.println(rsp);
                }

                connection.close();
                if (Integer.parseInt(msg[0]) == 6379) redisClientKeys.shutdown();
                if (Integer.parseInt(msg[0]) == 6381) redisClientRequest.shutdown();
                if (Integer.parseInt(msg[0]) == 6383) redisClientResponse.shutdown();

                //  Send reply back to client
                if (rsp == null) rsp = "null";
                responder.send(rsp);
            }
        }
    }
}
