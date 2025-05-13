package ru.petsinbloom.jFerzl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RequestReader {

    public static List<Patient> readRequest(Path excelPath) throws IOException {
        List<Patient> records = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelPath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Skip header if needed
            boolean skipHeader = false;
            long idCounter = 1;

            for (Row row : sheet) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String caseNumber = getCellAsString(row.getCell(0));
                String fullName = getCellAsString(row.getCell(1));
                LocalDate dateOfBirth = getCellAsDate(row.getCell(2));
                String ssn = getCellAsString(row.getCell(3));

                // Split full name into parts
                String fam = "";
                String im = "";
                String ot = "";

                if (fullName != null && !fullName.isBlank()) {
                    String[] parts = fullName.trim().split("\\s+"); // split by spaces

                    if (parts.length >= 2) {
                        fam = parts[0];
                        im = parts[1];
                        if (parts.length >= 3) {
                            ot = parts[2];
                        }
                    } else {
                        // fallback, if something wrong, treat all as "fam"
                        fam = fullName.trim();
                    }
                }

                records.add(new Patient(idCounter++, caseNumber, capitalize(fam), capitalize(im), capitalize(ot), dateOfBirth, ssn));            }
        }

        return records;
    }

    private static String getCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private static LocalDate getCellAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                // If true date, convert normally
                return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } else {
                // It might be a numeric value that is not a date - handle separately if needed
                return null;
            }
        } else if (cell.getCellType() == CellType.STRING) {
            // If it's a text like "23.01.1981", try to parse it manually
            String dateText = cell.getStringCellValue().trim();
            if (!dateText.isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    return LocalDate.parse(dateText, formatter);
                } catch (Exception e) {
                    System.err.println("Failed to parse date string: " + dateText);
                    return null;
                }
            }
        }
        return null;
    }
    public static String capitalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        input = input.trim().toLowerCase(); // all lowercase
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

}
