package src;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.*;

public class Shell {
    // Список для хранения истории команд
    private static List<String> commandHistory = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Welcome to the Java Shell!");
        System.out.println("Available commands:");
        System.out.println("  echo <message>       - Outputs the given message.");
        System.out.println("  history              - Displays the history of commands.");
        System.out.println("  \\e $PATH            - Displays the PATH environment variable.");
        System.out.println("  \\l                  - List partitions on /dev/sda.");
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
                } else if (input.startsWith("\\l ")) {
                    handleDiskCheck(input);
                } else if (input.equals("\\cron")) {
                    listCronJobs(); // Вывод задач планировщика
                } else if (input.startsWith("\\mem ")) {
                    handleMemoryDump(input); // Дамп памяти процесса
                } else {
                    executeCommand(input);
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

    // 10. Проверка загрузочного диска
    private static void handleDiskCheck(String input) {
        String diskPath = input.substring(3).trim();  // Получаем путь к диску
        if (isBootableDisk(diskPath)) {
            System.out.println("Диск " + diskPath + " является загрузочным.");
        } else {
            System.out.println("Диск " + diskPath + " не является загрузочным.");
        }
    }
    public static boolean isBootableDisk(String diskPath) {
        // Открываем файл устройства
        try (RandomAccessFile diskFile = new RandomAccessFile(diskPath, "r")) {
            byte[] buffer = new byte[512]; // Размер одного сектора
            int bytesRead = diskFile.read(buffer);

            // Проверяем, что удалось прочитать 512 байт
            if (bytesRead != 512) {
                System.out.println("Не удалось прочитать полный сектор.");
                return false;
            }

            // Проверяем последние два байта на сигнатуру 0x55AA
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            byte lastByte = byteBuffer.get(510);  // 510-й байт (0x55)
            byte secondLastByte = byteBuffer.get(511); // 511-й байт (0xAA)

            return lastByte == (byte) 0x55 && secondLastByte == (byte) 0xAA;
        } catch (IOException e) {
            System.err.println("Ошибка при чтении диска: " + e.getMessage());
            return false;
        }
    }

    // 11. Подключение VFS в /tmp/vfs со списком задач в планировщике по команде \cron
    private static void listCronJobs() {
        File vfsFile = new File("/tmp/vfs");
        try (FileWriter writer = new FileWriter(vfsFile)) {
            File cronDir = new File("/var/spool/cron");
            if (cronDir.exists() && cronDir.isDirectory()) {
                for (File file : Objects.requireNonNull(cronDir.listFiles())) {
                    writer.write(file.getName() + "\n");
                }
                System.out.println("VFS для задач cron создан в /tmp/vfs.");
            } else {
                System.out.println("Директория для задач cron не найдена.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка создания VFS: " + e.getMessage());
        }
    }

    // 12. Получение дампа памяти процесса по команде \mem <procid>
    // Дамп памяти процесса
    private static void handleMemoryDump(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Использование: \\mem <procid>");
            return;
        }
        String procId = parts[1];
        createCoreDump(procId);
    }

    // Метод для создания дампа памяти с использованием gcore
    private static void createCoreDump(String pid) {
        try {
            // Запуск команды gcore для создания дампа памяти
            ProcessBuilder processBuilder = new ProcessBuilder("gcore", pid);
            processBuilder.inheritIO();  // Наследуем ввод/вывод для отображения результатов
            Process process = processBuilder.start();
            int exitCode = process.waitFor();  // Ожидаем завершения команды
            if (exitCode == 0) {
                System.out.println("Дамп памяти для процесса " + pid + " успешно создан.");
            } else {
                System.err.println("Ошибка при создании дампа памяти.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Выполнение внешней команды
    private static void executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка выполнения команды: " + e.getMessage());
        }
    }
}
