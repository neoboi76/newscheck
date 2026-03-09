package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;

import java.util.List;

// Interface for news source clients (OCP/DIP — add new sources without changing scheduler)
public interface NewsSourceClient {

    String getSourceName();

    List<ArticleEvent> fetchAll();
}

