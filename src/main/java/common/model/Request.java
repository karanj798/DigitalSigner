package common.model;

import java.io.Serializable;
import java.util.List;

/**
 * This class is used for capturing Client's request, mainly using Object Serialization.
 */
public class Request implements Serializable {
    private final String fileName;
    private final List<String> signerList;

    /**
     * Constructor which initializes instance variables.
     * @param fileName Name of the file.
     * @param signerList {@code List<String>} of people who need to sign this document.
     */
    public Request(String fileName, List<String> signerList) {
        this.fileName = fileName;
        this.signerList = signerList;
    }

    /**
     * This method returns the name of the file.
     * @return Name of the file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * This method returns the names of people who need to sign the document.
     * @return {@code List<String>} of people who need to sign this document.
     */
    public List<String> getSignerList() {
        return signerList;
    }

    /**
     * Overriding toString method and output data in JSON format.
     * @return values of the instance variable in JSON format.
     */
    @Override
    public String toString() {
        return "{" +
                "fileName:'" + fileName + '\'' +
                ", signerList:" + signerList +
                '}';
    }
}
