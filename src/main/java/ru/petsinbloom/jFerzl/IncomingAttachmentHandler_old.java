package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.function.Consumer;

public class IncomingAttachmentHandler_old {
    private static Consumer<String> output;

    public static boolean handleAttachmentFile(Consumer<String> _output, Path directory, String expectedMessageId, String outputFile) throws IOException {
        output = _output;
        DirectoryStream<Path> stream = Files.newDirectoryStream(directory, path -> {
            String name = path.getFileName().toString();
            return name.startsWith("b") && Files.isRegularFile(path);
        });

        for (Path bFile : stream) {
            String foundResentId = null;
            String attachmentLine = null;

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
            }

            if (!Objects.equals(foundResentId, expectedMessageId)) {
                continue;
            }

            if (attachmentLine != null) {
                String[] tokens = attachmentLine.split("\\s+");
                if (tokens.length >= 2) {
                    String actualFileName = tokens[0]; // dA2ZOHQV.330
                    String newFileName = tokens[1]; // d0117fc0fa0.zip

                    Path sourceFile = directory.resolve(actualFileName);
                    // Path targetFile = directory.resolve(newFileName);
                    Path targetFile = directory.resolve(outputFile);


                    if (Files.exists(sourceFile)) {
                        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        output.accept("Attachment copied: " + actualFileName + " → " + outputFile + " (" + newFileName + ")");
                        return true;
                    } else {
                        output.accept("Attachment file not found: " + actualFileName);
                        return false;
                    }
                }
            }
        }

        output.accept("No matching attachment message found for Message-Id: " + expectedMessageId);
        return false;
    }
}
