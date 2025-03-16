package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.util.Date;
import java.util.Map;

import static gitlet.Utils.*;


/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Dumpable {
    /** The message of this Commit. */
    private final String message;

    /** Timestamp of this Commit*/
    private final Date timestamp;

    /** Parent Commit of this Commit */
    private final String parentID;

    /** Author of this Commit */
    private final String author;

    /** List of tracked files, identified by their SHA-1 hashes*/
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

    public void saveCommit(String id) {
        String firstTwoCharOfId = id.substring(0, 2);
        File objectsSubfolder = join(Repository.OBJECTS, firstTwoCharOfId);
        if (!objectsSubfolder.exists()) {
            try {
                objectsSubfolder.mkdir();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        File commitFile = join(objectsSubfolder, id.substring(2));
        writeObject(commitFile, this);

        try {
            commitFile.createNewFile();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void dump(){}
}
