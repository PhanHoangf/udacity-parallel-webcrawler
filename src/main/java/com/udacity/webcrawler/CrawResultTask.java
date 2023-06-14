package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CrawResultTask extends RecursiveTask<CrawlResult> {
    private final Map<String, Integer> wordCounts;
    private final Set<String> urlsVisited;
    private final String url;
    private final Integer depth;
    private final PageParserFactory pageParserFactory;
    private final int popularWordCount;
    private final List<Pattern> ignoredUrls;

    private final Duration timeout;

    private CrawResultTask(
            String url,
            Set<String> urlsVisited,
            Map<String, Integer> wordCounts,
            int depth,
            PageParserFactory pageParserFactory,
            int popularWordCount,
            List<Pattern> ignoredUrls,
            Duration timeout
    ) {
        this.url = url;
        this.urlsVisited = urlsVisited;
        this.wordCounts = wordCounts;
        this.depth = depth;
        this.pageParserFactory = pageParserFactory;
        this.popularWordCount = popularWordCount;
        this.ignoredUrls = ignoredUrls;
        this.timeout = timeout;
    }

    public static final class Builder {
        private Map<String, Integer> wordCounts;
        private Set<String> urlsVisited;
        private String url;
        private Integer depth;
        private PageParserFactory pageParserFactory;
        private int popularWordCount;
        private List<Pattern> ignoredUrls;

        private Duration timeout;

        public Builder setWordCounts(Map<String, Integer> wordCounts) {
            this.wordCounts = wordCounts;
            return this;
        }

        public Builder setTimeOut(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setUrlsVisited(Set<String> urlsVisited) {
            this.urlsVisited = urlsVisited;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setDepth(Integer depth) {
            this.depth = depth;
            return this;
        }

        public Builder setPageParserFactory(PageParserFactory pageParserFactory) {
            this.pageParserFactory = pageParserFactory;
            return this;
        }

        public Builder setPopularWordCount(int popularWordCount) {
            this.popularWordCount = popularWordCount;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public CrawResultTask build() {
            return new CrawResultTask(url,
                    urlsVisited,
                    wordCounts,
                    depth,
                    pageParserFactory,
                    popularWordCount,
                    ignoredUrls,
                    timeout);
        }
    }

    @Override
    protected CrawlResult compute() {
        if (this.depth == 0 || url.isEmpty()) {
            return new CrawlResult.Builder().setWordCounts(new HashMap<>())
                    .build();
        }

        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return new CrawlResult.Builder().build();
            }
        }

        if (!urlsVisited.add(url)) {
            return null;
        }


        List<CrawResultTask> subtasks = new ArrayList<>();

        PageParser.Result result = pageParserFactory.get(url).parse();
        Map<String, Integer> counts = result.getWordCounts();

        counts.forEach((word, count) -> {
            wordCounts.merge(word, count, Integer::sum);
        });

        List<String> subUrls = result.getLinks();

        for (String subUrl : subUrls) {
            CrawResultTask task = new CrawResultTask.Builder().setUrl(subUrl)
                    .setUrlsVisited(urlsVisited)
                    .setDepth(depth - 1)
                    .setPageParserFactory(pageParserFactory)
                    .setPopularWordCount(popularWordCount)
                    .setIgnoredUrls(ignoredUrls)
                    .setWordCounts(wordCounts)
                    .setTimeOut(timeout)
                    .build();
            subtasks.add(task);
        }

        invokeAll(subtasks);

        return new CrawlResult.Builder().setWordCounts(wordCounts)
                .setUrlsVisited(urlsVisited.size())
                .build();
    }

}
