package common;

/**
 * This class is used for fetching path to resources folder.
 */
public class FileUtils {

    /**
     * Method gets the current working directory.
     * @return current working directory.
     */
    private String getProjectPath() {
        return System.getProperty("user.dir");
    }

    /**
     * Method appends resources to the current working directory.
     * @return path to resources folder.
     */
    public String getResourcesPath() {
        return this.getProjectPath() + "/resources/";
    }
}
