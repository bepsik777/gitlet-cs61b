package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import gitlet.StagingArea;

import static gitlet.Utils.*;


/**
 * Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author TODO
 */
public class Commit implements Dumpable {
    private final String OBJECT_TYPE = "commit";
    /**
     * The message of this Commit.
     */
    private final String message;

    /**
     * Timestamp of this Commit
     */
    private final Date timestamp;

    /**
     * Parent Commit of this Commit
     */
    private final String parentID;

    /**
     * Author of this Commit
     */
    private final String author;

    /**
     * List of tracked files, identified by their SHA-1 hashes
     */
    private Map<String, String> trackedFiles;

    public Commit(String msg, String parentID, String author) {
        this.message = msg;
        this.author = author;
        this.parentID = parentID;
        if (parentID == null) {
            this.timestamp = new Date(0);
        } else {
            this.timestamp = new Date();
        }
        this.trackedFiles = new TreeMap<>();
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getParentID() {
        return this.parentID;
    }

    public String getAuthor() {
        return this.author;
    }

    public Map<String, String> getTrackedFiles() {
        return this.trackedFiles;
    }

    public void setTrackedFiles(Map<String, byte[]> stagingArea) {
        // Retrieve parent commit if exist
        Commit parentCommit = getParentCommit();
        if (parentCommit != null) {
            Map<String, String> parentTrackedFiles = parentCommit.getTrackedFiles();
            this.trackedFiles.putAll(parentTrackedFiles);
        }

        // Update tracked files with files from staging area
        TreeMap<String, String> stagingAreaIndex = new TreeMap<>();
        for (String key: stagingArea.keySet()) {
            byte[] fileContent = stagingArea.get(key);

            // If file is staged for removal, remove reference to it in new commit
            if (fileContent == null) {
                this.trackedFiles.remove(key);
                continue;
            }
            String fileShaHash = sha1((Object) serialize(new Blob(fileContent)));
            stagingAreaIndex.put(key, fileShaHash);
        }
        this.trackedFiles.putAll(stagingAreaIndex);
    }

    private Commit getParentCommit() {
        if (parentID != null) {
            File dir = join(Repository.OBJECTS, parentID.substring(0, 2));
            File fileName = join(dir, parentID.substring((2)));
            return readObject(fileName, Commit.class);
        }
        return null;
    }

    public void dump() {
        System.out.println("message: " + getMessage());
        System.out.println("parentId: " + getParentID());
        System.out.println("date: " + getTimestamp());
        System.out.println("tracked files: ");
        Map<String, String> trackedFiles = getTrackedFiles();
        for (String fileName: trackedFiles.keySet()) {
            System.out.println(fileName + ": " + trackedFiles.get(fileName));
        }
    }
}
