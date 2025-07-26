package com.heldertheking.search_engine.crawler;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import crawlercommons.robots.BaseRobotsParser;
import crawlercommons.robots.SimpleRobotRules;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Crawler is a service for recursively crawling web pages starting from a given URL.
 * It uses Jsoup for HTTP requests and HTML parsing, and supports configurable crawl depth.
 *
 * <p>
 * Usage:
 * <pre>
 *     crawler.start("<a href="https://example.com">...</a>");
 * </pre>
 * <p>
 * The crawler is designed to be scheduler-ready and can be run asynchronously.
 * <br><br>
 * Configuration:
 * - Set the maximum crawl depth in application.properties with 'crawler.max-crawl-level'.
 * <br><br>
 * Thread Safety:
 * - Each crawl run uses its own visited set, making it safe for concurrent scheduled runs.
 */
@Log4j2
@Service
public class Crawler {
    /**
     * Maximum crawl depth, configurable via 'crawler.max-crawl-level' in application.properties.
     */
    @Value("${crawler.max-crawl-level:5}")
    private int maxCrawlLevel;

    private final CrawlerQueueRepository queueRepository;
    private final CrawlerRestrictedUrlRepository restrictedUrlRepository;
    private final Map<String, BaseRobotRules> robotsCache = new ConcurrentHashMap<>();

    @Value("${crawler.user-agent:MyCrawlerBot}")
    private String userAgent;

    // Registry to track active crawlers: thread name -> info
    private static final ConcurrentMap<String, CrawlerTaskInfo> activeCrawlers = new ConcurrentHashMap<>();

    public static Map<String, CrawlerTaskInfo> getActiveCrawlers() {
        return Map.copyOf(activeCrawlers);
    }

    public record CrawlerTaskInfo(String url, Instant startTime) {
    }

    @Autowired
    public Crawler(CrawlerQueueRepository queueRepository, CrawlerRestrictedUrlRepository restrictedUrlRepository) {
        this.queueRepository = queueRepository;
        this.restrictedUrlRepository = restrictedUrlRepository;
    }

    /**
     * Starts the crawling process from the given URL asynchronously.
     *
     * @param url the starting URL for the crawl
     */
    @Async("crawlerTaskExecutor")
    public void start(String url) {
        String threadName = "Crawler-" + url + "-" + Instant.now().toEpochMilli();
        Thread.currentThread().setName(threadName);
        activeCrawlers.put(threadName, new CrawlerTaskInfo(url, Instant.now()));
        log.info("Starting crawler for \"{}\" with a max crawl depth of {}", url, maxCrawlLevel);
        try {
            crawl(1, url, new HashSet<>());
        } finally {
            activeCrawlers.remove(threadName);
        }
    }

    /**
     * Scheduled method to trigger crawling at fixed intervals.
     * Adjust the cron or fixedDelay as needed.
     */
    @Scheduled(fixedDelayString = "${crawler.scheduled-delay-ms:60000}")
    public void scheduledCrawl() {
        // Fetch all items with status PENDING
        var pendingItems = queueRepository.findAll().stream()
                .filter(item -> item.getStatus() == CrawlerQueueItem.CrawlerStatus.PENDING)
                .toList();
        for (var item : pendingItems) {
            log.info("Scheduled crawl triggered for {}", item.getUrl());
            start(item.getUrl());
        }
    }

    // Shutdown flag
    private static final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public static void stop() {
        isShutdown.set(true);
        log.info("Crawler shutdown requested. All running crawlers will stop as soon as possible.");
    }

    public static boolean isShutdown() {
        return isShutdown.get();
    }

    @PreDestroy
    public void onShutdown() {
        stop();
        log.info("@PreDestroy: Crawler stop() called for graceful shutdown.");
    }

    /**
     * Recursively crawls web pages up to the configured depth.
     *
     * @param level   current crawl depth
     * @param url     URL to crawl
     * @param visited set of already visited URLs
     */
    public void crawl(int level, String url, HashSet<String> visited) {
        if (isShutdown.get()) {
            log.info("Shutdown in progress. Exiting crawl for {}", url);
            return;
        }
        if (visited.contains(url)) {
            log.debug("URL already visited: {}", url);
            return;
        }

        if (level > maxCrawlLevel) {
            log.debug("Max crawl level reached: {} for URL: {}", maxCrawlLevel, url);
            return;
        }

        // Check robots.txt before crawling
        if (!isUrlAllowedByRobots(url)) {
            log.warn("Blocked by robots.txt: {}", url);
            restrictedUrlRepository.save(
                CrawlerRestrictedUrl.builder()
                    .url(url)
                    .userAgent(userAgent)
                    .reason("Disallowed by robots.txt")
                    .build()
            );
            return;
        }

        // Update queue item status to IN_PROGRESS
        queueRepository.findByUrl(url).ifPresent(item -> {
            item.setStatus(CrawlerQueueItem.CrawlerStatus.IN_PROGRESS);
            item.setLastMessage("Crawling in progress");
            queueRepository.save(item);
        });

        // Politeness delay to avoid flooding the server and logs
        try {
            Thread.sleep(500); // 500ms delay between requests
            if (isShutdown.get()) {
                log.info("Shutdown in progress. Exiting crawl for {} after sleep", url);
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Politeness delay interrupted", e);
            // Set status to FAILED and last message
            queueRepository.findByUrl(url).ifPresent(item -> {
                item.setStatus(CrawlerQueueItem.CrawlerStatus.FAILED);
                item.setLastMessage("Crawling interrupted: " + e.getMessage());
                item.setLastCrawledAt(java.time.Instant.now());
                queueRepository.save(item);
            });
            return;
        }
        try {
            if (isShutdown.get()) {
                log.info("Shutdown in progress. Exiting crawl for {} before request", url);
                return;
            }
            Document doc = request(url, visited);
            visited.add(url);

            if (doc != null) {
                for (Element link : doc.select("a[href]")) {
                    if (isShutdown.get()) {
                        log.info("Shutdown in progress. Exiting crawl for {} during link processing", url);
                        return;
                    }
                    String next_url = link.absUrl("href");
                    // Skip non-http(s) protocols
                    if (!(next_url.startsWith("http://") || next_url.startsWith("https://"))) {
                        log.trace("Skipping non-http(s) URL: {}", next_url);
                        continue;
                    }
                    try {
                        new URL(next_url); // Validate URL format
                        if (!visited.contains(next_url)) {
                            // Check if base URL is different
                            String baseCurrent = getBaseUrl(url);
                            String baseNext = getBaseUrl(next_url);
                            if (!baseCurrent.equals(baseNext)) {
                                // Only add if not already in queue
                                if (queueRepository.findByUrl(baseNext).isEmpty()) {
                                    queueRepository.save(
                                        CrawlerQueueItem.builder()
                                            .url(baseNext)
                                            .status(CrawlerQueueItem.CrawlerStatus.STOPPED)
                                            .lastMessage("Discovered new domain by crawler, awaiting approval.")
                                            .foundOnDomain(baseCurrent)
                                            .build()
                                    );
                                }
                                // Do not crawl this new domain now
                                continue;
                            }
                            log.debug("Found new URL: {}", next_url);
                            crawl(level + 1, next_url, visited);
                        } else {
                            log.trace("Skipping already visited URL: {}", next_url);
                        }
                    } catch (MalformedURLException e) {
                        log.warn("Invalid URL found: {}", next_url, e);
                    }
                }
                // Mark as COMPLETED after crawl
                queueRepository.findByUrl(url).ifPresent(item -> {
                    item.setStatus(CrawlerQueueItem.CrawlerStatus.COMPLETED);
                    item.setLastMessage("Crawling completed");
                    item.setLastCrawledAt(java.time.Instant.now());
                    queueRepository.save(item);
                });
            } else {
                // Mark as FAILED if crawl failed
                queueRepository.findByUrl(url).ifPresent(item -> {
                    item.setStatus(CrawlerQueueItem.CrawlerStatus.FAILED);
                    item.setLastMessage("Crawling failed");
                    item.setLastCrawledAt(java.time.Instant.now());
                    queueRepository.save(item);
                });
            }
        } catch (Exception e) {
            log.error("Unexpected error during crawling {}: {}", url, e.getMessage(), e);
            // Set status to FAILED and last message
            queueRepository.findByUrl(url).ifPresent(item -> {
                item.setStatus(CrawlerQueueItem.CrawlerStatus.FAILED);
                item.setLastMessage("Crawling failed: " + e.getMessage());
                item.setLastCrawledAt(java.time.Instant.now());
                queueRepository.save(item);
            });
        }
    }

    /**
     * Requests a URL and returns the Document if successful.
     *
     * @param url     The URL to request.
     * @param visited A list of visited URLs to avoid duplicates.
     * @return The Document if the request was successful, null otherwise.
     */
    public Document request(String url, HashSet<String> visited) {
        try {
            // Connects to the URL using Jsoup
            Connection con = Jsoup.connect(url);
            var doc = con.get();

            // Check if the response status code is 200 (OK)
            if (con.response().statusCode() != 200) {
                log.warn("Failed to connect to: {} with status code: {}", url, con.response().statusCode());
                return null;
            }

            log.info("Successfully connected to: {} | {}", url, doc.title());
            visited.add(url);
            return doc;
        } catch (IOException e) {
            log.error("ERROR: connection refused by: {}", url, e);
            return null;
        } catch (Exception e) {
            log.error("ERROR: unexpected error while connecting to: {}", url, e);
            return null;
        }
    }

    /**
     * Checks if a URL is allowed to be crawled according to robots.txt rules.
     */
    private boolean isUrlAllowedByRobots(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String host = url.getProtocol() + "://" + url.getHost()
                + (url.getPort() != -1 ? ":" + url.getPort() : "");
            BaseRobotRules rules = robotsCache.get(host);
            if (rules == null) {
                String robotsUrl = host + "/robots.txt";
                byte[] robotsTxt;
                try {
                    robotsTxt = Jsoup.connect(robotsUrl)
                        .userAgent(userAgent)
                        .ignoreContentType(true)
                        .timeout(5000)
                        .execute()
                        .bodyAsBytes();
                } catch (IOException e) {
                    // If robots.txt cannot be fetched, allow by default
                    log.warn("Could not fetch robots.txt for {}: {}", host, e.getMessage());
                    rules = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
                    robotsCache.put(host, rules);
                    return true;
                }
                BaseRobotsParser parser = new SimpleRobotRulesParser();
                rules = parser.parseContent(robotsUrl, robotsTxt, "text/plain", userAgent);
                robotsCache.put(host, rules);
            }
            return rules.isAllowed(urlStr);
        } catch (Exception e) {
            log.error("Error checking robots.txt for {}: {}", urlStr, e.getMessage());
            // If error, allow by default
            return true;
        }
    }

    /**
     * Extracts the base URL (protocol + host + port if present) from a full URL string.
     */
    private String getBaseUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String base = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() != -1) {
                base += ":" + url.getPort();
            }
            return base;
        } catch (MalformedURLException e) {
            return urlStr; // fallback, should not happen due to earlier checks
        }
    }
}
