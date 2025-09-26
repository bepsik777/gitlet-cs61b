package gitlet;
import java.util.HashMap;

import static gitlet.Utils.*;

public class StagingArea implements Dumpable {
    private final HashMap<String, byte[]> stagingArea;

    public StagingArea() {
        stagingArea = new HashMap<>();
    }

    public StagingArea(HashMap<String, byte[]> stagingArea) {
        this.stagingArea = stagingArea;
    }

    public HashMap<String, byte[]> getStagingArea() {
        return stagingArea;
    }

    public static HashMap<String, byte[]> getNewestStagingArea() {
        StagingArea sa = readObject(Repository.STAGING_AREA, StagingArea.class);
        return sa.getStagingArea();
    }

    public static void saveStagingArea(HashMap<String, byte[]> sa) {
        writeObject(Repository.STAGING_AREA, new StagingArea(sa));
    }

    public static void clearStagingArea() {
        HashMap<String, byte[]> stagingArea = getNewestStagingArea();
        stagingArea.clear();
        saveStagingArea(stagingArea);
    }

    public void dump() {

    }
}
