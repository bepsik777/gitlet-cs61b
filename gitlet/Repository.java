package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        File targetFile = join(CWD, filePath);
        if (!targetFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        byte[] targetFileContent = readContents(targetFile);
        HashMap<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        if (stagingArea.containsKey(filePath)
                && Arrays.equals(targetFileContent, stagingArea.get(filePath))) {
            return;
        }
        /*
         * If current working version is same as in HEAD commit, remove from staging area
         */
        if (isCommitTrackedByHead(filePath, targetFileContent)) {
            stagingArea.remove(filePath);
            return;
        }

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
                saveObject(new Blob(stagingArea.get(key)));
            }
            String commitId = saveObject(newCommit);
            Refs.updateHead(commitId, activeBranch);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        StagingArea.clearStagingArea();
    }

    public static void remove(String filepath) {
        File file = join(CWD, filepath);
        if (!file.exists()) {
            System.out.println("No such file in the current directory");
            return;
        }
        HashMap<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        Commit headCommit = getHeadCommit();
        Map<String, String> trackedFiles = headCommit.getTrackedFiles();

        if (!stagingArea.containsKey(filepath) && !trackedFiles.containsKey(filepath)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        //Remove file from staging area if staged
        stagingArea.remove(filepath);

        //stage for removal and delete from CWD if file is tracked by head commit
        String fileId = sha1((Object) serialize(new Blob(file)));
        if (trackedFiles.containsKey(filepath) && trackedFiles.get(filepath).equals(fileId)) {
            stagingArea.put(filepath, null);
            restrictedDelete(filepath);
        }
        saveStagingArea(stagingArea);
    }

    public static void basicCheckout(String fileName) {
        basicCheckout(fileName, null);
    }

    public static void basicCheckout(String fileName, String commitID) {
        File targetFile = join(CWD, fileName);
        Commit targetCommit;
        if (commitID == null) {
            targetCommit = getHeadCommit();
        } else {
            targetCommit = getCommitByShaHash(commitID);
        }
        Map<String, String> filesTrackedByCommit = targetCommit.getTrackedFiles();
        if (!filesTrackedByCommit.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File trackedFile = getFileByShaHash(filesTrackedByCommit.get(fileName));
        Blob trackedBlob = readObject(trackedFile, Blob.class);
        byte[] trackedContent = trackedBlob.getFileContent();
        String deserializedContent = new String(trackedContent, StandardCharsets.UTF_8);
        writeContents(targetFile, deserializedContent);
    }

    public static void checkoutBranch(String branchName) {
        if (!branchName.equals(Refs.getActiveBranch())) {
            StagingArea.clearStagingArea();
        } else {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit currHeadCommit = getHeadCommit();
        Refs.switchBranch(branchName);
        Commit nextHeadCommit = getHeadCommit();
        Map<String, String> currTrackedFiles = currHeadCommit.getTrackedFiles();
        Map<String, String> nextTrackedFiles = nextHeadCommit.getTrackedFiles();
        // override or add files tracked bu head commit from the new branch
        for (String fileName: nextTrackedFiles.keySet()) {
            File checkedFile = join(CWD, fileName);
            String fileId = nextTrackedFiles.get(fileName);
            if (checkedFile.exists()) {
                checkoutFile(fileId, fileName);
            } else {
                addFileToCWD(fileId, fileName);
            }
            if (currTrackedFiles.containsKey(fileName)) {
                currTrackedFiles.remove(fileName);
            }
        }
        // Remove each file tracked by previous branch head from CWD
        for (String fileName: currTrackedFiles.keySet()) {
            File file = join(CWD, fileName);
            file.delete();
        }
    }

    public static void branch(String branchName) {
        List<String> filesInHeadsDir = plainFilenamesIn(Refs.HEADS_DIR);
        if (filesInHeadsDir != null && filesInHeadsDir.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        String headCommitID = getCommitId(getHeadCommit());
        Refs.createNewBranch(branchName, headCommitID);
    }

    public static void log(Commit commit, String commitID) {
        commit.log(commitID);
        if (commit.getParentID() == null) {
            return;
        }
        String parentId = commit.getParentID();
        Commit parentCommit = commit.getParentCommit();
        log(parentCommit, parentId);
    }

    public static void log() {
        String headId = Refs.getHeadCommitId();
        Commit headCommit = getHeadCommit();
        log(headCommit, headId);
    }

    public static void globalLog() {
        List<Commit> allHeadCommits = Refs.getAllBranchesHeadsCommits();
        HashMap<String, Boolean> visitedCommits = new HashMap<String, Boolean>();
        for(Commit commit: allHeadCommits) {
            String commitID = getCommitId(commit);
            globalLog(visitedCommits, commit, commitID);
        }
    }

    private static void globalLog(HashMap<String, Boolean> visitedCommits, Commit currCommit, String currCommitID) {
        currCommit.log(currCommitID);
        visitedCommits.put(currCommitID, true);
        if (currCommit.getParentID() == null) {
            return;
        }
        if (visitedCommits.containsKey(currCommit.getParentID())) {
            return;
        }
        globalLog(visitedCommits, currCommit.getParentCommit(), currCommit.getParentID());
    }
}
