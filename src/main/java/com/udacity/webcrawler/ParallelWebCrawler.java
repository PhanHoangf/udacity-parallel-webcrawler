package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;

    @Inject
    PageParserFactory pageParserFactory;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @MaxDepth int maxDepth,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> ignoredUrls
    ) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        List<String> urlsVisited = Collections.synchronizedList(new ArrayList<>());
        Map<String, Integer> wordCounts = Collections.synchronizedMap(new HashMap<>());

        if (startingUrls.isEmpty()) {
            return new CrawlResult.Builder().build();
        } else {
            for (String url : startingUrls) {
                pool.invoke(new CrawResultTask(url, urlsVisited, wordCounts, maxDepth, pageParserFactory, popularWordCount, ignoredUrls));
            }
        }

        if (!wordCounts.isEmpty()) {
            wordCounts = WordCounts.sort(wordCounts, popularWordCount);
        }

        return new CrawlResult.Builder().setWordCounts(wordCounts)
                .setUrlsVisited(urlsVisited.size())
                .build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
