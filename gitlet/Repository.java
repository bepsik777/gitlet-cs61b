package gitlet;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static gitlet.StagingArea.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author TODO
 */
public class Repository {
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The refs directory
     */
    static public final File OBJECTS = join(Repository.GITLET_DIR, "objects");
    /**
     * The STAGING_AREA File
     */
    public static final File STAGING_AREA = join(GITLET_DIR, "STAGING_AREA");

    /**
     * Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that contains no
     * files and has the commit message initial commit (just like that, with no punctuation).
     * It will have a single branch: master, which initially points to this initial commit,
     * and master will be the current branch. The timestamp for this initial commit will
     * be 00:00:00 UTC, Thursday, 1 January 1970 in whatever format
     * you choose for dates (this is called “The (Unix) Epoch”, represented internally by the time 0.)
     * Since the initial commit in all repositories created by Gitlet will have exactly the same content,
     * it follows that all repositories will automatically share this commit (they will all have the same UID)
     * and all commits in all repositories will trace back to it.
     */

    public static void init() {
        if (!GITLET_DIR.exists()) {
            try {
                //Init gitlet repo
                GITLET_DIR.mkdir();
                OBJECTS.mkdir();
                //Init staging area
                STAGING_AREA.createNewFile();
                writeObject(STAGING_AREA, new StagingArea());
                //Init refs directory and HEAD file
                Refs.init();
                //Initial commit
                Commit initialCommit = new Commit("initial commit", null, null);
                String initialCommitId = saveObject(initialCommit);
                Refs.updateHead(initialCommitId, "master");
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
        }
    }

    public static void add(String filePath) {
        byte[] targetFileContent = serialize(new Blob(readContents(join(CWD, filePath))));
        HashMap<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        if (stagingArea.containsKey(filePath)
                && Arrays.equals(targetFileContent, stagingArea.get(filePath))) {
            return;
        }

        /*
         * Add:
         * if current working version is same as in HEAD commit, remove from staging area
         */

        stagingArea.put(filePath, targetFileContent);
        saveStagingArea(stagingArea);
    }

    public static void commit(String message) {
        Map<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        if (stagingArea.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        String headCommitId = Refs.getHeadCommitId();
        String activeBranch = Refs.getActiveBranch();
        Commit newCommit = new Commit(message, headCommitId, activeBranch);
        newCommit.setTrackedFiles(StagingArea.getNewestStagingArea());

        try {
            for (String key : stagingArea.keySet()) {
                saveSerializedObject(stagingArea.get(key));
            }
            String commitId = saveObject(newCommit);
            Refs.updateHead(commitId, "master");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        StagingArea.clearStagingArea();
    }
}
