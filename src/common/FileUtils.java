package common;

public class FileUtils {

    private String getProjectPath() {
        return System.getProperty("user.dir");
    }

    public String getResourcesPath() {
        return this.getProjectPath() + "/resources/";
    }
}
