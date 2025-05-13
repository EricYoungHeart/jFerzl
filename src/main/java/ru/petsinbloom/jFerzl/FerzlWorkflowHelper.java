package ru.petsinbloom.jFerzl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class FerzlWorkflowHelper {
    private final Consumer<String> output; // default
    private final Path workingFolder;

    // Все в минутах!
    private final long waitForFilesToBePickedUp = 2;
    private final long waitForRequestAcceptance = 3;
    private final long waitForRequestReady = 10;

//    public static void setOutput(Consumer<String> out) {
//        this.output = out;
//    }
//    public static void setWorkingFolder(Path wrkFolder) {
//        workingFolder = wrkFolder;
//    }

    public FerzlWorkflowHelper(Path workingFolder, Consumer<String> output) {
        this.workingFolder = workingFolder;
        this.output = output;
    }
    // Подготовительные методы
    /**
     * Ensures that the working directory exists by attempting to create it.
     * <p>
     * If the directory already exists, no action is taken. If it does not exist,
     * it is created along with any necessary but nonexistent parent directories.
     * <p>
     * Success or failure is reported via the {@link WorkFlowResult} enum.
     *
     * @return {@code WorkFlowResult.SUCCESS} if the directory exists or was successfully created,
     *         {@code WorkFlowResult.FAILURE} if an I/O error occurred.
     */
    WorkFlowResult prepareWorkingFolder() {
        try {
            Files.createDirectories(workingFolder);
            output.accept("Рабочая директория: " + workingFolder + " .");
            return WorkFlowResult.SUCCESS;
        } catch (IOException e) {
            output.accept("Ошибка при создании рабочей директории: " + e.getMessage());
            return WorkFlowResult.FAILURE;
        }
    }
    /**
     * Validates the existence of required input directories: {@code aisomsFolder}, {@code inputFolder}, and {@code outputFolder}.
     * <p>
     * Unlike some other folders, these must exist prior to running the workflow and are not created automatically.
     * If any of them is missing, an error message is logged and the workflow fails.
     *
     * @return {@code WorkFlowResult.SUCCESS} if all required folders exist,
     *         {@code WorkFlowResult.FAILURE} if any is missing.
     */
    WorkFlowResult checkForAisomsFolderExistance(Path aisomsFolder) {
        if(!Files.exists(aisomsFolder) || !Files.isDirectory(aisomsFolder)) {
            output.accept("Директория "+aisomsFolder.toFile()+" не найдена!");
            return WorkFlowResult.FAILURE;
        }
        for (int i = 1; i <= 10; i++) {
            String userFolderName = String.format("USR%03d", i);
            Path userFolder = aisomsFolder.resolve(userFolderName);
            if (!Files.exists(userFolder) || !Files.isDirectory(userFolder)) {
                output.accept("Субдиректория " + userFolder + " не найдена!");
                return WorkFlowResult.FAILURE;
            }
        }
        return WorkFlowResult.SUCCESS;
    }
    /**
     * Checks whether the Excel workbook containing the initial request is present at the given path.
     * <p>
     * If the file is found, logs a success message. If not, logs an error referencing the expected filename.
     *
     * @param requestBook the path to the request workbook file
     * @return {@code WorkFlowResult.SUCCESS} if the file exists,
     *         {@code WorkFlowResult.FAILURE} if it does not
     */
    WorkFlowResult checkRequestWorkbookPresence(Path requestBook) {
        if(Files.exists(requestBook)){
            output.accept("Обнаружена книга запроса: " + requestBook+" .");
            return WorkFlowResult.SUCCESS;
        }
        else {
            output.accept("Книга с запросом " + requestBook.toFile() + " не найдена!");
            return WorkFlowResult.FAILURE;
        }
    }
    WorkFlowResult readSprlpuIfPresent(Path SPRLPU_CSV, Map<String, MedCompany> sprlpu, Path F003) {
        if (!Files.exists(SPRLPU_CSV)) readSprlpuFromExcelAndWriteCsv(SPRLPU_CSV, F003);
        return readSprlpuFromCsv(SPRLPU_CSV, sprlpu);
    }
    /**
     * Reads medical organization data from a CSV file and loads it into a {@code Map<String, MedCompany>},
     * where each entry is keyed by its medical organization code (MCOD).
     *
     * @param csvFile the path to the CSV file
     * @return {@code WorkFlowResult.SUCCESS} if the data was successfully read,
     *         {@code WorkFlowResult.FAILURE} if an I/O error occurred
     */
    private WorkFlowResult readSprlpuFromCsv(Path csvFile, Map<String, MedCompany> sprlpu) {
        try {
            List<MedCompany> medCompanies = SprLpu.readFromCsv(csvFile);
            // sprlpu.clear();
            sprlpu.putAll(medCompanies.stream()
                    .collect(Collectors.toMap(MedCompany::getMcod, mc -> mc)));
        } catch (IOException e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }

    /**
     * Parses medical organization data from an F003 XML file and writes the result to a CSV file.
     * <p>
     * If the F003 file does not exist, or if parsing fails, the workflow reports failure.
     *
     * @param csvFile the path where the CSV data should be saved
     * @return {@code WorkFlowResult.SUCCESS} if parsing and writing succeed,
     *         {@code WorkFlowResult.FAILURE} otherwise
     */
    private WorkFlowResult readSprlpuFromExcelAndWriteCsv(Path csvFile, Path F003) {
        if(Files.exists(F003)){
            try {
                F003Xml.parseAndSaveToCSV(F003.toFile(), csvFile, output);
                return WorkFlowResult.SUCCESS;
            } catch (Exception e) {
                output.accept("Ошибка обработки F003.xml: " + e.getMessage());
                return WorkFlowResult.FAILURE;
            }
        }
        else{
            output.accept("Не найден файл F003.xml!");
            return WorkFlowResult.FAILURE;
        }
    }
    // Подготовительные методы

    // Основные методы
    /**
     * Attempts to load patient data either from a cached CSV file or, if missing,
     * from the original Excel request workbook.
     * <p>
     * If the CSV file exists, data is loaded directly from it.
     * Otherwise, data is read from the Excel file and written to a new CSV file for future reuse.
     *
     * @return {@code WorkFlowResult.SUCCESS} if the data was successfully loaded,
     *         {@code WorkFlowResult.FAILURE} if an error occurred during reading or writing
     */
    WorkFlowResult readPatientsIfPresent(Path patientsCSV, List<Patient> patients, Path requestBook) {
        if (Files.exists(patientsCSV)) {
            return readPatientsFromCsv(patientsCSV, patients);
        } else {
            return readPatientsFromExcelAndWriteCsv(patientsCSV, patients, requestBook );
        }
    }
    /**
     * Loads patient data from the specified CSV file.
     *
     * @param csvFile the path to the CSV file containing patient data
     * @return {@code WorkFlowResult.SUCCESS} if the file was read successfully,
     *         {@code WorkFlowResult.FAILURE} if an I/O error occurred
     */
    private WorkFlowResult readPatientsFromCsv(Path csvFile, List<Patient> patients) {
        try {
            patients.clear();
            patients.addAll(PatientsReader.readFromCsv(csvFile));
            output.accept("Patients.csv десериализован: " + patients.size() + " записей.");
            return WorkFlowResult.SUCCESS;
        } catch (IOException e) {
            output.accept("Не удалось прочитать patients.csv! Попробуйте удалить его и создать заново.");
            return WorkFlowResult.FAILURE;
        }
    }
    /**
     * Loads patient data from the Excel request workbook and writes it to a CSV file.
     * <p>
     * This method is used as a fallback when the CSV file does not exist.
     *
     * @param csvFile the path where the CSV file will be written
     * @return {@code WorkFlowResult.SUCCESS} if the data was successfully read and written,
     *         {@code WorkFlowResult.FAILURE} if an error occurred during reading or writing
     */
    private WorkFlowResult readPatientsFromExcelAndWriteCsv(Path csvFile, List<Patient> patients, Path requestBook) {
        try {
            patients.clear();
            patients.addAll(RequestReader.readRequest(requestBook));
            output.accept("Считано записей: " + patients.size() + " из книги запроса.");
            PatientsWriter.writeToCsv(csvFile, patients);
            output.accept("Данные записаны в CSV: " + patients.size() + " записей.");
            return WorkFlowResult.SUCCESS;
        } catch (IOException e) {
            output.accept("Не удалось прочитать файл " + requestBook + "!");
            return WorkFlowResult.FAILURE;
        }
    }
    /**
     * Creates the FERZ PI search request data file (D-file) if it does not already exist.
     * <p>
     * The file is written in R3 CSV format using patient data. If the file already exists,
     * it is left unchanged and a success message is logged.
     *
     * @return {@code WorkFlowResult.SUCCESS} if the file was created or already exists,
     *         {@code WorkFlowResult.FAILURE} if an error occurred during file creation
     */
    WorkFlowResult createDataSearchRequest(List<Patient> patients, String FERZ_PI_SEARCH_DATA) {
        try {
            SearchRequestDataCreator.Create(patients, workingFolder.resolve(FERZ_PI_SEARCH_DATA));
            output.accept(FERZ_PI_SEARCH_DATA + " created successfully.");
            return WorkFlowResult.SUCCESS;
        } catch (Exception e) {
            output.accept("Не удалось создать " + FERZ_PI_SEARCH_DATA+" !");
            return WorkFlowResult.FAILURE;
        }
    }

    WorkFlowResult createPassportSearchRequest(String FERZ_PI_SEARCH_PASSPORT, String PI_SEARCH_ID, String FERZ_PI_SEARCH_DATA, String messageId) {
        try {
            PassportWriter.searchPassportWrite(workingFolder.resolve(FERZ_PI_SEARCH_PASSPORT), PI_SEARCH_ID, FERZ_PI_SEARCH_DATA, messageId);
            output.accept("File" +  FERZ_PI_SEARCH_PASSPORT + " created successfully.");
            return WorkFlowResult.SUCCESS;
        } catch (Exception e) {
            output.accept("Не удалось сформировать " + FERZ_PI_SEARCH_PASSPORT+" !");
            return WorkFlowResult.FAILURE;
        }
    }
    /**
     * Copies the generated FERZ PI search request files (data and passport)
     * from the working directory to the output directory.
     * <p>
     * If either copy operation fails, the workflow is aborted with {@code FAILURE}.
     *
     * @return {@code WorkFlowResult.SUCCESS} if both files were copied successfully,
     *         {@code WorkFlowResult.FAILURE} if an I/O error occurred
     */
    WorkFlowResult copySearchRequestToOutputFolder(Path outputFolder,
            String FERZ_PI_SEARCH_DATA, String FERZ_PI_SEARCH_DFile, String FERZ_PI_SEARCH_PASSPORT) {
        try {
            Files.copy(
                    workingFolder.resolve(FERZ_PI_SEARCH_DATA),
                    outputFolder.resolve(FERZ_PI_SEARCH_DFile)
            );
        } catch (IOException e) {
            output.accept("Ошибка при копировании D-файла: " + e.getMessage());
            return WorkFlowResult.FAILURE;
        }

        try {
            Files.copy(
                    workingFolder.resolve(FERZ_PI_SEARCH_PASSPORT),
                    outputFolder.resolve(FERZ_PI_SEARCH_PASSPORT),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            output.accept("Ошибка при копировании файла-паспорта: " + e.getMessage());
            return WorkFlowResult.FAILURE;
        }

        return WorkFlowResult.SUCCESS;

    }
    /**
     * Waits for specified files in the output directory to be removed, indicating that the mail system
     * or external process has picked them up for further handling.
     * <p>
     * If the files are not removed within the specified timeout, or if an I/O error occurs,
     * the workflow is considered to have failed.
     *
     * @return {@code WorkFlowResult.SUCCESS} if the files disappeared within the timeout,
     *         {@code WorkFlowResult.FAILURE} otherwise
     */
    WorkFlowResult waitForRequestToBePickedUp(Path outputFolder, String FERZ_PI_SEARCH_PASSPORT) {
        List<String> filesToWatch = List.of(FERZ_PI_SEARCH_PASSPORT);
        Duration timeout = Duration.ofMinutes(waitForFilesToBePickedUp); // переменная определена выше в классе

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                boolean success = waitForFilesToDisappear.Check(outputFolder, filesToWatch, output, timeout);
                if (success) {
                    return WorkFlowResult.SUCCESS;
                } else {
                    output.accept("Попытка " + attempt + ": файл(ы) не были удалены в течение таймаута.");
                }
            } catch (IOException e) {
                output.accept("Попытка " + attempt + ": ошибка при ожидании удаления файла: " + e.getMessage());
            }
        }

        // After 3 failed attempts
        output.accept("Удаление файлов не подтверждено после " + maxRetries + " попыток.");
        return WorkFlowResult.FAILURE;
    }

    /**
     * Waits for an incoming message confirming that the FERZ PI search request has been accepted and queued.
     * <p>
     * The method looks for a B-file with the specified {@code Message-ID} and a subject containing
     * the keyword {@code "ACCEPTED"} within a 2-minute timeout.
     *
     * @return {@code WorkFlowResult.SUCCESS} if the confirmation message is received,
     *         {@code WorkFlowResult.FAILURE} if not found or an error occurs
     */
    WorkFlowResult waitForSearchRequestAcceptance(String expectedMessageId, Path inputFolder) {
        try {
            boolean isRequestInQueue = IncomingMessageChecker.checkIncomingMessages(
                    output,
                    inputFolder,
                    expectedMessageId,
                    Duration.ofMinutes(waitForRequestAcceptance),
                    subject -> subject.contains("ACCEPTED")
            );
            if (!isRequestInQueue) {
                output.accept("Подтверждение постановки запроса в очередь не получено.");
                return WorkFlowResult.FAILURE;
            }
            return WorkFlowResult.SUCCESS;
        } catch (Exception e) {
            output.accept("Ошибка при проверке входящих сообщений: " + e.getMessage());
            return WorkFlowResult.FAILURE;
        }
    }

    WorkFlowResult waitForSearchRequestReady(Path inputFolder, String expectedMessageId, String zipResponse) {
        boolean isResponseready = false;
        try {
            isResponseready = IncomingAttachmentHandler.handleAttachmentFile(output, inputFolder, expectedMessageId, zipResponse,
                    Duration.ofMinutes(waitForRequestReady));
            if(!isResponseready)
                return WorkFlowResult.FAILURE;
        } catch (Exception e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult copySearchResponseFromInputToLocale(String zipSearchResponse, Path inputFolder) {
        if(Files.exists(inputFolder.resolve(zipSearchResponse))){
            try {
                Files.copy(inputFolder.resolve(zipSearchResponse), workingFolder.resolve(zipSearchResponse));
                Files.delete(inputFolder.resolve(zipSearchResponse));
                return WorkFlowResult.SUCCESS;
            } catch (IOException ex) {
                output.accept(ex.getMessage());
                return WorkFlowResult.FAILURE;
            }
        }
        return WorkFlowResult.FAILURE;
    }

    WorkFlowResult unzipSearchResponse(String zipSearchResponse, String unzipName) {
            try {
                ZipCsvExtractor.extractCsvFromZip(output, workingFolder.resolve(zipSearchResponse), workingFolder, unzipName);
            } catch (Exception e) {
                output.accept(e.getMessage());
                return WorkFlowResult.FAILURE;
            }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult applySearchResponseData(String FERZ_PI_SEARCH_RESPONSE, List<Patient> patients) {
        try {
            ResponseCsvProcessor.applyResponseData(output, workingFolder.resolve(FERZ_PI_SEARCH_RESPONSE), patients);
            output.accept("Результаты запроса FERZ_PI_SEARCH обработаны! ");
            // patients.forEach(output);
        } catch (Exception e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }


    WorkFlowResult createDataHistoryRequest(String FERZ_PI_HISTORY_DATA, List<Patient> patients) {
        // if(!Files.exists(workingFolder.resolve(FERZ_PI_HISTORY_DATA))){
        try {
            FinalCsvWriter.writeFinalFormat(workingFolder.resolve(FERZ_PI_HISTORY_DATA), patients);
            output.accept(FERZ_PI_HISTORY_DATA + " created successfully.");
        } catch (Exception e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        //}
        //else{
        //    output.accept(FERZ_PI_HISTORY_DATA + " previously existed.");
        //}
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult createPassportHistoryRequest(String FERZ_PI_HISTORY_PASSPORT, String PI_HISTORY_ID, String FERZ_PI_HISTORY_DATA) {
        //if(!Files.exists(workingFolder.resolve(FERZ_PI_HISTORY_PASSPORT))){
        try {
            PassportWriter.historyPassportWrite(workingFolder.resolve(FERZ_PI_HISTORY_PASSPORT), PI_HISTORY_ID, FERZ_PI_HISTORY_DATA);
            output.accept("File " +  FERZ_PI_HISTORY_PASSPORT + " created successfully.");
        } catch (Exception e) {
            output.accept("Не удалось сформировать " + FERZ_PI_HISTORY_PASSPORT+" !");
        }
        //}else{
        //    output.accept("Файл-паспорт запроса ferz_pi_history " + FERZ_PI_HISTORY_PASSPORT + " уже существует.");
        //}
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult copyHistoryRequestToOutputFolder(Path outputFolder,
                                      String FERZ_PI_HISTORY_DATA, String FERZ_PI_HISTORY_DFile, String FERZ_PI_HISTORY_PASSPORT) {
        try{
            Files.copy(workingFolder.resolve(FERZ_PI_HISTORY_DATA), outputFolder.resolve(FERZ_PI_HISTORY_DFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }

        try{
            Files.copy(workingFolder.resolve(FERZ_PI_HISTORY_PASSPORT), outputFolder.resolve(FERZ_PI_HISTORY_PASSPORT), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
   }

   WorkFlowResult waitFilesToBePickedUp(Path outputFolder,  String FERZ_PI_HISTORY_PASSPORT){
       List<String> filesToWatch = List.of(FERZ_PI_HISTORY_PASSPORT);
       Duration timeout = Duration.ofMinutes(2); // wait up to 1 minutes
       try {
           boolean success = waitForFilesToDisappear.Check(outputFolder, filesToWatch, output, timeout);
           if (!success) {
               return WorkFlowResult.FAILURE;
           }
       } catch (IOException e) {
           output.accept(e.getMessage());
           return WorkFlowResult.FAILURE;
       }
       return WorkFlowResult.SUCCESS;
   }

   // Если не пришло сообщение о постановке в очередь, попробуем продолжить работу...
   WorkFlowResult waitForHistoryRequestAcceptance(String PI_HISTORY_ID, Path inputFolder){
       String expectedMessageId = PI_HISTORY_ID + ".USR010@RUBY.MSK.OMS";
       Boolean isRequestInQueue = false;
       try {
           isRequestInQueue = IncomingMessageChecker.checkIncomingMessages(output, inputFolder, expectedMessageId,
                   Duration.ofMinutes(10), subject -> subject.contains("ACCEPTED"));
           if(!isRequestInQueue)
            //return WorkFlowResult.FAILURE;
            return WorkFlowResult.SUCCESS;
       } catch (Exception e) {
           output.accept(e.getMessage());
           //return WorkFlowResult.FAILURE;
           return WorkFlowResult.SUCCESS;
       }
       return WorkFlowResult.SUCCESS;
   }

   WorkFlowResult waitForHistoryRequestReady(Path inputFolder, String expectedMessageId){
       boolean isResponseready = false;
//       if(!Files.exists(inputFolder.resolve("pi_history_response.zip"))){
           try {
               isResponseready = IncomingAttachmentHandler.handleAttachmentFile(output, inputFolder, expectedMessageId, "pi_history_response.zip", Duration.ofMinutes(15));
               if(!isResponseready)
                   return WorkFlowResult.FAILURE;
           } catch (Exception e) {
               output.accept(e.getMessage());
               return WorkFlowResult.FAILURE;
           }
//              }
       return WorkFlowResult.SUCCESS;
   }

   WorkFlowResult copyHistoryResponseFromInputToLocale(Path inputFolder) {
//                if(!Files.exists(workingFolder.resolve("pi_history_response.zip"))){
            if(Files.exists(inputFolder.resolve("pi_history_response.zip"))){
                try {
                    Files.copy(inputFolder.resolve("pi_history_response.zip"), workingFolder.resolve("pi_history_response.zip"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    output.accept(ex.getMessage());
                    return WorkFlowResult.FAILURE;
                }
            }
//        }else{
//            output.accept("Файл pi_history_response.zip скопирован ранее.");
//        }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult unzipHistoryResponse() {
//                if(!Files.exists(workingFolder.resolve("pi_history_response.csv"))){
            try {
                MultiCsvExtractor.extractNamedCsvParts(output, workingFolder.resolve("pi_history_response.zip"), workingFolder);
            } catch (Exception e) {
                output.accept(e.getMessage());
                return WorkFlowResult.FAILURE;
            }
//        }else{
//            output.accept("pi_history_response.csv разархивирован ранее.");
//        }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult applyHistoryResonseData(List<Patient> patients) {
        try{
            InsuCsvProcessor.applyInsuranceData(workingFolder.resolve("insu.csv"), patients);
            output.accept("Обрабатываем файл insu.csv.");
        }catch (Exception e){
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult applyKrepData(List<Patient> patients, Map<String, MedCompany> sprlpu) {
        try{
            KrepCsvProcessor.applyKrepData(workingFolder.resolve("krep.csv"), patients, sprlpu);
            output.accept("Обрабатываем файл krep.csv.");
        } catch (IOException e) {
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }

    WorkFlowResult writeToExcel(List<Patient> patients) {
        try{
            //patients.sort((a, b) -> a.getCaseNumber().compareToIgnoreCase(b.getCaseNumber()));
            ExcelExporter.exportToExcel(workingFolder.resolve("response.xlsx"), patients);
        }catch (Exception e){
            output.accept(e.getMessage());
            return WorkFlowResult.FAILURE;
        }
        return WorkFlowResult.SUCCESS;
    }

}
