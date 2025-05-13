package ru.petsinbloom.jFerzl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class PassportWriter {
    private static final String messageFinalPart = ".ERIC@YOUNG.MSK.OMS";

    // ferz_pi_search
    public static void searchPassportWrite(Path outputPath, String code, String attachmentFileName, String messageId) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
            writer.write("To: erz@mgf.msk.oms");
            writer.newLine();
            writer.write("Message-Id: " + messageId);
            writer.newLine();
            writer.write("Subject: FERZ_PI_SEARCH");
            writer.newLine();
            writer.write("Attachment: d" + code + " " + attachmentFileName);
            writer.newLine();
        }
    }

    // ferz_pi_history
    public static void historyPassportWrite(Path outputPath, String code, String attachmentFileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
            writer.write("To: erz@mgf.msk.oms");
            writer.newLine();
            writer.write("Message-Id: " + code + messageFinalPart);
            writer.newLine();
            writer.write("Subject: FERZ_PI_HISTORY");
            writer.newLine();
            writer.write("Attachment: d" + code + " " + attachmentFileName);
            writer.newLine();
        }
    }
}
