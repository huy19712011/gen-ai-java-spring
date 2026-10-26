package org.example.genaijavaspring;

import org.example.genaijavaspring.rag.service.PdfWatcherService;
import org.example.genaijavaspring.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenAiJavaSpringApplication implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;
    private final PdfWatcherService pdfWatcherService;

    public GenAiJavaSpringApplication(RagIngestionService ragIngestionService, PdfWatcherService pdfWatcherService) {
        this.ragIngestionService = ragIngestionService;
        this.pdfWatcherService = pdfWatcherService;
    }

    public static void main(String[] args) {

        SpringApplication.run(GenAiJavaSpringApplication.class, args);
        System.out.println("running...");
    }

    @Override
    public void run(String... args) throws Exception {

        ragIngestionService.initializePgVectorStore();
        pdfWatcherService.start();
    }
}
