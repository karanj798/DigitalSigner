package server;

import java.io.Serializable;

public class ReplicaMsg {
    private String key;
    private long sequence;
    private byte[] body;

    public ReplicaMsg(String key, long sequence, byte[] body) {
        this.key = key;
        this.sequence = sequence;
        this.body = body; // clone if needed
    }

    public String getKey(){
        return key;
    }

    public long getSequence(){
        return sequence;
    }

    public void setSequence(long sequence){
        this.sequence = sequence;
    }

    public byte[] getBody(){
        return body;
    }


}
