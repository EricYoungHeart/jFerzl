package ru.petsinbloom.jFerzl;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class waitForFilesToDisappear {

    public static boolean Check(Path folder, List<String> targetFilenames, Consumer<String> output, Duration timeout) throws IOException {
        Set<String> waitingFor = new HashSet<>();

        // Step 1: Check what files still exist
        for (String filename : targetFilenames) {
            if (Files.exists(folder.resolve(filename))) {
                waitingFor.add(filename);
            } else {
                output.accept("Файл уже отправлен: " + filename);
            }
        }

        if (waitingFor.isEmpty()) {
            output.accept("Все указанные файлы уже отправлены.");
            return true;
        }

        // output.accept("Ожидаем отправки файлов: " + waitingFor);

        // Step 2: Register watcher AFTER checking current state
        WatchService watchService = FileSystems.getDefault().newWatchService();
        folder.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);

        Instant deadline = Instant.now().plus(timeout);

        while (!waitingFor.isEmpty() && Instant.now().isBefore(deadline)) {
            WatchKey key;
            try {
                long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
                key = watchService.poll(millisLeft, TimeUnit.MILLISECONDS);
                if (key == null) {
                    output.accept("Время ожидания истекло. Файлы остались: " + waitingFor);
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

                // Just in case — confirm the file is truly gone
                if (!Files.exists(folder.resolve(fileName)) && waitingFor.remove(fileName.toString())) {
                    output.accept("Файл исчез: " + fileName);
                }
            }

            if (!key.reset()) {
                output.accept("Ключ наблюдателя недействителен.");
                return false;
            }

            // 🛠️ Safety net: in case watcher missed anything, recheck manually
            waitingFor.removeIf(filename -> !Files.exists(folder.resolve(filename)));
        }

        watchService.close();

        if (waitingFor.isEmpty()) {
            // output.accept("Все указанные файлы отправлены.");
            return true;
        } else {
            output.accept("Истекло время ожидания. Файлы остались: " + waitingFor);
            return false;
        }
    }


//    public static boolean Check(Path folder, List<String> targetFilenames, Consumer<String> output, Duration timeout) throws IOException {
//        // Step 1: check right now if all files are already gone
//        // Set<String> waitingFor = new HashSet<>(targetFilenames);
//        Set<String> waitingFor = new HashSet<>();
//        for (String filename : targetFilenames) {
//            if (Files.exists(folder.resolve(filename))) {
//                waitingFor.add(filename);
//            } else {
//                output.accept("Файл уже отправлен: " + filename);
//            }
//        }
//        if (waitingFor.isEmpty()) {
//            output.accept("Все указанные файлы уже отправлены.");
//            return true;
//        }
//
//        WatchService watchService = FileSystems.getDefault().newWatchService();
//        folder.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
//        output.accept("Ожидаем удаление файлов: " + waitingFor);
//
//        for (String filename : targetFilenames) {
//            if (Files.exists(folder.resolve(filename))) {
//                waitingFor.add(filename);
//            } else {
//                output.accept("Файл уже отправлен: " + filename);
//            }
//        }
//
//        if (waitingFor.isEmpty()) {
//            output.accept("Все указанные файлы уже отправлены.");
//            return true;
//        }
//
//        output.accept("Ожидаем отправки файлов: " + waitingFor);
//
//        Instant deadline = Instant.now().plus(timeout);
//
//        while (!waitingFor.isEmpty() && Instant.now().isBefore(deadline)) {
//            WatchKey key;
//            try {
//                long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
//                key = watchService.poll(millisLeft, TimeUnit.MILLISECONDS); // wait with timeout
//                if (key == null) {
//                    // Timeout expired
//                    output.accept("Время ожидания истекло. Оставшиеся файлы: " + waitingFor);
//                    return false;
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                output.accept("Ожидание прервано.");
//                return false;
//            }
//
//            for (WatchEvent<?> event : key.pollEvents()) {
//                WatchEvent<Path> ev = (WatchEvent<Path>) event;
//                Path fileName = ev.context();
//                if (waitingFor.remove(fileName.toString())) {
//                    output.accept("Файл исчез: " + fileName);
//                }
//            }
//
//            if (!key.reset()) {
//                output.accept("Ключ наблюдателя недействителен.");
//                return false;
//            }
//        }
//
//        watchService.close();
//
//        if (waitingFor.isEmpty()) {
//            output.accept("Все указанные файлы отправлены.");
//            return true;
//        } else {
//            output.accept("Истекло время ожидания. Файлы остались: " + waitingFor);
//            return false;
//        }
//    }

}
