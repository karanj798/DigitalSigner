package server;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class RedisRequester {

    ZContext context;
    ZMQ.Socket requester;
    public RedisRequester() {
        context = new ZContext();
        requester = context.createSocket(SocketType.REQ);
        requester.connect("tcp://localhost:5559");
    }

    public String get(int port, String key) {
        requester.send(port + "|get|" + key, 0);
        return requester.recvStr(0);
    }

    public void set(int port, String key, String value) {
        requester.send(port + "|set|" + key + "|" + value, 0);
    }

    public String keys(int port) {
        requester.send(port + "|keys|*", 0);

        return requester.recvStr(0);
    }

    public void del(int port, String key) {
        requester.send(port + "|del|" + key, 0);
        requester.recvStr(0);
    }

    public void close() {
        requester.close();
        context.close();
    }
}
