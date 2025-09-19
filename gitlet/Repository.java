package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;

import static gitlet.StagingArea.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *
 * @author apotocki
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
        if (isFileTrackedByHeadAndUnchanged(filePath, targetFileContent)) {
            stagingArea.remove(filePath);
            saveStagingArea(stagingArea);
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
        HashMap<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        Commit headCommit = getHeadCommit();
        Map<String, String> trackedFiles = headCommit.getTrackedFiles();
        if (!stagingArea.containsKey(filepath) && !trackedFiles.containsKey(filepath)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        //unstage
        if (stagingArea.containsKey(filepath)) {
            stagingArea.remove(filepath);
        }
        //stage for removal and delete from CWD if file is tracked by head commit
        if (trackedFiles.containsKey(filepath)) {
            stagingArea.put(filepath, null);
            restrictedDelete(filepath);
        }
        saveStagingArea(stagingArea);
    }

    public static void basicCheckout(String fileName) {
        basicCheckout(fileName, null);
    }

    public static void basicCheckout(String fileName, String commitID) {
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
        checkoutFile(filesTrackedByCommit.get(fileName), fileName);
    }

    private static void checkoutCommit(Commit targetCommit, Commit currCommit) {
        Map<String, String> trackedFiles = targetCommit.getTrackedFiles();
        Map<String, String> trackedFilesByHeadCommit = currCommit.getTrackedFiles();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        for (String file: filesInCWD) {
            if (!trackedFilesByHeadCommit.containsKey(file) && trackedFiles.containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        for (String file: trackedFiles.keySet()) {
            if (filesInCWD.contains(file)) {
                checkoutFile(trackedFiles.get(file), file);
            } else {
                addFileToCWD(trackedFiles.get(file), file);
            }
        }
        for (String file: trackedFilesByHeadCommit.keySet()) {
            if (!trackedFiles.containsKey(file)) {
                File fileToRemove = join(CWD, file);
                fileToRemove.delete();
            }
        }
    }

    public static void checkoutBranch(String branchName) {
        if (!branchName.equals(Refs.getActiveBranch())) {
            StagingArea.clearStagingArea();
        } else {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        List<String> allBranchesNames = Refs.getAllBranchesNames();
        if (!allBranchesNames.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        Commit currHeadCommit = getHeadCommit();
        Refs.switchBranch(branchName);
        Commit nextHeadCommit = getHeadCommit();
        checkoutCommit(nextHeadCommit, currHeadCommit);
    }

    public static void merge(String targetBranch) {
        boolean isConflicted = false;
        HashMap<String, byte[]> sa = StagingArea.getNewestStagingArea();
        if (!sa.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        List<String> allBranches = Refs.getAllBranchesNames();
        if (!allBranches.contains(targetBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String currBranch = Refs.getActiveBranch();
        if (currBranch.equals(targetBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit currBranchHead = getHeadCommit();
        String currBranchHeadID = getCommitId(currBranchHead);
        Commit targetBranchHead = getHeadCommit(targetBranch);
        String targetBranchHeadID = getCommitId(targetBranchHead);
        Commit splitPointCommit = findSplitPoint(currBranch, targetBranch);
        String splitPointCommitID = getCommitId(splitPointCommit);
        assert splitPointCommit != null;
        if (splitPointCommitID.equals(targetBranchHeadID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitPointCommitID.equals(currBranchHeadID)) {
            checkoutBranch(targetBranch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Map<String, String> currBranchTrackedFiles = currBranchHead.getTrackedFiles();
        Map<String, String> targetBranchTrackedFiles = targetBranchHead.getTrackedFiles();
        Map<String, String> splitPointTrackedFiles = splitPointCommit.getTrackedFiles();
        // check for untracked files
        List<String> filesInCWD = plainFilenamesIn(CWD);
        for (String file: filesInCWD) {
            if (!currBranchTrackedFiles.containsKey(file) && targetBranchTrackedFiles.containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        for (String file: currBranchTrackedFiles.keySet()) {
            String fileAtSplitPointHash = splitPointTrackedFiles.get(file);
            String fileAtCurrBranchHash = currBranchTrackedFiles.get(file);
            String fileAtTargetBranchHash = targetBranchTrackedFiles.get(file);
            File f = join(CWD, file);
            if (fileAtSplitPointHash != null) {
                if (fileAtSplitPointHash.equals(fileAtCurrBranchHash) && fileAtTargetBranchHash == null) {
                    // unmodified in curr branch, absent in target branch
                    remove(file);
                } else if (fileAtSplitPointHash.equals(fileAtCurrBranchHash) && !fileAtSplitPointHash.equals(fileAtTargetBranchHash)) {
                    // unmodified in curr branch, modified in target branch
                    basicCheckout(file, getCommitId(targetBranchHead));
                    add(file);
                } else if (fileAtTargetBranchHash == null || !fileAtCurrBranchHash.equals(fileAtTargetBranchHash)) {
                    // present at split point, modified in different ways (conflict)
                    String currBranchFileHash = currBranchTrackedFiles.get(file);
                    String targetBranchFileHash = targetBranchTrackedFiles.get(file);
                    File currBranchFileVersion = currBranchFileHash != null ? getFileByShaHash(currBranchFileHash) : null;
                    File targetBranchFileVersion = targetBranchFileHash != null ? getFileByShaHash(targetBranchFileHash) : null;
                    String mergeContent = getConflictedFilesContent(currBranchFileVersion, targetBranchFileVersion);
                    writeContents(f, mergeContent);
                    add(file);
                    isConflicted = true;
                }
            }
            targetBranchTrackedFiles.remove(file);
        }
        for (String file: new ArrayList<>(targetBranchTrackedFiles.keySet())) {
            String fileAtSplitPointHash = splitPointTrackedFiles.get(file);
            String fileAtTargetBranchHash = targetBranchTrackedFiles.get(file);
            if (fileAtSplitPointHash != null && fileAtSplitPointHash.equals(fileAtTargetBranchHash)) {
                targetBranchTrackedFiles.remove(file);
            }
            //absent from split point and curr branch, present in target branch
            if (fileAtSplitPointHash == null) {
                basicCheckout(file, Refs.getHeadCommitId(targetBranch));
                add(file);
            }
        }
        String msg = "Merged "+ targetBranch +" into " + currBranch + ".";
        commitMerge(msg, getCommitId(currBranchHead), getCommitId(targetBranchHead));
        if (isConflicted) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static String getConflictedFilesContent(File serializedCurrBranchVersion, File serializedTargetBranchVersion) {
        String currBranchVersionContent = serializedCurrBranchVersion != null ? getSerializedFileContent(serializedCurrBranchVersion): "";
        String targetBranchVersionContent = serializedTargetBranchVersion != null ? getSerializedFileContent(serializedTargetBranchVersion): "";
        return "<<<<<<< HEAD\n" + currBranchVersionContent + "=======\n" + targetBranchVersionContent + ">>>>>>>\n";
    }

    private static Commit findSplitPoint(String currBranch, String targetBranch) {
        List<String> currBranchCommitHistory = getCommitHistory(Refs.getHeadCommitId(currBranch));
        List<String> targetBranchCommitHistory = getCommitHistory(Refs.getHeadCommitId(targetBranch));
        for (String c: targetBranchCommitHistory) {
            if (currBranchCommitHistory.contains(c)) {
                return getCommitByShaHash(c);
            }
        }
        return null;
    }

    private static List<String> getCommitHistory(String commitID) {
        List<String> res = new ArrayList<>();
        Queue<String> commitQueue = new ArrayDeque<>();
        commitQueue.add(commitID);
        while (commitQueue.peek() != null) {
             String currCommitID = commitQueue.poll();
             if (res.contains(currCommitID)) {
                 continue;
             } else {
                 res.add(currCommitID);
             }
             Commit currCommit = getCommitByShaHash(currCommitID);
             String parentIdOne = currCommit.getParentID();
             String parentIdTwo = currCommit.getSecondParentID();
             if (parentIdOne != null) {
                 commitQueue.add(parentIdOne);
             }
             if (parentIdTwo != null) {
                 commitQueue.add(parentIdTwo);
             }
        }
        return res;
    }

    private static void commitMerge(String message, String currBranchHeadID, String targetBranchHeadID) {
        Map<String, byte[]> stagingArea = StagingArea.getNewestStagingArea();
        if (stagingArea.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        String activeBranch = Refs.getActiveBranch();
        Commit newCommit = new Commit(message, currBranchHeadID, targetBranchHeadID, activeBranch);
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

    public static void removeBranch(String branchName) {
        String activeBranch = Refs.getActiveBranch();
        if (activeBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        List<String> allBranchesNames = Refs.getAllBranchesNames();
        if (!allBranchesNames.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        File branchPointer = join(Refs.HEADS_DIR, branchName);
        try {
            branchPointer.delete();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            ;
        }
    }

    public static void reset(String commitID) {
        Commit targetCommit = getCommitByShaHash(commitID);
        Commit currCommit = getHeadCommit();
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        checkoutCommit(targetCommit, currCommit);
        Refs.updateHead(commitID, Refs.getActiveBranch());
        StagingArea.clearStagingArea();
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
        HashMap<String, Commit> allCommits = getAllCommits();
        for (String key : allCommits.keySet()) {
            Commit c = allCommits.get(key);
            c.log(key);
        }
    }

    public static void find(String msg) {
        HashMap<String, Commit> allCommits = getAllCommits();
        boolean atLeastOneMsgFound = false;
        for (String key : allCommits.keySet()) {
            Commit commit = allCommits.get(key);
            if (commit.getMessage().equals(msg)) {
                if (!atLeastOneMsgFound) {
                    atLeastOneMsgFound = true;
                }
                System.out.println(key);
            }
        }
        if (!atLeastOneMsgFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    private static HashMap<String, Commit> getAllCommits() {
        List<Commit> allHeadCommits = Refs.getAllBranchesHeadsCommits();
        HashMap<String, Commit> allCommits = new HashMap<>();
        for (Commit c : allHeadCommits) {
            getAllCommits(c, allCommits);
        }
        return allCommits;
    }

    private static void getAllCommits(Commit commit, HashMap<String, Commit> allCommits) {
        String commitID = getCommitId(commit);
        String parentID = commit.getParentID();
        allCommits.put(commitID, commit);
        if (parentID == null || allCommits.containsKey(parentID)) {
            return;
        }
        Commit parentCommit = commit.getParentCommit();
        getAllCommits(parentCommit, allCommits);
    }

    public static void status() {
        String activeBranchName = Refs.getActiveBranch();
        List<String> allBranchesNames = Refs.getAllBranchesNames();
        HashMap<String, byte[]> sa = StagingArea.getNewestStagingArea();
        Commit head = getHeadCommit();
        Map<String, String> trackedByHead = head.getTrackedFiles();
        List<String> filesInCwd = plainFilenamesIn(CWD);
        TreeSet<String> modified = new TreeSet<>();
        TreeSet<String> deleted = new TreeSet<>();
        TreeSet<String> untrackedUnstaged = new TreeSet<>();

        for (String fileName : filesInCwd) {
            boolean isTrackedByHead = isFileTrackedByHead(fileName);
            boolean isStaged = sa.containsKey(fileName);
            byte[] fileContent = readContents(join(CWD, fileName));
            if (isTrackedByHead && !isStaged) {
                boolean isTrackedAndUnmodified = isFileTrackedByHeadAndUnchanged(fileName, fileContent, trackedByHead);
                if (!isTrackedAndUnmodified) {
                    modified.add(fileName + " (modified)");
                    trackedByHead.remove(fileName);
                }
            }
            if (isStaged) {
                byte[] stagedContent = sa.get(fileName);
                if (!Arrays.equals(fileContent, stagedContent) && stagedContent != null) {
                    modified.add(fileName + " (modified)");
                    sa.remove(fileName);
                }
            }
            if (!isTrackedByHead && (!isStaged || sa.get(fileName) == null)) {
                untrackedUnstaged.add(fileName);
                sa.remove(fileName);
            }
        }

        for (String fileName : sa.keySet()) {
            byte[] fileContent = sa.get(fileName);
            File file = join(CWD, fileName);
            if (fileContent != null && !file.exists()) {
                deleted.add(fileName + " (deleted)");
                sa.remove(fileName);
            }
        }

        for (String fileName : trackedByHead.keySet()) {
            File file = join(CWD, fileName);
            if (!file.exists() && !sa.containsKey(fileName)) {
                deleted.add(fileName + " (deleted)");
            }
        }

        System.out.println("=== Branches ===");
        System.out.println("*" + activeBranchName);
        for (String branchName : allBranchesNames) {
            if (!branchName.equals(activeBranchName)) {
                System.out.println(branchName);
            }
        }
        System.out.println();
        printStagingArea(sa);
        saveStagingArea(sa);
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String file : deleted) {
            System.out.println(file);
        }
        for (String file : modified) {
            System.out.println(file);
        }
        System.out.println("\n=== Untracked Files ===");
        for (String file : untrackedUnstaged) {
            System.out.println(file);
        }
        System.out.println();
    }
}
