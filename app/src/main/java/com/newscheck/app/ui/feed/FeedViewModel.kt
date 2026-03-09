package com.newscheck.app.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newscheck.app.data.model.Article
import com.newscheck.app.data.model.PagedResponse
import com.newscheck.app.data.repository.ArticleRepository
import com.newscheck.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _feedState = MutableLiveData<FeedState>()
    val feedState: LiveData<FeedState> = _feedState

    private val _breakingNews = MutableLiveData<List<Article>>()
    val breakingNews: LiveData<List<Article>> = _breakingNews

    private val _searchState = MutableLiveData<SearchState>()
    val searchState: LiveData<SearchState> = _searchState

    private var currentPage = 0
    private var isLastPage = false
    private val allArticles = mutableListOf<Article>()

    init { loadFeed() }

    fun loadFeed(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            isLastPage = false
            allArticles.clear()
        }
        if (isLastPage && !refresh) return
        if (_feedState.value is FeedState.LoadingMore) return

        _feedState.value = if (currentPage == 0) FeedState.Loading else FeedState.LoadingMore

        viewModelScope.launch {
            when (val result = articleRepository.getFeed(currentPage)) {
                is Result.Success -> {
                    allArticles.addAll(result.data.content)
                    isLastPage = result.data.last
                    currentPage++
                    _feedState.value = FeedState.Success(
                        articles = allArticles.toList(),
                        hasMore = !isLastPage
                    )
                }
                is Result.Error -> loadPublicFeed(refresh)
                else -> {}
            }
        }
    }

    private suspend fun loadPublicFeed(refresh: Boolean) {
        when (val result = articleRepository.getAllArticles(if (refresh) 0 else currentPage)) {
            is Result.Success -> {
                allArticles.addAll(result.data.content)
                isLastPage = result.data.last
                currentPage++
                _feedState.value = FeedState.Success(allArticles.toList(), !isLastPage)
            }
            is Result.Error -> _feedState.value = FeedState.Error(result.message)
            else -> {}
        }
    }

    fun loadBreaking() {
        viewModelScope.launch {
            when (val result = articleRepository.getBreaking()) {
                is Result.Success -> _breakingNews.value = result.data
                else -> {}
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _searchState.value = SearchState.Loading
        viewModelScope.launch {
            when (val result = articleRepository.search(query)) {
                is Result.Success -> _searchState.value = SearchState.Success(result.data.content)
                is Result.Error   -> _searchState.value = SearchState.Error(result.message)
                else -> {}
            }
        }
    }

    fun markRead(articleId: Long) {
        viewModelScope.launch { articleRepository.markRead(articleId) }
    }

    sealed class FeedState {
        object Loading : FeedState()
        object LoadingMore : FeedState()
        data class Success(val articles: List<Article>, val hasMore: Boolean) : FeedState()
        data class Error(val message: String) : FeedState()
    }

    sealed class SearchState {
        object Loading : SearchState()
        data class Success(val articles: List<Article>) : SearchState()
        data class Error(val message: String) : SearchState()
    }
}