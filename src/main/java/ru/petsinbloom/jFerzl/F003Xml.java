package ru.petsinbloom.jFerzl;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class F003Xml {

    public static void parseAndSaveToCSV(File xmlFile, Path csvOutput, Consumer<String> output) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        BufferedWriter writer = Files.newBufferedWriter(csvOutput, StandardCharsets.UTF_8);
        writer.write("mcod;nam_mop;nam_mok");
        writer.newLine();

        parser.parse(xmlFile, new DefaultHandler() {
            private StringBuilder content = new StringBuilder();
            private String mcod, namMop, namMok;
            private boolean insideMedCompany = false;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                content.setLength(0);
                if (qName.equals("medCompany")) {
                    insideMedCompany = true;
                    mcod = namMop = namMok = "";
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                content.append(ch, start, length);
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if (!insideMedCompany) return;

                switch (qName) {
                    case "mcod" -> mcod = content.toString().trim();
                    case "nam_mop" -> namMop = content.toString().trim();
                    case "nam_mok" -> namMok = content.toString().trim();
                    case "medCompany" -> {
                        try {
                            writer.write(String.join(";", mcod, namMop, namMok));
                            writer.newLine();
                        } catch (IOException e) {
                            output.accept("Ошибка записи CSV: " + e.getMessage());
                        }
                        insideMedCompany = false;
                    }
                }
            }
        });

        writer.close();
        output.accept("F003.xml обработан, CSV создан: " + csvOutput);
    }
}
