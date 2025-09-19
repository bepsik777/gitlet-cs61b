package gitlet;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static gitlet.Utils.*;

public class Refs {
    public static final File REF_DIR = join(Repository.GITLET_DIR, "refs");
    public static final File HEAD = join(Repository.GITLET_DIR, "HEAD");
    public static final File HEADS_DIR = join(REF_DIR, "heads");
    public static final File REMOTES_DIR = join(REF_DIR, "remotes");

    public static void init() {
        File masterBranch = join(HEADS_DIR, "master");
        try {
            REF_DIR.mkdir();
            HEADS_DIR.mkdir();
            REMOTES_DIR.mkdir();
            HEAD.createNewFile();
            masterBranch.createNewFile();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void switchBranch(String branchName) {
        File branchHead = join(HEADS_DIR, branchName);
        if (branchHead.exists()) {
            writeContents(HEAD, "ref: refs/heads/", branchName);
        } else {
            throw new GitletException("No branch with this name exist");
        }
    }

    public static void updateHead(String commit, String branchName) {
        File branchHead = join(HEADS_DIR, branchName);
        if (branchHead.exists()) {
            writeContents(branchHead, commit);
            writeContents(HEAD, "ref: refs/heads/", branchName);
        } else {
            System.out.println("No branch with this name exist");
        }
    }

    public static String getHeadCommitId() {
        String headLocation = readContentsAsString(HEAD).replaceFirst("ref: ", "");
        File head = join(Repository.GITLET_DIR, headLocation);
        return readContentsAsString(head);
    }

    public static String getHeadCommitId(String branchName) {
        return readContentsAsString(join(HEADS_DIR, branchName));
    }

    public static String getActiveBranch() {
        return readContentsAsString(HEAD).replaceFirst("ref: refs/heads/", "");
    }

    public static List<String> getAllBranchesNames() {
        return plainFilenamesIn(HEADS_DIR);
    }

    public static void createNewBranch(String branchName, String headCommitID) {
        File newBranch = join(HEADS_DIR, branchName);
        if (newBranch.exists()) {
            System.out.println("Branch with the given name already exist");
            return;
        }
        try {
            newBranch.createNewFile();
            writeContents(newBranch, headCommitID);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<Commit> getAllBranchesHeadsCommits() {
        List<String> filesInHeadsDir = plainFilenamesIn(HEADS_DIR);
        if (filesInHeadsDir == null) {
            System.out.println("no commits to log");
            return null;
        }
        return filesInHeadsDir.stream().map(fileName -> readContentsAsString(join(HEADS_DIR, fileName))).map(Utils::getCommitByShaHash).collect(Collectors.toList());
    }
}
