package com.heldertheking.search_engine.telemetry;

import com.heldertheking.search_engine.crawler.Crawler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler-executor")
public class CrawlerExecutorController {

    private final ThreadPoolTaskExecutor crawlerTaskExecutor;

    public CrawlerExecutorController(@Qualifier("crawlerTaskExecutor") ThreadPoolTaskExecutor crawlerTaskExecutor) {
        this.crawlerTaskExecutor = crawlerTaskExecutor;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeCount", crawlerTaskExecutor.getActiveCount());
        status.put("poolSize", crawlerTaskExecutor.getPoolSize());
        status.put("corePoolSize", crawlerTaskExecutor.getCorePoolSize());
        status.put("maxPoolSize", crawlerTaskExecutor.getMaxPoolSize());
        status.put("queueSize", crawlerTaskExecutor.getThreadPoolExecutor().getQueue().size());

        // Add info for each active crawler
        var crawlers = new ArrayList<>();
        for (var entry : Crawler.getActiveCrawlers().entrySet()) {
            var info = new HashMap<String, Object>();
            info.put("threadName", entry.getKey());
            info.put("url", entry.getValue().url());
            info.put("startTime", entry.getValue().startTime());
            crawlers.add(info);
        }
        status.put("activeCrawlers", crawlers);
        return status;
    }
}
