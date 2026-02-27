package com.perch.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: lsh
 * @Date: 2026/02/25/11:23
 * @Description: 向量化服务类
 */
public interface KnowledgeIngestionService {

    String ingestSingleBook(
            MultipartFile file,
            String bookName,
            String category,
            Integer chunkSize,
            Integer minChunkSizeChars,
            Integer minChunkLengthToEmbed,
            Integer maxNumChunks,
            Boolean keepSeparator
    );
}
