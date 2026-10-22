package org.example.genaijavaspring;

import org.example.genaijavaspring.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenAiJavaSpringApplication implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;

    public GenAiJavaSpringApplication(RagIngestionService ragIngestionService) {
        this.ragIngestionService = ragIngestionService;
    }

    public static void main(String[] args) {

        SpringApplication.run(GenAiJavaSpringApplication.class, args);
        System.out.println("running...");
    }

    @Override
    public void run(String... args) throws Exception {

        ragIngestionService.initializePgVectorStore();
    }
}
