package com.heldertheking.search_engine.crawler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlerQueueRepository extends JpaRepository<CrawlerQueueItem, String> {
    Optional<CrawlerQueueItem> findByUrl(String url);

    List<CrawlerQueueItem> findByStatus(CrawlerQueueItem.CrawlerStatus status);
}
