package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.time.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.TimeUnit;

public class IncomingMessageChecker {

    public static boolean checkIncomingMessages(
            Consumer<String> output,
            Path directory,
            String expectedMessageId,
            Duration timeout,
            Predicate<String> subjectMatcher
    ) throws IOException {

        WatchService watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        Instant deadline = Instant.now().plus(timeout);
        output.accept("Ожидание файла с Resent-Message-Id = " + expectedMessageId);

        if (checkExistingFiles(directory, expectedMessageId, subjectMatcher, output)) {
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
                    if (checkFile(filePath, expectedMessageId, subjectMatcher, output)) {
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

    private static boolean checkExistingFiles(Path directory, String expectedMessageId, Predicate<String> subjectMatcher, Consumer<String> output) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> path.getFileName().toString().startsWith("b") && Files.isRegularFile(path))) {
            for (Path path : stream) {
                if (checkFile(path, expectedMessageId, subjectMatcher, output)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkFile(Path path, String expectedMessageId, Predicate<String> subjectMatcher, Consumer<String> output) {
        final int maxAttempts = 30;
        final int waitMillis = 100;

        if (!waitForReadableFile(path, maxAttempts, waitMillis, output)) {
            output.accept("Файл не готов для чтения: " + path.getFileName());
            return false;
        }

        String foundResentMessageId = null;
        String foundSubject = null;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Resent-Message-Id:")) {
                    foundResentMessageId = line.substring("Resent-Message-Id:".length()).trim();
                }
                if (line.startsWith("Subject:")) {
                    String subject = line.substring("Subject:".length()).trim();
                    if (subjectMatcher.test(subject)) {
                        foundSubject = subject;
                    }
                }
            }
        } catch (IOException e) {
            output.accept("Ошибка при чтении файла (IncomingMessageChecker): " + path.getFileName() + ": " + e.getMessage());
            return false;
        }

        if (Objects.equals(foundResentMessageId, expectedMessageId) && foundSubject != null) {
            output.accept("Запрос поставлен в очередь: " + path.getFileName() + ", " + foundSubject);
            return true;
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

//    private static boolean checkFile(Path path, String expectedMessageId, Predicate<String> subjectMatcher, Consumer<String> output) {
//        String foundResentMessageId = null;
//        String foundSubject = null;
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Resent-Message-Id:")) {
//                    foundResentMessageId = line.substring("Resent-Message-Id:".length()).trim();
//                }
//                if (line.startsWith("Subject:")) {
//                    String subject = line.substring("Subject:".length()).trim();
//                    if (subjectMatcher.test(subject)) {
//                        foundSubject = subject;
//                    }
//                }
//            }
//        } catch (IOException e) {
//            output.accept("Ошибка при чтении файла: " + path.getFileName() + ": " + e.getMessage());
//            return false;
//        }
//
//        if (Objects.equals(foundResentMessageId, expectedMessageId) && foundSubject != null) {
//            output.accept("Запрос поставлен в очередь: " + path.getFileName() + ", " + foundSubject);
//            return true;
//        }
//
//        return false;
//    }
}
