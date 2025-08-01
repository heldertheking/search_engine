package com.heldertheking.search_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PreDestroy;
import com.heldertheking.search_engine.crawler.CrawlerQueueRepository;
import com.heldertheking.search_engine.crawler.CrawlerQueueItem;
import com.heldertheking.search_engine.crawler.Crawler;
import java.time.Instant;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SearchEngineApplication {

    @Autowired
    private CrawlerQueueRepository queueRepository;

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineApplication.class, args);
    }

    @PreDestroy
    public void onShutdown() {
        System.out.println("Application is shutting down, stopping all crawlers...");
        Crawler.stop();
        var inProgressItems = queueRepository.findAll().stream()
            .filter(item -> item.getStatus() == CrawlerQueueItem.CrawlerStatus.IN_PROGRESS)
            .toList();
        for (var item : inProgressItems) {
            item.setStatus(CrawlerQueueItem.CrawlerStatus.PENDING);
            item.setLastMessage("Application was shutdown");
            item.setLastCrawledAt(Instant.now());
            queueRepository.save(item);
        }
    }
}
