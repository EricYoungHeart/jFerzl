package ru.petsinbloom.jFerzl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;

public class ExcelExporter {

    public static void exportToExcel(Path outputPath, List<Patient> patients) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Patients");
        //sheet.setColumnWidth(col, 25 * 256);
        //sheet.setColumnWidth(col, 20 * 256);
        //sheet.setColumnWidth(col, 20 * 256);

        // Header row
        Row header = sheet.createRow(0);
        int col = 0;
        //header.createCell(col++).setCellValue("RID");
        sheet.setColumnWidth(col, 20 * 256);
        header.createCell(col++).setCellValue("История болезни");
        sheet.setColumnWidth(col, 25 * 256);
        header.createCell(col++).setCellValue("Фамилия");
        sheet.setColumnWidth(col, 20 * 256);
        header.createCell(col++).setCellValue("Имя");
        sheet.setColumnWidth(col, 20 * 256);
        header.createCell(col++).setCellValue("Отчество");
        header.createCell(col++).setCellValue("Пол");
        sheet.setColumnWidth(col, 10 * 256);
        header.createCell(col++).setCellValue("Дата рождения");
        sheet.setColumnWidth(col, 15 * 256);
        header.createCell(col++).setCellValue("СНИЛС");
        sheet.setColumnWidth(col, 18 * 256);
        header.createCell(col++).setCellValue("ЕНП");
        sheet.setColumnWidth(col, 10 * 256);
        header.createCell(col++).setCellValue("Дата начала действия полиса");
        sheet.setColumnWidth(col, 10 * 256);
        header.createCell(col++).setCellValue("Дата окончания действия полиса");
        header.createCell(col++).setCellValue("OKATO");
        sheet.setColumnWidth(col, 100 * 256);
        header.createCell(col++).setCellValue("Наименование СМО");
        header.createCell(col++).setCellValue("Код СМО");

        // Create enough columns for Kreps (let's assume up to 3 Kreps per patient)
        for (int i = 1; i <= 3; i++) {
            sheet.setColumnWidth(col, 5 * 256);
            header.createCell(col++).setCellValue("Статус прикрепления");
            sheet.setColumnWidth(col, 5 * 256);
            header.createCell(col++).setCellValue("Профиль прикрепления");
            sheet.setColumnWidth(col, 10 * 256);
            header.createCell(col++).setCellValue("МО прикрепления");
            sheet.setColumnWidth(col, 100 * 256);
            header.createCell(col++).setCellValue("Наименование МО прикрепления");
            sheet.setColumnWidth(col, 10 * 256);
            header.createCell(col++).setCellValue("Дата начала прикрепления");
            sheet.setColumnWidth(col, 10 * 256);
            header.createCell(col++).setCellValue("Дата окончания прикрепления");
        }

        // Date formatter
        //DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");


        // Fill data rows
        int rowNum = 1;
        for (Patient p : patients) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            //row.createCell(c++).setCellValue(p.getRid());
            row.createCell(c++).setCellValue(p.getCaseNumber());
            row.createCell(c++).setCellValue(p.getFam());
            row.createCell(c++).setCellValue(p.getIm());
            row.createCell(c++).setCellValue(p.getOt());
            row.createCell(c++).setCellValue(p.getGender() != null ? (p.getGender()==1?"муж":"жен") : "");
            row.createCell(c++).setCellValue(p.getDateOfBirth() != null ? p.getDateOfBirth().format(df) : "");
            row.createCell(c++).setCellValue(p.getSocialSecurityNumber());
            row.createCell(c++).setCellValue(p.getEnp());
            row.createCell(c++).setCellValue(p.getDateB() != null ? p.getDateB().format(df) : "");
            row.createCell(c++).setCellValue(p.getDateE() != null ? p.getDateE().format(df) : "");
            row.createCell(c++).setCellValue(p.getOkato());
            row.createCell(c++).setCellValue(p.getInsurName());
            row.createCell(c++).setCellValue(p.getInsurCode());

            // Add up to 3 Krep rows in flat format
            List<Krep> kreps = p.getKreps();
            kreps.sort(Comparator.comparing(Krep::getStatus));
            for (int i = 0; i < Math.min(kreps.size(), 3); i++) {
                Krep k = kreps.get(i);

                row.createCell(c++).setCellValue(k.getStatus());
                row.createCell(c++).setCellValue(k.getArea());
                row.createCell(c++).setCellValue(k.getMoId());
                row.createCell(c++).setCellValue(k.getMoName());
                row.createCell(c++).setCellValue(k.getDateB() != null ? k.getDateB().format(df) : "");
                row.createCell(c++).setCellValue(k.getDateE() != null ? k.getDateE().format(df) : "");
            }
        }

        // Autosize columns
        for (int i = 0; i < col; i++) {
        //    sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            workbook.write(out);
        }

        workbook.close();
        System.out.println("Excel file created: " + outputPath);
    }
}
