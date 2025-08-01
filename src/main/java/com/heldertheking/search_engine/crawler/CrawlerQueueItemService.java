package com.heldertheking.search_engine.crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrawlerQueueItemService {
    private final CrawlerQueueRepository repository;

    @Autowired
    public CrawlerQueueItemService(CrawlerQueueRepository crawlerQueueRepository) {
        this.repository = crawlerQueueRepository;
    }

    public List<CrawlerQueueItem> getQueueForStatus(CrawlerQueueItem.CrawlerStatus status) {
        return repository.findByStatus(status);
    }

    public List<CrawlerQueueItem> getAllQueues() {
        return repository.findAll();
    }
}
