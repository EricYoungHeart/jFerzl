package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KrepCsvProcessor {

    public static void applyKrepData(Path krepCsvPath, List<Patient> patients, Map<String, MedCompany> sprlpu) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(krepCsvPath.toFile()), "windows-1251"))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] tokens = line.replace("\"", "").split(";", -1);
                if (tokens.length < 20) {
                    System.out.println("Skipping line " + line);
                    continue;
                }

                long reqId = Long.parseLong(tokens[0].trim());
                int npp = Integer.parseInt(tokens[2].trim());
                String area = tokens[3].trim();
                String status = tokens[5].trim();

                // area != "1' - ошибка!!!
                // if(!area.equals("1")) continue;
                if (!"1".equals(area)) continue; // Null-safe

                if(Set.of("НАП", "НСУ", "НДВ", "НСМ","НПП", "НВП").contains(status)){
                    continue;
                }

                LocalDate dateB = parseDate(tokens[8]);
                LocalDate dateE = parseDate(tokens[9]);
                String moId = tokens[11].trim();
                String moCode = tokens[12].trim();
                String moFId = tokens[13].trim();
                String moOkato = tokens[18].trim();

                if(dateE!=null && dateE.isBefore(LocalDate.of(LocalDate.now().getYear(), 1, 1)))
                    continue;

                Krep krep = new Krep(dateB, dateE, moId, moCode, moFId, moOkato);
                krep.setStatus(status);
                krep.setArea(area);

                MedCompany mc = sprlpu.get(moId);
                if (mc != null) {
                    krep.setMoName(mc.getNamMop());
                }

                for (Patient p : patients) {
                    if (p.getId() == reqId) {
                        p.getKreps().add(krep);
                        break;
                    }
                }
            }
        }
    }

    private static LocalDate parseDate(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
