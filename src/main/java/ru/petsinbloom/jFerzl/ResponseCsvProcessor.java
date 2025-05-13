package ru.petsinbloom.jFerzl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ResponseCsvProcessor {
    private static Consumer<String> output;

    public static void applyResponseData(Consumer<String> _output, Path responseCsvPath, List<Patient> patients) throws IOException {
        output = _output;

        try (BufferedReader reader = new BufferedReader(new FileReader(responseCsvPath.toFile()))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                // Split by semicolon, remove quotes
                String[] tokens = line.replace("\"", "").split(";", -1);

                if (tokens.length < 7) continue;

                long reqId = Long.parseLong(tokens[0].trim());
                String rid = tokens[1].trim();
                String enp = tokens[2].trim(); // если пустой, то на переработку!
                String oip = tokens[3].trim();
                String genderStr = tokens[6].trim();
                Byte gender = genderStr.isEmpty() ? null : Byte.parseByte(genderStr);

                // Find the patient with matching id
                for (Patient patient : patients) {
                    if (patient.getId() == reqId) {
                        patient.setRid(rid);
                        patient.setEnp(enp);
                        patient.setOip(oip);
                        patient.setGender(gender);
                        break;
                    }
                }
            }
        }
    }
}
