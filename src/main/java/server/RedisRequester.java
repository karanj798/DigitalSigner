package server;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * This class sends messages to ZeroMQ broker {@code RedisBroker} class.
 */
public class RedisRequester {

    ZContext context;
    ZMQ.Socket requester;

    /**
     * Initialize connection.
     */
    public RedisRequester() {
        context = new ZContext();
        requester = context.createSocket(SocketType.REQ);
        requester.connect("tcp://localhost:5559");
    }

    /**
     * Sends a get message.
     * @param port The port number of Redis instance that is to be used.
     * @param key The key that is used to query Redis instance.
     * @return String value which is mapped to key.
     */
    public String get(int port, String key) {
        requester.send(port + "|get|" + key, 0);
        return requester.recvStr(0);
    }

    /**
     * Sends a set message.
     * @param port The port number of Redis instance that is to be used.
     * @param key The key that is used to insert in Redis instance.
     * @param value The value that should be mapped to the key.
     */
    public void set(int port, String key, String value) {
        requester.send(port + "|set|" + key + "|" + value, 0);
        requester.recvStr(0);
    }

    /**
     * Sends keys message, retrieves all the keys from Redis instance.
     * @param port The port number of Redis instance that is to be used.
     * @return {@code List<String} of keys in Redis instance.
     */
    public String keys(int port) {
        requester.send(port + "|keys|*", 0);
        return requester.recvStr(0);
    }

    /**
     * Sends a delete message.
     * @param port The port number of Redis instance that is to be used.
     * @param key The key whose entry is to be deleted.
     */
    public void del(int port, String key) {
        requester.send(port + "|del|" + key, 0);
        requester.recvStr(0);
    }

    /**
     * Closes the ZeroMQ connection.
     */
    public void close() {
        requester.close();
        context.close();
    }
}
