package com.heldertheking.search_engine.crawler;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "crawler_queue")
public class CrawlerQueueItem {
    @Id
    @EqualsAndHashCode.Include
    private String url;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlerStatus status = CrawlerStatus.STOPPED;

    public enum CrawlerStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        STOPPED
    }

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_crawled_at")
    private Instant lastCrawledAt; // Timestamp of the last crawl attempt

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // Nullable field for the domain where this URL was found
    @Column(name = "found_on_domain", nullable = true)
    private String foundOnDomain;
}
