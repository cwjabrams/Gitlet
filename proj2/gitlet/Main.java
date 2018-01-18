package gitlet;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String command = args[0];
        if (!_commands.contains(command)) {
            System.out.println("No command with that name exists.");
            System.exit(0);
        } else if (command.equals("init")) {
            if (args.length != 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            } else {
                try {
                    new Gitlet().init();
                } catch (GitletException e) {
                    System.out.print(e.getMessage());
                }
            }
        } else {
            File gitlet = (new File(".gitlet"));
            if (!gitlet.exists()) {
                System.out.println("Not in an initialized gitlet directory.");
                return;
            } else {
                commandHelper(args);
            }
        }
    }


    /** Calls the corresponding Gitlet method for <COMMAND> in args.
     *  Prints an error message if the wrong number or type of operands are given.
     * @param args: A String array containing <COMMAND> <OPERAND> ....
     */
    public static void commandHelper(String[] args) {
        String command = args[0];
        int numOperands = args.length - 1;
        if (numOperands == 0) {
            switch (command) {
                case "log":
                    Gitlet.log();
                    break;
                case "global-log":
                    Gitlet.globalLog();
                    break;
                case "status":
                    Gitlet.status();
                    break;
                default:
                    System.out.print("Incorrect operands.");
                    System.exit(0);
            }
        } else if (numOperands == 1) {
            String operand = args[1];
            if (operand == null) {
                System.out.print("Incorrect operands.");
                System.exit(0);
            }
            if (_oneArgCommands.keySet().contains(command)) {
                try {
                    _oneArgCommands.get(command).doCommand(operand);
                } catch (GitletException e) {
                    System.out.print(e.getMessage());
                }
            } else {
                System.out.print("Incorrect operands.");
                System.exit(0);
            }
        } else if (numOperands == 2) {
            String operand = args[2];
            if (command.equals("checkout") && args[1].equals("--")) {
                try {
                    Gitlet.checkoutFile(operand);
                } catch (GitletException e) {
                    System.out.print(e.getMessage());
                }
            } else {
                System.out.print("Incorrect operands.");
                System.exit(0);
            }
        } else if (numOperands == 3) {
            String commitID = args[1];
            String fileName = args[3];
            if (args[0].equals("checkout") && args[2].equals("--")) {
                try {
                    Gitlet.checkoutFileFromCommit(commitID, fileName);
                } catch (GitletException e) {
                    System.out.print(e.getMessage());
                }
            } else {
                System.out.print("Incorrect operands.");
                System.exit(0);
            }
        }
    }


    /** A mapping of String commands to Gitlet command operations that take in one argument. */
    static HashMap<String, OneArgCommand> _oneArgCommands = new HashMap<String, OneArgCommand>() { {
            put("add", Gitlet::add); put("rm", Gitlet::remove); put("branch", Gitlet::branch);
            put("rm-branch", Gitlet::rmBranch); put("commit", Gitlet::commit);
            put("find", Gitlet::find); put("checkout", Gitlet::checkoutBranch);
            put("merge", Gitlet::merge); put("reset", Gitlet::reset);
        }
    };

    /** A functional interface used by a single-argument command.*/
    interface OneArgCommand {
        void doCommand(String a);
    }


    /** A list of the valid commands. */
    private static List<String> _commands = Arrays.asList("log", "global-log", "status", "init",
            "add", "rm", "branch", "rm-branch", "commit", "find", "checkout", "merge", "reset");

}
