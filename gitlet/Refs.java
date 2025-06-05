package gitlet;

import java.io.File;

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

    public static void updateHead(String commit, String branch) {
        File branchHead = join(HEADS_DIR, branch);
        if (branchHead.exists()) {
            writeContents(branchHead, commit);
            writeContents(HEAD, "ref: refs/heads/", branch);
        } else {
            System.out.println("No branch with this name exit");
        }
    }

    public static String getHeadCommitId() {
        String headLocation = readContentsAsString(HEAD).replaceFirst("ref: ", "");
        File head = join(Repository.GITLET_DIR, headLocation);
        return readContentsAsString(head);
    }

    public static String getActiveBranch() {
        return readContentsAsString(HEAD).replaceFirst("ref: ", "").replaceFirst(HEADS_DIR.toString(), "");
    }
}
