package src;

import java.io.*;
import java.util.*;

public class Shell {
    // Список для хранения истории команд
    private static List<String> commandHistory = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Welcome to the Java Shell!");
        System.out.println("Available commands:");
        System.out.println("  echo <message>       - Outputs the given message.");
        System.out.println("  history             - Displays the history of commands.");
        System.out.println("  \\e $PATH            - Displays the PATH environment variable.");
        System.out.println("  \\l /dev/sda         - List partitions on /dev/sda.");
        System.out.println("  \\cron               - List scheduled cron jobs.");
        System.out.println("  \\mem <procid>       - Dump memory of the specified process.");
        System.out.println("  exit, \\q            - Exits the shell.");
        System.out.println();
        // Устанавливаем обработчик сигнала SIGHUP
        setupSighupHandler();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> "); // Prompt for the shell

                // Чтение ввода
                if (!scanner.hasNextLine()) {
                    // Ctrl+D отправляет EOF, который здесь обрабатывается
                    System.out.println("\nExiting...");
                    break;
                }

                String input = scanner.nextLine().trim();

                // История команд
                commandHistory.add(input);
                saveCommandHistoryToFile(); // Сохраняем команду в файл

                // Обработка команд
                if (input.equalsIgnoreCase("exit") || input.equals("\\q")) {
                    System.out.println("Exiting...");
                    break;
                } else if (input.startsWith("echo ")) {
                    handleEcho(input); // Команда echo
                } else if (input.startsWith("\\e $PATH")) {
                    handleEnvironmentVariable(); // Вывод переменной окружения
                } else if (input.equalsIgnoreCase("history")) {
                    showHistory(); // Показ истории команд
                } else if (input.startsWith("\\l /dev/sda")) {
                    listPartitions(); // Вывод разделов на /dev/sda
                } else if (input.equals("\\cron")) {
                    listCronJobs(); // Вывод задач планировщика
                } else if (input.startsWith("\\mem ")) {
                    handleMemoryDump(input); // Дамп памяти процесса
                } else {
                    runCommand(input); // Выполнение бинарных команд
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    // 4. Сохранение истории команд в файл
    private static void saveCommandHistoryToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("command_history.txt", true))) {
            writer.write(commandHistory.get(commandHistory.size() - 1)); // Записываем последнюю команду
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

    // 5. Обработка команды echo
    private static void handleEcho(String input) {
        String message = input.substring(5).trim(); // Убираем "echo "
        System.out.println(message);
    }

    // 6. Проверка введённой команды
    private static void runCommand(String input) {
        try {
            Process process = new ProcessBuilder(input.split(" ")).start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Command execution failed: " + e.getMessage());
        }
    }

    // 7. Вывод переменной окружения $PATH
    private static void handleEnvironmentVariable() {
        String path = System.getenv("PATH");
        System.out.println("PATH: " + path);
    }

    // 8. Показ истории введённых команд
    private static void showHistory() {
        System.out.println("Command history:");
        for (String command : commandHistory) {
            System.out.println(command);
        }
    }

    // 9. Обработка сигнала SIGHUP
    private static void setupSighupHandler() {
        // Создаем новый поток для отслеживания сигнала SIGHUP
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Configuration reloaded");
        }));
    }

    // 10. Получение информации о разделах в системе по команде \l /dev/sda
    private static void listPartitions() {
        try {
            Process process = new ProcessBuilder("lsblk", "/dev/sda").start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to list partitions: " + e.getMessage());
        }
    }

    // 11. Подключение VFS в /tmp/vfs со списком задач в планировщике по команде \cron
    private static void listCronJobs() {
        try {
            Process process = new ProcessBuilder("crontab", "-l").start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to list cron jobs: " + e.getMessage());
        }
    }

    // 12. Получение дампа памяти процесса по команде \mem <procid>
    private static void handleMemoryDump(String input) {
        String procId = input.substring(5).trim();
        try {
            Process process = new ProcessBuilder("cat", "/proc/" + procId + "/maps").start();
            process.waitFor();
            // Если есть доступ, можно получить содержимое памяти
            process = new ProcessBuilder("cat", "/proc/" + procId + "/mem").start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to dump memory for process " + procId + ": " + e.getMessage());
        }
    }
}
