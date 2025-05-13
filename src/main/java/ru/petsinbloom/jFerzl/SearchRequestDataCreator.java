package ru.petsinbloom.jFerzl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SearchRequestDataCreator {

    public static void Create(List<Patient> records, Path outputCsv) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsv.toFile()))) {
            // Пишем заголовок. Порядок следования полей критичен!
            writer.write("REQ_ID;FAM;IM;OT;W;DR;SNILS;ENP;ERN;OIP;DUDL_CODE;DUDL_SER;DUDL_NUM;PCY_TYPE;PCY_SER;PCY_NUM;OMS_OKATO;CITIZEN_OKSM;DR_1;DR_2;DS_1;DS_2;DT;OLD_SFP");
            writer.newLine();

            for (Patient record : records) {
                if (record.getEnp() != null && !record.getEnp().isEmpty()) {
                    continue; // Если ЕНП определен, повторно не запрашиваем!
                }

                String line = String.format(
                        "%d;;;;;;%s;;;;;;;;;;;;;;;;;",
                        record.getId(),
                        //safe(record.getFam()),
                        //safe(record.getIm()),
                        //safe(record.getOt()),
                        //record.getDateOfBirth(),
                        safe(record.getSocialSecurityNumber())
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static String safe(String input) {
        return input == null ? "" : input;
    }
}
