package common.model;

import java.io.Serializable;
import java.util.List;

public class Request implements Serializable {
    private final String fileName;
    private final List<String> signerList;

    public Request(String fileName, List<String> signerList) {
        this.fileName = fileName;
        this.signerList = signerList;
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getSignerList() {
        return signerList;
    }

    @Override
    public String toString() {
        return "{" +
                "fileName:'" + fileName + '\'' +
                ", signerList:" + signerList +
                '}';
    }
}
