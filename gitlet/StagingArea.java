package gitlet;
import java.util.HashMap;

import static gitlet.Utils.*;

public class StagingArea implements Dumpable {
    private final HashMap<String, String> stagingArea;

    public StagingArea() {
        stagingArea = new HashMap<>();
    }

    public StagingArea(HashMap<String, String> stagingArea) {
        this.stagingArea = stagingArea;
    }

    public HashMap<String, String> getStagingArea() {
        return stagingArea;
    }

    public static HashMap<String, String> getNewestStagingArea() {
        StagingArea sa = readObject(Repository.STAGING_AREA, StagingArea.class);
        return sa.getStagingArea();
    }

    public static void saveStagingArea(HashMap<String, String> sa) {
        writeObject(Repository.STAGING_AREA, new StagingArea(sa));
    }

    public void dump() {

    }
}
