package com.perch.controller;

import com.perch.pojo.common.Result;
import com.perch.service.KnowledgeIngestionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Knowledge ingestion endpoints.
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeIngestionController {

    private final KnowledgeIngestionService knowledgeIngestionService;

    /**
     * Upload and ingest a single book with dynamic parsing options.
     */
    @PostMapping(value = "/ingest/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> ingestSingleBook(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer chunkSize,
            @RequestParam(required = false) Integer minChunkSizeChars,
            @RequestParam(required = false) Integer minChunkLengthToEmbed,
            @RequestParam(required = false) Integer maxNumChunks,
            @RequestParam(required = false) Boolean keepSeparator) {
        String taskId = knowledgeIngestionService.ingestSingleBook(
                file,
                bookName,
                category,
                chunkSize,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                keepSeparator
        );

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", file.getOriginalFilename());
        data.put("bookName", bookName);
        data.put("category", category);
        data.put("taskId", taskId);

        return Result.success(data, "ingestion queued");
    }
}
