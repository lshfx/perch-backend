package com.perch.controller;

import com.perch.pojo.common.Result;
import com.perch.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Knowledge ingestion test endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeIngestionController {

    private final KnowledgeIngestionService knowledgeIngestionService;

    /**
     * Manually trigger ingestion for the psychology book.
     */
    @PostMapping("/ingest/single")
    public Result<String> ingestSingleBook(@RequestParam String fileName) {
        // 开一个独立的后台线程去干脏活累活
        CompletableFuture.runAsync(() -> {
            try {
                knowledgeIngestionService.ingestSingleBook(fileName);
            } catch (Exception e) {
                // 这里只能打日志了，因为 HTTP 请求已经提前返回了
                log.error("❌ 后台入库书籍 {} 失败: {}", fileName, e.getMessage());
            }
        });

        // 瞬间返回给前端（Postman），再也不会超时了！
        return Result.success(null, "书籍《" + fileName + "》已加入后台入库队列，请留意控制台进度日志！");
    }
}
