package ru.petsinbloom.jFerzl;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// import static ru.petsinbloom.jFerzl.Main.config;

class FerzlWorkflow {
    // private static AppConfig config;
    private final Path aisomsFolder;
    private final Path workingFolder;
    private final Path commonFolder;

    private Consumer<String> output;

    private Path requestBook;

    private static final String REQUEST_WORKBOOK_FILENAME = "request.xlsx";
    private static final String PATIENTS_CSV = "patients.csv";

    private static Path F003;
    private static Path SPRLPU_CSV;

    private List<Patient> patients=new ArrayList<>();

    private Map<String, MedCompany> sprlpu = new HashMap<>();

    FerzlWorkflowHelper helper;

    //static {
    //    config = AppConfig.INSTANCE;
    // }

    public FerzlWorkflow(Consumer<String> output) {
        workingFolder = Path.of(Main.config.getConfig("workingFolder"));
        commonFolder = Path.of(Main.config.getConfig("commonFolder"));
        aisomsFolder = Path.of(Main.config.getConfig("aisomsFolder"));

        F003 = commonFolder.resolve("F003.xml");
        SPRLPU_CSV = commonFolder.resolve("sprlpu.csv");

        this.output = output;

        helper = new FerzlWorkflowHelper(workingFolder, output);
        //FerzlWorkflowHelper.setOutput(output);
        //FerzlWorkflowHelper.setWorkingFolder(workingFolder);
    }

    public WorkFlowResult runWorkFlow(){
        /// Проверяем наличие рабочей директории, создаем, если ее нет
        if (failed(helper.prepareWorkingFolder()))
            return WorkFlowResult.FAILURE;

        // Проверяем наличие директории АИСОМС
        if(failed(helper.checkForAisomsFolderExistance(aisomsFolder)))
            return WorkFlowResult.FAILURE;

        /// Проверяем наличие книги с запросом. Если её нет, прекращаем работу
        requestBook = workingFolder.resolve(REQUEST_WORKBOOK_FILENAME);
        if(failed(helper.checkRequestWorkbookPresence(requestBook)))
            return WorkFlowResult.FAILURE;

        /// Читаем федеральный справочник МО и пишем в csv
        if(failed(helper.readSprlpuIfPresent(SPRLPU_CSV, sprlpu, F003)))
            return WorkFlowResult.FAILURE;

        /// Преобразуем данные 1-го листа книги с запросом в файл PATIENTS_CSV. Читаем его, если он создан ранее
        if(failed(helper.readPatientsIfPresent(workingFolder.resolve(PATIENTS_CSV), patients, requestBook)))
            return WorkFlowResult.FAILURE;

        // Делим на пакеты - пока пробуем по 100, там видно будет...
        int batchSize = 100;
        List<List<Patient>> batches = new ArrayList<>();
        for (int i = 0; i < patients.size(); i += batchSize) {
            batches.add(patients.subList(i, Math.min(i + batchSize, patients.size())));
        }

        int threadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 4);
        List<Future<WorkFlowResult>> futures = new ArrayList<>();

        for (int batchNum = 0; batchNum < batches.size(); batchNum++) {
            int shortIndex = (batchNum % 10) + 1; // 1 to 10, repeated

            String user = String.format("USR%03d", shortIndex);
            Path userFolder = aisomsFolder.resolve(user);
            // String endingMessageId = String.format(".%s@ERIC.YOUNG.RU", user);

            List<Patient> patients = batches.get(batchNum);
            output.accept("Обработка пакета #" + (batchNum + 1) + " из " + batches.size());

            /// Запрос типа search
            futures.add(executor.submit(() -> {
                SearchRequestWithRetries searchRequestWithRetries = new SearchRequestWithRetries();
                HistoryRequestWithRetries historyRequestWithRetries = new HistoryRequestWithRetries();

                WorkFlowResult searchResult = searchRequestWithRetries.run(patients, output, userFolder,
                        String.format(".%s@ERZL.SEARCH.MSK.OMS", user), helper);
                WorkFlowResult historyResult = historyRequestWithRetries.run(patients, output, userFolder,
                        String.format(".%s@ERZL.HISTORY.MSK.OMS", user), sprlpu, helper);

                return (searchResult == WorkFlowResult.SUCCESS && historyResult == WorkFlowResult.SUCCESS)
                        ? WorkFlowResult.SUCCESS
                        : WorkFlowResult.FAILURE;
                }));

            /// Запрос типа history
            // HistoryRequestWithRetries.run(patients, output, userFolder, endingMessageId, sprlpu);
        }
        executor.shutdown();

        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            output.accept("Поток был прерван во время ожидания завершения: " + e.getMessage());
        }

        /// Пишем в результирующий Excel response.xlsx
        if(failed(helper.writeToExcel(patients)))
            return WorkFlowResult.FAILURE;

        return WorkFlowResult.SUCCESS;
    }


    private boolean failed(WorkFlowResult result) {
        return result != WorkFlowResult.SUCCESS;
    }

}
