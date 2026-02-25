package com.perch.service.impl;

import com.perch.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

    private final VectorStore vectorStore;

    // 神奇的写法：自动扫描 knowledge 目录下所有的 epub 文件！
    @Value("classpath:knowledge/*.epub")
    private Resource[] epubFiles;

    @Override
    public void ingestSingleBook(String fileName) {
        log.info("▶️ 收到入库指令，准备处理单本书籍: {}", fileName);

        // 动态加载 knowledge 目录下的指定文件
        Resource resource = new ClassPathResource("knowledge/" + fileName);
        if (!resource.exists()) {
            log.error("❌ 找不到文件: {}", fileName);
            throw new RuntimeException("文件不存在: " + fileName);
        }

        try {
            // 1. 读取
            log.info("   📖 正在使用 Tika 解析电子书内容...");
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            List<Document> documents = tikaReader.get();

            // 2. 注入元数据
            String bookName = fileName.replace(".epub", "").replace(".txt", "");
            for (Document doc : documents) {
                doc.getMetadata().put("book_name", bookName);
                doc.getMetadata().put("category", "psychology");
            }

            // 3. 切片 (保持咱们测试好的完美参数)
            TokenTextSplitter splitter = new TokenTextSplitter(300, 100, 5, 10000, true);
            List<Document> splitDocuments = splitter.apply(documents);
            log.info("   ✂️ 【{}】解析切片完成，共获得 {} 个知识段落。", bookName, splitDocuments.size());

            // 4. 向量化入库
            log.info("   🧠 正在调用 bge-m3 进行向量化计算，请保持网络和程序稳定...");
            vectorStore.add(splitDocuments);

            log.info("✅ 【{}】入库成功！安全落地！", bookName);

        } catch (Exception e) {
            log.error("❌ 处理书籍 {} 时发生致命错误: {}", fileName, e.getMessage());
            throw new RuntimeException("入库失败: " + e.getMessage());
        }
    }
}