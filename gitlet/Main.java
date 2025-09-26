package gitlet;


import static gitlet.Utils.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author apotocki
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        String firstArg = args[0];
        if (firstArg == null) {
            System.out.println("Please provide an argument");
            return;
        }
        switch (firstArg) {
            case "init":
                init();
                break;
            case "add":
                add(args);
                break;
            case "rm":
                rm(args);
                break;
            case "commit":
                commit(args);
                break;
            case "checkout":
                checkout(args);
                break;
            case "branch":
                branch(args);
                break;
            case "rm-branch":
                removeBranch(args);
                break;
            case "reset":
                reset(args);
                break;
            case "merge":
                merge(args);
                break;
            case "log":
                log();
                break;
            case "global-log":
                globalLog();
                break;
            case "find":
                find(args);
                break;
            case "status":
                status();
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    private static void init() {
        Repository.init();
    }

    private static void add(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a valid file path");
        }
        String fileName = args[1];
        Repository.add(fileName);
    }

    private static void rm(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a valid file path");
            return;
        }
        String path = args[1];
        Repository.remove(path);
    }

    private static void commit(String[] args) {
        if (args.length == 1 || args[1].isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String message = args[1];
        Repository.commit(message);
    }

    private static void checkout(String[] args) {
        if (args.length == 1) {
            System.out.println("Please enter a commit message.");
            return;
        }
        if (args.length == 2) {
            String branchName = args[1];
            Repository.checkoutBranch(branchName);
            return;
        }
        if (args.length == 3 && args[1].equals("--")) {
            String checkedOutFile = args[2];
            Repository.basicCheckout(checkedOutFile);
            return;
        }
        if (args.length == 4) {
            String commitID = args[1];
            String operand = args[2];
            if (!operand.equals("--")) {
                System.out.println("Incorrect operands.");
            } else {
                String checkedOutFile = args[3];
                Repository.basicCheckout(checkedOutFile, commitID);
            }
        }
    }

    private static void branch(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a branch name");
            return;
        }
        String branchName = args[1];
        Repository.branch(branchName);
    }

    private static void removeBranch(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a branch name");
            return;
        }
        String branchToDelete = args[1];
        Repository.removeBranch(branchToDelete);
    }

    private static void reset(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a branch name");
            return;
        }
        String commitToReset = args[1];
        Repository.reset(commitToReset);
    }

    private static void merge(String[] args) {
        if (args.length == 1) {
            System.out.println("pls provide a branch name");
            return;
        }
        String branchToMerge = args[1];
        Repository.merge(branchToMerge);
    }

    private static void log() {
        Repository.log();
    }

    private static void globalLog() {
        Repository.globalLog();
    }

    private static void find(String[] args) {
        String msg = args[1];
        Repository.find(msg);
    }

    private static void status() {
        Repository.status();
    }
}
