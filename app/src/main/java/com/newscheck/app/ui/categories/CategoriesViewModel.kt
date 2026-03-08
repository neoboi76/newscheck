package com.newscheck.app.ui.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newscheck.app.data.model.Article
import com.newscheck.app.data.model.NewsCategory
import com.newscheck.app.data.repository.ArticleRepository
import com.newscheck.app.data.repository.UserRepository
import com.newscheck.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _articlesState = MutableLiveData<ArticlesState>()
    val articlesState: LiveData<ArticlesState> = _articlesState

    private val _subscriptions = MutableLiveData<Set<String>>()
    val subscriptions: LiveData<Set<String>> = _subscriptions

    private val _subscribeResult = MutableLiveData<String?>()
    val subscribeResult: LiveData<String?> = _subscribeResult

    private var currentCategory: String = NewsCategory.GENERAL.slug
    private var currentPage = 0
    private var isLastPage = false
    private val allArticles = mutableListOf<Article>()

    init {
        loadSubscriptions()
        loadCategory(NewsCategory.GENERAL.slug)
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            when (val result = userRepository.getSubscriptions()) {
                is Result.Success -> _subscriptions.value = result.data.toSet()
                else -> _subscriptions.value = emptySet()
            }
        }
    }

    fun loadCategory(category: String, refresh: Boolean = false) {
        if (category != currentCategory || refresh) {
            currentCategory = category
            currentPage = 0
            isLastPage = false
            allArticles.clear()
        }
        if (isLastPage) return
        _articlesState.value = if (currentPage == 0) ArticlesState.Loading else ArticlesState.LoadingMore

        viewModelScope.launch {
            when (val result = articleRepository.getByCategory(category, currentPage)) {
                is Result.Success -> {
                    allArticles.addAll(result.data.content)
                    isLastPage = result.data.last
                    currentPage++
                    _articlesState.value = ArticlesState.Success(allArticles.toList(), !isLastPage)
                }
                is Result.Error -> _articlesState.value = ArticlesState.Error(result.message)
                else -> {}
            }
        }
    }

    fun toggleSubscription(category: String) {
        val current = _subscriptions.value ?: emptySet()
        viewModelScope.launch {
            if (category in current) {
                when (userRepository.unsubscribe(category)) {
                    is Result.Success -> {
                        _subscriptions.value = current - category
                        _subscribeResult.value = "Unsubscribed from $category"
                    }
                    is Result.Error -> _subscribeResult.value = "Login to manage subscriptions"
                    else -> {}
                }
            } else {
                when (userRepository.subscribe(category)) {
                    is Result.Success -> {
                        _subscriptions.value = current + category
                        _subscribeResult.value = "Subscribed to $category"
                    }
                    is Result.Error -> _subscribeResult.value = "Login to manage subscriptions"
                    else -> {}
                }
            }
        }
    }

    sealed class ArticlesState {
        object Loading : ArticlesState()
        object LoadingMore : ArticlesState()
        data class Success(val articles: List<Article>, val hasMore: Boolean) : ArticlesState()
        data class Error(val message: String) : ArticlesState()
    }
}