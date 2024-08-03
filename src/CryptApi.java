import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CryptApi {

    private final HttpClient httpClient;
    private final int requestLimit;
    private final long intervalMillis;
    private final ArrayBlockingQueue<Long> requestTimestamps;

    public CryptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.requestLimit = requestLimit;
        this.intervalMillis = timeUnit.toMillis(1);
        this.requestTimestamps = new ArrayBlockingQueue<>(requestLimit);
    }

    public void createDocument(Document document, String sign) {

        while (true) {
            try {
                long currentTime = System.currentTimeMillis();

                // Очищаем очередь, если время от момента заполнения первого элемента превысило заданный временной интервал
                cleanUpOldTimestamps(currentTime);

                // Проверяем, не превышен ли лимит
                if (requestTimestamps.size() < requestLimit) {
                    // Добавляем текущую метку времени
                    requestTimestamps.add(currentTime);

                    // Сериализация документа в JSON
                    String jsonDocument = toJson(document);

                    // Отправляем HTTP POST запрос
                    HttpResponse<String> response = sendDocument(jsonDocument, sign);

                    if (response.statusCode() == 200) {
                        System.out.println(String.format("Документ %s успешно создан.", document.getDocId()));
                    } else {
                        System.out.println(String.format("Ошибка при создании документа: %s, response body: %s", document.getDocId(), response.body()));
                    }
                    return;
                }

                // Если лимит превышен, ожидаем до следующего интервала
                long waitTime = requestTimestamps.peek() + intervalMillis - currentTime;
                if (waitTime > 0) {
                    Thread.sleep(waitTime);
                }
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ошибка при выполнении запроса", e);
            }
        }
    }

    private void cleanUpOldTimestamps(long currentTime) {
        long earliestTimestamp = currentTime - intervalMillis;
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < earliestTimestamp) {
            requestTimestamps.poll();
        }
    }

    private HttpResponse<String> sendDocument(String jsonDocument, String sign) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + sign)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String toJson(Document document) {
        try {
            return new ObjectMapper().writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сериализации документа в JSON", e);
        }
    }

    @Data
    public static class Description {

        @JsonProperty("participantInn")
        private String participantInn;
    }

    @Data
    public static class Product {

        @JsonProperty("certificate_document")
        private String certificateDocument;

        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("tnved_code")
        private String tnvedCode;

        @JsonProperty("uit_code")
        private String uitCode;

        @JsonProperty("uitu_code")
        private String uituCode;
    }

    @Data
    public static class Document {

        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private Product[] products;

        @JsonProperty("reg_date")
        private String regDate;

        @JsonProperty("reg_number")
        private String regNumber;
    }

    /**
     * Тест
     */
    public static void main(String[] args) {
        // 1 запрос в секунду
        CryptApi api = new CryptApi(TimeUnit.SECONDS, 1);

        // Для теста создаем пул на 2 потока
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {

            // Для использования в лямбда-выражении
            int finalI = i;

            executorService.submit(() -> {
                Description description = new Description();
                description.setParticipantInn("123456789");

                Product product = new Product();
                product.setCertificateDocument("cert" + finalI);
                product.setCertificateDocumentDate("2020-01-23");
                product.setCertificateDocumentNumber("num" + finalI);
                product.setOwnerInn("123456789");
                product.setProducerInn("987654321");
                product.setProductionDate("2020-01-23");
                product.setTnvedCode("1234");
                product.setUitCode("uit123");
                product.setUituCode("uitu123");

                Document document = new Document();
                document.setDescription(description);
                document.setDocId("doc" + finalI);
                document.setDocStatus("Active");
                document.setDocType("LP_INTRODUCE_GOODS");
                document.setImportRequest(true);
                document.setOwnerInn("123456789");
                document.setParticipantInn("123456789");
                document.setProducerInn("987654321");
                document.setProductionDate("2020-01-23");
                document.setProductionType("Type1");
                document.setProducts(new Product[] {product});
                document.setRegDate("2020-01-23");
                document.setRegNumber("reg" + finalI);

                api.createDocument(document, "eyJTokenTokenTokenTokenToken");
            });
        }

        executorService.shutdown();
    }
}