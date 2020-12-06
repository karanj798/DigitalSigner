package server;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.zeromq.*;

import java.util.LinkedList;
import java.util.Queue;

public class RedisBroker extends Thread{
    private final int SERVER_TASKS = 1;
    private final int REDIS_WORKERS = 10;

    private String redisHost;
    private int redisPort;

    private ServerTask serverTask;
    private Queue<String> workerQueue;

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
            }

            serverTask = new ServerTask();
            serverTask.start();

            workerQueue = new LinkedList<String>();

            ZMQ.Poller items = context.createPoller(2);
            items.register(redisSocket, ZMQ.Poller.POLLIN);

            while (!Thread.currentThread().isInterrupted()) {

                if (workerQueue.size() > 0)
                    items.register(serverSocket, ZMQ.Poller.POLLIN);

                if (items.poll() < 0) break;

                if (items.pollin(0)) {
                    workerQueue.add(redisSocket.recvStr());

                    //  Second frame is empty
                    String empty = redisSocket.recvStr();
                }

                if (items.pollin(1)) {
                    String reqMsg = workerQueue.poll();
                    redisSocket.send(reqMsg);
                }
            }
        }
    }

    public String getToken(int port, String key){
        String reqMsg = port + ",get," + key;
        return serverTask.request(reqMsg);
    }

    public String setToken(int port, String key, String val){
        String reqMsg = port + ",set," + key + "," + val;
        return serverTask.request(reqMsg);
    }

    public String getKeys(int port, String pattern){
        String reqMsg = port + ",keys," + pattern;
        return serverTask.request(reqMsg);
    }

    public String delToken(int port, String key){
        String reqMsg = port + ",del," + key;
        return serverTask.request(reqMsg);
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

        public String request(String reqMsg){
            client.send(reqMsg);
            return client.recvStr();
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

        int portNum;

        public RedisConnectorWorker(String host, int portNum){
            this.portNum = portNum;
            RedisURI redisURI = RedisURI.Builder.redis(host, portNum).build();
            this.redisClientKeys = RedisClient.create(redisURI);

            redisURI = RedisURI.Builder.redis(host, portNum + 2).build();
            this.redisClientRequest = RedisClient.create(redisURI);

            redisURI = RedisURI.Builder.redis(host, portNum + 4).build();
            this.redisClientResponse = RedisClient.create(redisURI);
        }

        @Override
        public void run(){
            startConnection();

            while (!Thread.currentThread().isInterrupted()){
                String[] msg = client.recvStr().split(",");
                switch (Integer.valueOf(msg[0])){
                    case(6379):
                        this.connection = redisClientKeys.connect();
                        break;
                    case(6381):
                        this.connection = redisClientRequest.connect();
                        break;
                    case(6383):
                        this.connection = redisClientResponse.connect();
                        break;
                    default:
                        break;
                }

                String rsp = "";
                if(msg[1].equals("get")) {
                    //rsp = connection.sync().get(msg[2]);
                    System.out.println("get requested");
                } else if(msg[1].equals("set")){
                    //rsp = connection.sync().set(msg[2], msg[3]);
                } else if(msg[1].equals("keys")){
                    //rsp = connection.sync().keys(msg[2]).toString();
                } else if(msg[1].equals("del")){
                    //rsp = connection.sync().del(msg[2]).toString();
                }

                client.send(rsp);
                connection.close();
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
            redisClientKeys.shutdown();
            redisClientRequest.shutdown();
            redisClientResponse.shutdown();

            //zmq
            client.close();
            context.close();
        }

    }

}
