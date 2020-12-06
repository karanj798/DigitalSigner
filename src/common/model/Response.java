package common.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class Response implements Serializable {
    private final String fileName;
    private final List<String> signerList;
    private final List<Long> timeStampList;

    public Response(String fileName, List<String> signerList, List<Long> timeStampList) {
        this.fileName = fileName;
        this.signerList = signerList;
        this.timeStampList = timeStampList;
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getSignerList() {
        return signerList;
    }

    public List<Long> getTimeStampList() {
        return timeStampList;
    }

    @Override
    public String toString() {
        return "{" +
                "fileName:'" + fileName + '\'' +
                ", signerList:" + signerList +
                ", timeStampList:" + timeStampList +
                '}';
    }
}
