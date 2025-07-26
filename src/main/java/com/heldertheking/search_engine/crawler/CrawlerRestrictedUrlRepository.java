package com.heldertheking.search_engine.crawler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerRestrictedUrlRepository extends JpaRepository<CrawlerRestrictedUrl, Long> {
}

