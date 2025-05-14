package ru.petsinbloom.jFerzl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

class HistoryRequestWithRetries {

    WorkFlowResult run(
            List<Patient> patients, Consumer<String> output, Path userFolder, String endingMessageId, Map<String, MedCompany> sprlpu,
            FerzlWorkflowHelper helper) {

        int MAX_ATTEMPTS = 5;
        int attempt = 0;
        long missing = Integer.MAX_VALUE;
        while (attempt < MAX_ATTEMPTS && missing > 0) {
            attempt++;
            // output.accept("Попытка №" + attempt);

            String PI_HISTORY_ID = String.format("%06d", new Random().nextInt(1_000_000));
            String FERZ_PI_HISTORY_DATA = "pi_history_request_" + PI_HISTORY_ID;
            String FERZ_PI_HISTORY_RESPONSE = "pi_history_response_" + PI_HISTORY_ID;
            String FERZ_PI_HISTORY_PASSPORT = "b" + PI_HISTORY_ID;
            String FERZ_PI_HISTORY_DFile = "d" + PI_HISTORY_ID;
            String insuFile = "insu_" + PI_HISTORY_ID + ".csv";
            String krepFile = "krep_" + PI_HISTORY_ID + ".csv";
            String expectedMessageId = PI_HISTORY_ID + endingMessageId;
            String zipHistoryResponse = "historyResponse_" + PI_HISTORY_ID + ".zip";

            // Generating the second request FERZ_PI_HISTORY
            if(failed(helper.createDataHistoryRequest(FERZ_PI_HISTORY_DATA, patients)))
                return WorkFlowResult.FAILURE;

            if(failed(helper.createPassportHistoryRequest(FERZ_PI_HISTORY_PASSPORT, PI_HISTORY_ID, FERZ_PI_HISTORY_DATA, expectedMessageId)))
                return WorkFlowResult.FAILURE;

            if(failed(helper.copyHistoryRequestToOutputFolder(userFolder.resolve("OUTPUT"), FERZ_PI_HISTORY_DATA, FERZ_PI_HISTORY_DFile,
                    FERZ_PI_HISTORY_PASSPORT)))
                return WorkFlowResult.FAILURE;

            /// Отправка
            if(failed(helper.waitFilesToBePickedUp(userFolder.resolve("OUTPUT"), FERZ_PI_HISTORY_PASSPORT)))
                return WorkFlowResult.FAILURE;

            /// Поиск b-файла с заданным Resent-Message-Id и Subject вида FERZ_PI_SEARCH ACCEPTED ID:157702
            /// Если такой файл найден, значит запрос поставлен в очередь
            if(failed(helper.waitForHistoryRequestAcceptance(expectedMessageId, userFolder.resolve("INPUT"))))
                return WorkFlowResult.FAILURE;

            /// Ищем сообщение с результатом обработки запроса
            if(failed(helper.waitForHistoryRequestReady(userFolder.resolve("INPUT"), expectedMessageId, zipHistoryResponse)))
                return WorkFlowResult.FAILURE;

            if(failed(helper.copyHistoryResponseFromInputToLocale(zipHistoryResponse, userFolder.resolve("INPUT"))))
                return WorkFlowResult.FAILURE;

            /// Распаковываем ответ
            if(failed(helper.unzipHistoryResponse(zipHistoryResponse, PI_HISTORY_ID)))
                return WorkFlowResult.FAILURE;

            ///
            if(failed(helper.applyHistoryResonseData(patients, insuFile)))
                return WorkFlowResult.FAILURE;

            /// Добавляем прикрепление
            if(failed(helper.applyKrepData(patients, sprlpu, krepFile)))
                return WorkFlowResult.FAILURE;

            long previousAttempt = missing;
            missing = patients.stream().filter(p -> p.getInsurCode() == null || p.getInsurCode().isEmpty()).count();
            output.accept("Пустых записей в ответе: " + missing);

            /// Если два раза подряд одинаковый результат, дальше запросы не отправляем...
            if(previousAttempt == missing){
                break;
            }

        }
        return WorkFlowResult.SUCCESS;
    }

    private static boolean failed(WorkFlowResult result) {
        return result != WorkFlowResult.SUCCESS;
    }


}
