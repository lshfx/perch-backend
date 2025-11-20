package com.perch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.autoconfigure.zhipuai.ZhiPuAiAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;

@SpringBootApplication()
public class PerchBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerchBackendApplication.class, args);
    }

}
