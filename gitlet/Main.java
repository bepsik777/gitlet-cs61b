package gitlet;


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
                String fileName = args[1];
                if (fileName == null) {
                    System.out.println("pls provide a valid file path");
                    break;
                }
                Repository.add(fileName);
                break;
            case "print-index":
                Repository.printStagingArea();
                break;
        }
    }
}
