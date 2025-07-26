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
@Table(name = "crawler_restricted_urls")
public class CrawlerRestrictedUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String reason; // e.g. "Disallowed by robots.txt"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

