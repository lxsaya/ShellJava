import java.util.Scanner;
public class Shell {
    public static void main(String[] args) {
        System.out.println("Welcome to the Java Shell! Type a command or exit with Ctrl+D, 'exit', or '\\q'.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> "); // Prompt for the shell

                // Read input
                if (!scanner.hasNextLine()) {
                    // Ctrl+D sends EOF, which is handled here
                    System.out.println("\nExiting...");
                    break;
                }

                String input = scanner.nextLine().trim();

                // Exit commands
                if (input.equalsIgnoreCase("exit") || input.equals("\\q")) {
                    System.out.println("Exiting...");
                    break;
                }

                // Print the input
                System.out.println("You entered: " + input);
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
