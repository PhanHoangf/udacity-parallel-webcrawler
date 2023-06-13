package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CrawResultTask extends RecursiveTask<CrawlResult> {
    private final Map<String, Integer> wordCounts;
    private final List<String> urlsVisited;
    private final String url;
    private final Integer depth;
    private Integer currentDepth;
    private final PageParserFactory pageParserFactory;
    private final int popularWordCount;
    private final List<Pattern> ignoredUrls;

    public CrawResultTask(
            String url,
            List<String> urlsVisited,
            Map<String, Integer> wordCounts,
            int depth,
            PageParserFactory pageParserFactory,
            int popularWordCount,
            List<Pattern> ignoredUrls
    ) {
        this.url = url;
        this.urlsVisited = urlsVisited;
        this.wordCounts = wordCounts;
        this.depth = depth;
        this.pageParserFactory = pageParserFactory;
        this.popularWordCount = popularWordCount;
        this.ignoredUrls = ignoredUrls;
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

        if (urlsVisited.contains(url)) {
            return null;
        }

        urlsVisited.add(url);

        List<CrawResultTask> subtasks = new ArrayList<>();
        PageParser.Result result = pageParserFactory.get(url).parse();
        Map<String, Integer> counts = result.getWordCounts();

        counts.forEach((word, count) -> {
            wordCounts.merge(word, count, Integer::sum);
        });

        List<String> subUrls = result.getLinks();

        for (String subUrl : subUrls) {
            subtasks.add(new CrawResultTask(subUrl, urlsVisited, wordCounts, depth - 1, pageParserFactory, popularWordCount, ignoredUrls));
        }

        invokeAll(subtasks);

        return new CrawlResult.Builder().setWordCounts(wordCounts)
                .setUrlsVisited(urlsVisited.size())
                .build();
    }

}
