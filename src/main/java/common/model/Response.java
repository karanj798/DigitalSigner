package common.model;

import java.io.Serializable;
import java.util.List;

/**
 * This class is used for capturing Client's response, mainly using Object Serialization.
 */
public class Response implements Serializable {
    private final String fileName;
    private final List<String> signerList;
    private final List<Long> timeStampList;

    /**
     * Constructor which initializes instance variables.
     * @param fileName Name of the file.
     * @param signerList {@code List<String>} of people who need to sign this document.
     * @param timeStampList {@code List<String>} of all the timestamps.
     */
    public Response(String fileName, List<String> signerList, List<Long> timeStampList) {
        this.fileName = fileName;
        this.signerList = signerList;
        this.timeStampList = timeStampList;
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
     * This method returns list of timestamp of when the clients signed the document.
     * @return {@code List<Long>} of all the timestamps.
     */
    public List<Long> getTimeStampList() {
        return timeStampList;
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
                ", timeStampList:" + timeStampList +
                '}';
    }
}
