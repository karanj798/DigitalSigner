package server;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.zeromq.*;

import java.util.LinkedList;
import java.util.Queue;

public class RedisBroker extends Thread{
    private final int SERVER_TASKS = 1;
    private final int REDIS_WORKERS = 2;

    private String redisHost;
    private int redisPort;

    public RedisBroker(String host, int port){
        this.redisHost = host;
        this.redisPort = port;
    }

    @Override
    public void run(){
        this.start();
    }

    public void start() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket serverSocket = context.createSocket(SocketType.ROUTER);
            ZMQ.Socket redisSocket = context.createSocket(SocketType.ROUTER);
            serverSocket.bind("ipc://server-task.ipc");
            redisSocket.bind("ipc://redis-connector.ipc");

            for (int i = 0; i < REDIS_WORKERS; i++) {
                new RedisConnectorWorker(redisHost, redisPort).start();
                new RedisConnectorWorker(redisHost, redisPort + 2).start();
                new RedisConnectorWorker(redisHost, redisPort + 4).start();
            }

            for (int i = 0; i < SERVER_TASKS; i++) new ServerTask().start();

            Queue<String> workerQueue = new LinkedList<String>();

            while (!Thread.currentThread().isInterrupted()) {
                ZMQ.Poller items = context.createPoller(2);
                items.register(redisSocket, ZMQ.Poller.POLLIN);

                if (workerQueue.size() > 0)
                    items.register(serverSocket, ZMQ.Poller.POLLIN);

                if (items.poll() < 0) break;

                if (items.pollin(0)) {
                    workerQueue.add(redisSocket.recvStr());

                    //  Second frame is empty
                    String empty = redisSocket.recvStr();
                }
            }
        }
    }


    class ServerTask extends Thread{
        private ZContext context;
        private ZMQ.Socket client;

        @Override
        public void run(){
            startServerConn();
            while (!Thread.currentThread().isInterrupted())
            closeServerConn();
        }

        public void startServerConn(){
            try {
                this.context = new ZContext();
                client = context.createSocket(SocketType.REQ);

                client.connect("ipc://redis-connector.ipc");

                //  Send request, get reply
                client.send("Hello");
                String reply = client.recvStr();
                System.out.println("Redis Worker: " + reply);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void closeServerConn(){
            client.close();
            context.close();
        }
    }

    class RedisConnectorWorker extends Thread{
        private ZContext context;
        private ZMQ.Socket client;

        //private RedisURI redisURI;
        private RedisClient redisClientKeys, redisClientRequest, redisClientResponse;
        private StatefulRedisConnection<String, String> connection;

        private RedisAsyncCommands<String, String> async;
        private RedisKeyCommands<String, String> syncKeyCmd;
        private RedisStringCommands<String, String> syncStringCmd;

        int portNum;

        public RedisConnectorWorker(String host, int portNum){
            this.portNum = portNum;
            RedisURI redisURIKeys = RedisURI.Builder.redis(host, portNum).build();
            this.redisClientKeys = RedisClient.create(redisURIKeys);

            RedisURI redisURIRequest = RedisURI.Builder.redis(host, portNum + 2).build();
            this.redisClientRequest = RedisClient.create(redisURIRequest);

            RedisURI redisURIResponse = RedisURI.Builder.redis(host, portNum + 4).build();
            this.redisClientResponse = RedisClient.create(redisURIResponse);
        }

        @Override
        public void run(){
            startConnection();
            while (!Thread.currentThread().isInterrupted()){
                String[] msg = client.recvStr().split(",");
                String rsp = "";
                if(msg[0].equals("get")) {
                    rsp = connection.sync().get(msg[1]);
                } else if(msg[0].equals("set")){
                    rsp = connection.sync().set(msg[1], msg[2]);
                } else if(msg[0].equals("keys")){
                    rsp = connection.sync().keys(msg[1]).toString();
                } else if(msg[0].equals("del")){
                    rsp = connection.sync().del(msg[1]).toString();
                }

                client.send(rsp);
            }
            closeConnection();
        }

        public void startConnection(){
            try {
                this.context = new ZContext();
                client = context.createSocket(SocketType.REQ);

                client.connect("ipc://server-task.ipc");

                //  Send request, get reply
                client.send("READY");
                String reply = client.recvStr();
                System.out.println("Server Task: " + reply);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void closeConnection(){
            connection.close();
            //redisClient.shutdown();

            //zmq
            client.close();
            context.close();
        }

    }

}
