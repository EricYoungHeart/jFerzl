package ru.petsinbloom.jFerzl;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/// Метод run класса будет вызываться в concurrent среде
class SearchRequestWithRetries {

    WorkFlowResult run(
            List<Patient> patients, Consumer<String> output, Path userFolder, String endingMessageId, FerzlWorkflowHelper helper) {

        int MAX_ATTEMPTS = 5;
        int attempt = 0;
        long missing = Integer.MAX_VALUE;

        while (attempt < MAX_ATTEMPTS && missing > 0) {
            attempt++;
            // output.accept("FERZ_PI_SEARCH: попытка №" + attempt);

            String PI_SEARCH_ID = String.format("%06d", new Random().nextInt(1_000_000));
            String FERZ_PI_SEARCH_DATA = "pi_search_request_" + PI_SEARCH_ID;
            String FERZ_PI_SEARCH_RESPONSE = "pi_search_response_" + PI_SEARCH_ID;
            String FERZ_PI_SEARCH_PASSPORT = "b" + PI_SEARCH_ID;
            String FERZ_PI_SEARCH_DFile = "d" + PI_SEARCH_ID;
            String expectedMessageId = PI_SEARCH_ID + endingMessageId;
            String zipSearchResponse = "searchResponse_" + PI_SEARCH_ID + ".zip";

            /// Генерируем файл данных запроса ferz_pi_search
            if (failed(helper.createDataSearchRequest(patients, FERZ_PI_SEARCH_DATA)))
                return WorkFlowResult.FAILURE;

            /// Создаем файл-паспорт запроса ferz_pi_search
            if (failed(helper.createPassportSearchRequest(FERZ_PI_SEARCH_PASSPORT, PI_SEARCH_ID, FERZ_PI_SEARCH_DATA, expectedMessageId)))
                return WorkFlowResult.FAILURE;

            /// Копируем сформированные файлы (паспорт и дата) search
            if (failed(helper.copySearchRequestToOutputFolder(userFolder.resolve("OUTPUT"),
                    FERZ_PI_SEARCH_DATA, FERZ_PI_SEARCH_DFile, FERZ_PI_SEARCH_PASSPORT)))
                return WorkFlowResult.FAILURE;

            /// Отправка
            if (failed(helper.waitForRequestToBePickedUp(userFolder.resolve("OUTPUT"), FERZ_PI_SEARCH_PASSPORT)))
                return WorkFlowResult.FAILURE;

            /// Поиск b-файла с заданным Resent-Message-Id и Subject вида FERZ_PI_SEARCH ACCEPTED ID:157702
            /// Если такой файл найден, значит запрос поставлен в очередь
            boolean isRequestInQueue = false;
            if (failed(helper.waitForSearchRequestAcceptance(expectedMessageId, userFolder.resolve("INPUT"))))
                return WorkFlowResult.FAILURE;

            // Здесь надо скопировать ответ и удалить полученные файлы
            // Пока это не делается для целей отладки
            // Здесь надо скопировать ответ и удалить полученные файлы

            /// Ищем сообщение с результатом обработки запроса
            if (failed(helper.waitForSearchRequestReady(userFolder.resolve("INPUT"), expectedMessageId, zipSearchResponse)))
                return WorkFlowResult.FAILURE;

            // Копируем разархивированный pi_search_response.zip из input в локалку
            if (failed(helper.copySearchResponseFromInputToLocale(zipSearchResponse, userFolder.resolve("INPUT"))))
                return WorkFlowResult.FAILURE;

            /// Распаковываем ответ
            if (failed(helper.unzipSearchResponse(zipSearchResponse, FERZ_PI_SEARCH_RESPONSE)))
                return WorkFlowResult.FAILURE;

            ///  Загружаем полученные данные из первого запроса в модель patients
            if (failed(helper.applySearchResponseData(FERZ_PI_SEARCH_RESPONSE, patients)))
                return WorkFlowResult.FAILURE;

            // Считаем сколько пустых
            long previousAttempt = missing;
            missing = patients.stream().filter(p -> p.getEnp() == null || p.getEnp().isEmpty()).count();
            output.accept("Пустых записей в ответе: " + missing);
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
