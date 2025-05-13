package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class IncomingAttachmentHandler {

    public static boolean handleAttachmentFile(
            Consumer<String> output,
            Path directory,
            String expectedMessageId,
            String outputFile,
            Duration timeout
    ) throws IOException {

        WatchService watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        // First, check existing files (if any already present)
        if (checkExistingFiles(directory, expectedMessageId, outputFile, output)) {
            watchService.close();
            return true;
        }

        Instant deadline = Instant.now().plus(timeout);
        output.accept("Ожидание файла с вложением для Message-Id = " + expectedMessageId);

        while (Instant.now().isBefore(deadline)) {
            WatchKey key;
            try {
                long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
                key = watchService.poll(millisLeft, TimeUnit.MILLISECONDS);
                if (key == null) {
                    output.accept("Время ожидания истекло. Вложение не найдено.");
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                output.accept("Ожидание прервано.");
                return false;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path filePath = directory.resolve(fileName);

                if (fileName.toString().startsWith("b") && Files.isRegularFile(filePath)) {
                    if (checkFile(filePath, directory, expectedMessageId, outputFile, output)) {
                        watchService.close();
                        return true;
                    }
                }
            }

            if (!key.reset()) {
                output.accept("Ключ наблюдателя недействителен.");
                return false;
            }
        }

        watchService.close();
        output.accept("Истекло время ожидания. Вложение не найдено.");
        return false;
    }

    private static boolean checkExistingFiles(Path directory, String expectedMessageId, String outputFile, Consumer<String> output) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> path.getFileName().toString().startsWith("b") && Files.isRegularFile(path))) {
            for (Path path : stream) {
                if (checkFile(path, directory, expectedMessageId, outputFile, output)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkFile(Path bFile, Path directory, String expectedMessageId, String outputFile, Consumer<String> output) {
        String foundResentId = null;
        String attachmentLine = null;

        int maxAttempts = 30;
        int waitMillis = 100;

        if (!waitForReadableFile(bFile, maxAttempts, waitMillis, output)) {
            output.accept("Файл не готов для чтения: " + bFile.getFileName());
            return false;
        }

        int readAttempts = 5;
        boolean success = false;
        for (int i = 0; i < readAttempts; i++) {
            try (BufferedReader reader = new BufferedReader(new FileReader(bFile.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Resent-Message-Id:")) {
                        foundResentId = line.substring("Resent-Message-Id:".length()).trim();
                    }
                    if (line.startsWith("Attachment:")) {
                        attachmentLine = line.substring("Attachment:".length()).trim();
                    }
                }
                success = true;
                break; // ✅ Successfully read
            } catch (IOException e) {
                output.accept("Попытка " + (i + 1) + " не удалась: " + e.getMessage());
                try {
                    Thread.sleep(100); // wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        if(!success)
            output.accept("Не удалось прочитать файл после нескольких попыток: " + bFile.getFileName());

        if (!Objects.equals(foundResentId, expectedMessageId)) {
            return false;
        }

//        try (BufferedReader reader = new BufferedReader(new FileReader(bFile.toFile()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Resent-Message-Id:")) {
//                    foundResentId = line.substring("Resent-Message-Id:".length()).trim();
//                }
//                if (line.startsWith("Attachment:")) {
//                    attachmentLine = line.substring("Attachment:".length()).trim();
//                }
//            }
//        } catch (IOException e) {
//            output.accept("Ошибка при чтении файла (IncomingAttachmentHandler): " + bFile.getFileName() + ": " + e.getMessage());
//            return false;
//        }


        if (attachmentLine != null) {
            String[] tokens = attachmentLine.split("\\s+");
            if (tokens.length >= 2) {
                String actualFileName = tokens[0]; // dA2ZOHQV.330
                String newFileName = tokens[1];    // d0117fc0fa0.zip

                Path sourceFile = directory.resolve(actualFileName);
                Path targetFile = directory.resolve(outputFile);

//                try {
//                    if (Files.exists(sourceFile)) {
//                        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
//                        output.accept("Attachment copied: " + actualFileName + " → " + outputFile + " (" + newFileName + ")");
//                        return true;
//                    } else {
//                        output.accept("Attachment file not found: " + actualFileName);
//                        return false;
//                    }
//                } catch (IOException e) {
//                    output.accept("Ошибка копирования: " + e.getMessage());
//                    return false;
//                }

                // Wait for the actual file to appear (up to 3 seconds)
                /// Метод пришлось переделать
                /// a classic race condition between the arrival of the metadata (b-file) and the actual attachment (d-file).
                /// Since the b-file appears first, your code tries to copy the d-file immediately — but it hasn't been created yet.
                int waitAttempts = 30;
                waitMillis = 100;

                for (int i = 0; i < waitAttempts; i++) {
                    if (Files.exists(sourceFile)) {
                        try {
                            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            output.accept("Attachment copied: " + actualFileName + " → " + outputFile + " (" + newFileName + ")");
                            return true;
                        } catch (IOException e) {
                            output.accept("Ошибка копирования файла: " + e.getMessage());
                            return false;
                        }
                    }

                    try {
                        Thread.sleep(waitMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        output.accept("Ожидание файла прервано.");
                        return false;
                    }
                }

                output.accept("Attachment file not found after waiting: " + actualFileName);
                return false;
            }
        }
        return false;
    }

    static boolean waitForReadableFile(Path file, int attempts, int millisPerAttempt, Consumer<String> output) {
        for (int i = 0; i < attempts; i++) {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    return true; // File is accessible and can be read
                } catch (IOException e) {
                    output.accept("Файл существует, но заблокирован или не завершен: " + file.getFileName());
                }
            }

            try {
                Thread.sleep(millisPerAttempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                output.accept("Ожидание файла прервано.");
                return false;
            }
        }

        output.accept("Файл не стал доступен для чтения после ожидания: " + file.getFileName());
        return false;
    }

}
