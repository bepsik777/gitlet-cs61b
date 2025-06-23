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
            case "print-head":
                printHeadCommit();
                break;
            case "print-index":
                printStagingArea();
                break;
        }
    }
}
