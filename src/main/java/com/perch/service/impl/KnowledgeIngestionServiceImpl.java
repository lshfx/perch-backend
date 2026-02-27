package com.perch.service.impl;

import com.perch.exception.CustomException;
import com.perch.service.KnowledgeIngestionService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service

public class KnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

    private final VectorStore vectorStore;

    private final Executor taskExecutor;

    public KnowledgeIngestionServiceImpl(VectorStore vectorStore, @Qualifier("archiveTaskExecutor")Executor taskExecutor) {
        this.vectorStore = vectorStore;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public String ingestSingleBook(
            MultipartFile file,
            String bookName,
            String category,
            Integer chunkSize,
            Integer minChunkSizeChars,
            Integer minChunkLengthToEmbed,
            Integer maxNumChunks,
            Boolean keepSeparator) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(400, "upload file is required");
        }

        String originalFilename = file.getOriginalFilename();
        String resolvedBookName = resolveBookName(bookName, originalFilename);
        String resolvedCategory = StringUtils.hasText(category) ? category : "general";

        int resolvedChunkSize = resolvePositive(chunkSize, 300, "chunkSize");
        int resolvedMinChunkSizeChars = resolvePositive(minChunkSizeChars, 100, "minChunkSizeChars");
        int resolvedMinChunkLengthToEmbed = resolvePositive(minChunkLengthToEmbed, 5, "minChunkLengthToEmbed");
        int resolvedMaxNumChunks = resolvePositive(maxNumChunks, 10000, "maxNumChunks");
        boolean resolvedKeepSeparator = keepSeparator == null || keepSeparator;

        byte[] fileBytes = readFileBytes(file);
        String taskId = UUID.randomUUID().toString();

        log.info("Queue ingestion task: taskId={}, file={}, bookName={}, category={}",
                taskId, originalFilename, resolvedBookName, resolvedCategory);

        CompletableFuture.runAsync(
                () -> ingestAsync(
                        taskId,
                        fileBytes,
                        originalFilename,
                        resolvedBookName,
                        resolvedCategory,
                        resolvedChunkSize,
                        resolvedMinChunkSizeChars,
                        resolvedMinChunkLengthToEmbed,
                        resolvedMaxNumChunks,
                        resolvedKeepSeparator),
                taskExecutor
        ).exceptionally(ex -> {
            log.error("Ingestion task failed: taskId={}, file={}, error={}",
                    taskId, originalFilename, ex.getMessage());
            return null;
        });

        return taskId;
    }

    private void ingestAsync(
            String taskId,
            byte[] fileBytes,
            String originalFilename,
            String bookName,
            String category,
            int chunkSize,
            int minChunkSizeChars,
            int minChunkLengthToEmbed,
            int maxNumChunks,
            boolean keepSeparator) {
        try {
            Resource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            List<Document> documents = tikaReader.get();

            for (Document doc : documents) {
                doc.getMetadata().put("book_name", bookName);
                doc.getMetadata().put("category", category);
                doc.getMetadata().put("task_id", taskId);
                if (StringUtils.hasText(originalFilename)) {
                    doc.getMetadata().put("source_file", originalFilename);
                }
            }

            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,
                    minChunkSizeChars,
                    minChunkLengthToEmbed,
                    maxNumChunks,
                    keepSeparator
            );
            List<Document> splitDocuments = splitter.apply(documents);
            log.info("Split completed: taskId={}, book={}, chunks={}", taskId, bookName, splitDocuments.size());

            vectorStore.add(splitDocuments);
            log.info("Ingestion completed: taskId={}, book={}", taskId, bookName);
        } catch (Exception e) {
            log.error("Ingestion failed: taskId={}, file={}, error={}", taskId, originalFilename, e.getMessage());
        }
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(500, "failed to read upload file");
        }
    }

    private String resolveBookName(String bookName, String originalFilename) {
        if (StringUtils.hasText(bookName)) {
            return bookName;
        }
        if (!StringUtils.hasText(originalFilename)) {
            return "unknown";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        return lastDot > 0 ? originalFilename.substring(0, lastDot) : originalFilename;
    }

    private int resolvePositive(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new CustomException(400, fieldName + " must be > 0");
        }
        return value;
    }
}
