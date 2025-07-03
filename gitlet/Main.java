package gitlet;


import static gitlet.Utils.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author TODO
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                if (args.length == 1) {
                    System.out.println("pls provide a valid file path");
                    break;
                }
                String fileName = args[1];
                Repository.add(fileName);
                break;
            case "rm":
                if (args.length == 1) {
                    System.out.println("pls provide a valid file path");
                    break;
                }
                String path = args[1];
                Repository.remove(path);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("pls provide a commit message");
                    break;
                }
                String message = args[1];
                Repository.commit(message);
                break;
            case "checkout":
                if (args.length == 1) {
                    System.out.println("pls provide a commit message");
                    break;
                }
                if (args.length == 2) {
                    break;
                }
                if (args.length == 3 && args[1].equals("--")) {
                    String checkedOutFile = args[2];
                    Repository.basicCheckout(checkedOutFile);
                    break;
                }
                if (args.length == 4) {
                    String commitID = args[1];
                    String checkedOutFile = args[3];
                    Repository.basicCheckout(checkedOutFile, commitID);
                    break;
                }
            case "branch":
                if (args.length == 1) {
                    System.out.println("pls provide a branch name");
                    break;
                }
                String branchName = args[1];
                Repository.branch(branchName);
                break;
            case "log":
                Repository.log();
                break;
            case "print-head":
                printHeadCommit();
                break;
            case "print-index":
                printStagingArea();
                break;
        }
    }
}
