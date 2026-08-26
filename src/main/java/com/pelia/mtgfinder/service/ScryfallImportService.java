package com.pelia.mtgfinder.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pelia.mtgfinder.dto.ScryfallBulkDataResponse;
import com.pelia.mtgfinder.dto.ScryfallCard;
import com.pelia.mtgfinder.dto.ImportLogResponse;
import com.pelia.mtgfinder.dto.ScryfallImportResponse;
import com.pelia.mtgfinder.model.BulkImport;
import com.pelia.mtgfinder.model.ImportStatus;

import tools.jackson.databind.ObjectMapper;

@Service
public class ScryfallImportService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    private static final int MAX_REQUEST_ATTEMPTS = 3;
    private static final Pattern CARD_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${scryfall.api.url}")
    private String scryfallApiUrl;

    @Value("${com.pelia.mtgfinder.user-agent}")
    private String userAgent;

    @Value("${com.pelia.mtgfinder.bulk-insert-batch-size}")
    Integer bulkInsertBatchSize;

    private final PostgresBulkCopyService postgresBulkCopyService;
    private final BulkImportService bulkImportService;
    private final ImportLogService importLogService;

    public ScryfallImportService(
            ObjectMapper objectMapper,
            PostgresBulkCopyService postgresBulkCopyService,
            BulkImportService bulkImportService,
            ImportLogService importLogService) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.postgresBulkCopyService = postgresBulkCopyService;
        this.bulkImportService = bulkImportService;
        this.importLogService = importLogService;
    }

    public ScryfallImportResponse importCards(String bulkType) {
        ScryfallBulkDataResponse bulkData = getBulkJsonlUrl(bulkType);
        URI jsonlDownloadUri = URI.create(bulkData.jsonlDownloadUri());

        BulkImport bulkImport = new BulkImport();
        bulkImport.setScryfallBulkType(bulkData.type());
        bulkImport.setScryfallUpdatedAt(bulkData.updatedAt());
        bulkImport = bulkImportService.save(bulkImport);

        try (Stream<ScryfallCard> cardStream = streamBulkCards(jsonlDownloadUri, bulkImport.getId())) {
            int batchCount = 0;
            int totalCards = 0;
            List<ScryfallCard> cards = new ArrayList<>(bulkInsertBatchSize);
            Iterator<ScryfallCard> iterator = cardStream.iterator();

            while (iterator.hasNext()) {
                cards.add(iterator.next());
                if (cards.size() >= bulkInsertBatchSize) {
                    batchCount++;
                    System.out.println("Processing batch " + batchCount + " with " + cards.size() + " cards");

                    postgresBulkCopyService.insertBatchCopy(bulkImport.getId(), cards);
                    totalCards += cards.size();
                    cards.clear();
                }
            }

            if (!cards.isEmpty()) {
                batchCount++;
                System.out.println("Processing final batch " + batchCount + " with " + cards.size() + " cards");
                postgresBulkCopyService.insertBatchCopy(bulkImport.getId(), cards);
                totalCards += cards.size();
            }

            bulkImport.setTotalCards(totalCards);
            bulkImportService.save(bulkImport);
            BulkImport completedImport = bulkImportService.activate(bulkImport.getId());
            List<ImportLogResponse> logs = importLogService.findByImportId(bulkImport.getId()).stream()
                    .map(ImportLogResponse::from)
                    .toList();
            return ScryfallImportResponse.from(completedImport, logs);
        } catch (RuntimeException exception) {
            try {
                postgresBulkCopyService.deleteImportData(bulkImport.getId());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            bulkImport.setStatus(ImportStatus.FAILED);
            try {
                bulkImportService.save(bulkImport);
            } catch (RuntimeException statusException) {
                exception.addSuppressed(statusException);
            }
            throw exception;
        }
    }

    private Stream<ScryfallCard> streamBulkCards(URI jsonlDownloadUri, UUID importId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(jsonlDownloadUri)
                .timeout(REQUEST_TIMEOUT)
                .setHeader("User-Agent", userAgent)
                .setHeader("Accept", "application/x-ndjson,application/json;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = sendWithRetry(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream body = response.body()) {
                    System.out.println("Body: " + new String(body.readAllBytes(), StandardCharsets.UTF_8));
                }
                throw new IllegalStateException(
                        "Scryfall JSONL download failed with HTTP " + response.statusCode());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    decompressIfNecessary(response.body()), StandardCharsets.UTF_8));
            AtomicLong lineNumber = new AtomicLong();

            return reader.lines()
                    .map(line -> parseCard(importId, lineNumber.incrementAndGet(), line))
                    .filter(Objects::nonNull)
                    .onClose(() -> closeReader(reader));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while downloading Scryfall JSONL", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not open Scryfall JSONL stream", exception);
        }
    }

    private ScryfallCard parseCard(UUID importId, long lineNumber, String line) {
        if (line.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(line, ScryfallCard.class);
        } catch (RuntimeException exception) {
            importLogService.log(importId, buildDeserializationErrorMessage(lineNumber, line, exception));
            return null;
        }
    }

    private String buildDeserializationErrorMessage(long lineNumber, String line, RuntimeException exception) {
        Matcher cardIdMatcher = CARD_ID_PATTERN.matcher(line);
        String cardId = cardIdMatcher.find() ? cardIdMatcher.group(1) : "indisponível";
        String errorMessage = exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage();

        return "Erro ao desserializar linha " + lineNumber
                + " (card ID: " + cardId + "): " + errorMessage
                + System.lineSeparator() + "Conteúdo: " + line;
    }

    private ScryfallBulkDataResponse getBulkJsonlUrl(String bulkType) {
        if (bulkType == null || bulkType.isBlank()) {
            throw new IllegalArgumentException("Bulk type must not be blank");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scryfallApiUrl + "/bulk-data/" + bulkType))
                .timeout(REQUEST_TIMEOUT)
                .setHeader("User-Agent", userAgent)
                .setHeader("Accept", "application/json;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Scryfall bulk-data request failed with HTTP " + response.statusCode());
            }

            ScryfallBulkDataResponse bulkData = objectMapper.readValue(
                    response.body(), ScryfallBulkDataResponse.class);

            if (bulkData.jsonlDownloadUri() == null || bulkData.jsonlDownloadUri().isBlank()) {
                throw new IllegalStateException(
                        "Scryfall response does not contain jsonl_download_uri for bulk type " + bulkType);
            }

            return bulkData;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while requesting Scryfall bulk data", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read Scryfall bulk-data response", exception);
        }
    }

    private InputStream decompressIfNecessary(InputStream inputStream) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(inputStream);
        buffered.mark(2);
        int firstByte = buffered.read();
        int secondByte = buffered.read();
        buffered.reset();

        if (firstByte == 0x1f && secondByte == 0x8b) {
            return new GZIPInputStream(buffered);
        }

        return buffered;
    }

    private <T> HttpResponse<T> sendWithRetry(
            HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        Duration retryDelay = INITIAL_RETRY_DELAY;

        for (int attempt = 1; ; attempt++) {
            HttpResponse<T> response = httpClient.send(request, bodyHandler);
            if (!isRetryable(response.statusCode()) || attempt >= MAX_REQUEST_ATTEMPTS) {
                return response;
            }

            closeResponseBody(response.body());
            Thread.sleep(retryDelay.toMillis());
            retryDelay = retryDelay.multipliedBy(2);
        }
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500 && statusCode < 600;
    }

    private void closeResponseBody(Object body) throws IOException {
        if (body instanceof InputStream inputStream) {
            inputStream.close();
        }
    }

    private void closeReader(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not close Scryfall JSONL stream", exception);
        }
    }
}
