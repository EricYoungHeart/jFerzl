package ru.petsinbloom.jFerzl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class IncomingMessageChecker_old {
    private static Consumer<String> output;

    public static boolean checkIncomingMessages(Consumer<String> output, Path directory, String expectedMessageId, Duration timeout) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        Instant deadline = Instant.now().plus(timeout);
        output.accept("Ожидание файла с Resent-Message-Id = " + expectedMessageId);

        // Also check existing files immediately
        if (checkExistingFiles(directory, expectedMessageId, output)) {
            watchService.close();
            return true;
        }

        while (Instant.now().isBefore(deadline)) {
            WatchKey key;
            try {
                long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
                key = watchService.poll(millisLeft, TimeUnit.MILLISECONDS);
                if (key == null) {
                    output.accept("Время ожидания истекло. Сообщение не найдено.");
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
                    if (checkFile(filePath, expectedMessageId, output)) {
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
        output.accept("Истекло время ожидания. Сообщение не найдено.");
        return false;
    }

    private static boolean checkExistingFiles(Path directory, String expectedMessageId, Consumer<String> output) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> path.getFileName().toString().startsWith("b") && Files.isRegularFile(path))) {
            for (Path path : stream) {
                if (checkFile(path, expectedMessageId, output)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkFile(Path path, String expectedMessageId, Consumer<String> output) {
        String foundResentMessageId = null;
        String foundSubject = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Resent-Message-Id:")) {
                    foundResentMessageId = line.substring("Resent-Message-Id:".length()).trim();
                }
                if (line.startsWith("Subject:") && line.contains("ACCEPTED")) {
                    foundSubject = line.substring("Subject:".length()).trim();
                }
            }
        } catch (IOException e) {
            output.accept("Ошибка при чтении файла: " + path.getFileName() + ": " + e.getMessage());
            return false;
        }

        if (Objects.equals(foundResentMessageId, expectedMessageId) && foundSubject != null) {
            output.accept("Запрос поставлен в очередь: " + path.getFileName() + ", " + foundSubject);
            return true;
        }

        return false;
    }

    public static boolean checkIncomingMessages_oldVersion(Consumer<String> _output, Path directory, String expectedMessageId) throws IOException {
        output = _output;
        DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> {
            String filename = path.getFileName().toString();
            return filename.startsWith("b") && Files.isRegularFile(path);
        });

        for (Path path : stream) {
            String foundResentMessageId = null;
            String foundSubject = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Resent-Message-Id:")) {
                        foundResentMessageId = line.substring("Resent-Message-Id:".length()).trim();
                    }
                    if (line.startsWith("Subject:") && line.contains("ACCEPTED"))  {
                        foundSubject = line.substring("Subject:".length()).trim();
                    }
                }
            }

            if (Objects.equals(foundResentMessageId, expectedMessageId) && foundSubject != null) {
                output.accept("Запрос поставлен в очередь: " + path.getFileName() + ", " + foundSubject);
                // output.accept("Matching file found: " + path.getFileName());
                // output.accept("Subject: " + foundSubject);
                return true;
            }
        }

        output.accept("No matching message found for Message-Id: " + expectedMessageId);
        return false;
    }

    public static boolean checkIncomingMessages2(Consumer<String> _output, Path directory, String expectedMessageId) throws IOException {
        output = _output;
        DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> {
            String filename = path.getFileName().toString();
            return filename.startsWith("b") && Files.isRegularFile(path);
        });

        for (Path path : stream) {
            String foundResentMessageId = null;
            String foundSubject = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Resent-Message-Id:")) {
                        foundResentMessageId = line.substring("Resent-Message-Id:".length()).trim();
                    }
                    if (line.startsWith("Subject:") && !line.contains("ACCEPTED"))  {
                        int len = "Subject:".length();
                        foundSubject = line.substring("Subject:".length()).trim();
                    }
                }
            }

            if (Objects.equals(foundResentMessageId, expectedMessageId) && foundSubject != null) {
                output.accept("Запрос поставлен в очередь: " + path.getFileName() + ", " + foundSubject);
                //output.accept("Matching file found: " + path.getFileName());
                //output.accept("Subject: " + foundSubject);
                return true;
            }
        }

        output.accept("No matching message found for Message-Id: " + expectedMessageId);
        return false;
    }

}
