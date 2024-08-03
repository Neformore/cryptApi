import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class CryptApi {

    public static void main(String[] args) {

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
}