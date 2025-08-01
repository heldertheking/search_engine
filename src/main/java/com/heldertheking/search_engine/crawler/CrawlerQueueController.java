package com.heldertheking.search_engine.crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crawler-queue")
public class CrawlerQueueController {

    private final CrawlerQueueItemService service;

    @Autowired
    public CrawlerQueueController(CrawlerQueueItemService crawlerQueueItemService) {
        this.service = crawlerQueueItemService;
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CrawlerQueueItem>> getQueueForStatus(@PathVariable CrawlerQueueItem.CrawlerStatus status) {
        return ResponseEntity.ok(service.getQueueForStatus(status));
    }

    @GetMapping("/")
    public ResponseEntity<List<CrawlerQueueItem>> getAllQueues() {
        return ResponseEntity.ok(service.getAllQueues());
    }

    @PostMapping("/stop")
    public String stopCrawlerQueue() {
        // This method should stop the crawler queue.
        // For now, it returns a placeholder string.
        return "Stopping the crawler queue is not implemented yet.";
    }

    @PostMapping("/start")
    public String startCrawlerQueue() {
        // This method should start the crawler queue.
        // For now, it returns a placeholder string.
        return "Starting the crawler queue is not implemented yet.";
    }

    @DeleteMapping("/{domain}")
    public String deleteQueueForDomain(@PathVariable String domain) {
        // This method should delete the queue for a specific domain.
        // For now, it returns a placeholder string.
        return "Deleting crawler queue for domain " + domain + " is not implemented yet.";
    }
}
