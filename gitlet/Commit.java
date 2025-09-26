package gitlet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import static gitlet.Utils.*;


/**
 * Represents a gitlet commit object.
 *  does at a high level.
 * @author apotocki
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
    private final String parentId;

    /**
     * Second parent Commit of this Commit if is a merge commit
     */
    private String secondParentId;

    /**
     * Author of this Commit
     */
    private final String author;

    /**
     * List of tracked files, identified by their SHA-1 hashes
     */
    private final Map<String, String> trackedFiles;

    public Commit(String msg, String parentID, String author) {
        this.message = msg;
        this.author = author;
        this.parentId = parentID;
        this.secondParentId = null;
        if (parentID == null) {
            this.timestamp = new Date(0);
        } else {
            this.timestamp = new Date();
        }
        this.trackedFiles = new TreeMap<>();
    }

    public Commit(String msg, String parentID, String secondParentID, String author) {
        this(msg, parentID, author);
        this.secondParentId = secondParentID;
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getParentId() {
        return this.parentId;
    }

    public String getSecondParentId() {
        return this.secondParentId;
    }

    public Commit getParentCommit() {
        if (parentId != null) {
            return getCommitByShaHash(this.parentId);
        }
        return null;
    }

    public Commit getSecondParentCommit() {
        if (secondParentId != null) {
            return getCommitByShaHash(this.secondParentId);
        }
        return null;
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
        Commit secondParentCommit = getSecondParentCommit();
        if (parentCommit != null) {
            Map<String, String> parentTrackedFiles = parentCommit.getTrackedFiles();
            this.trackedFiles.putAll(parentTrackedFiles);
        }
        if (secondParentCommit != null) {
            Map<String, String> parentTrackedFiles = secondParentCommit.getTrackedFiles();
            this.trackedFiles.putAll(parentTrackedFiles);
        }

        // Update tracked files with files from staging area
        TreeMap<String, String> stagingAreaIndex = new TreeMap<>();
        for (String key : stagingArea.keySet()) {
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

    public void log(String id) {
        String dateFormat = "EEE LLL d kk:mm:ss yyyy Z";
        SimpleDateFormat sf = new SimpleDateFormat(dateFormat);
        System.out.println("===");
        System.out.println("commit " + id);
        System.out.println("Date: " + sf.format(getTimestamp()));
        System.out.println(getMessage());
        System.out.println();
    }

    public void dump() {
        System.out.println("message: " + getMessage());
        System.out.println("parentId: " + getParentId());
        System.out.println("date: " + getTimestamp());
        System.out.println("tracked files: ");
        for (String fileName : getTrackedFiles().keySet()) {
            System.out.println(fileName + ": " + trackedFiles.get(fileName));
        }
    }
}
