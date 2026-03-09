package com.newscheck.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.newscheck.app.R
import com.newscheck.app.data.model.Article
import com.newscheck.app.databinding.FragmentFeedBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var breakingAdapter: BreakingNewsAdapter
    private var isSearchMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupSearch()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.loadBreaking()
    }

    private fun setupRecyclerViews() {
        articleAdapter = ArticleAdapter { article -> navigateToDetail(article) }
        breakingAdapter = BreakingNewsAdapter { article -> navigateToDetail(article) }

        binding.rvFeed.apply {
            adapter = articleAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(rv, dx, dy)
                    val lm = rv.layoutManager as LinearLayoutManager
                    val total = lm.itemCount
                    val last = lm.findLastVisibleItemPosition()
                    if (!isSearchMode && last >= total - 3) {
                        viewModel.loadFeed()
                    }
                }
            })
        }

        binding.rvBreaking.apply {
            adapter = breakingAdapter
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    isSearchMode = it.isNotBlank()
                    if (isSearchMode) viewModel.search(it)
                    else viewModel.loadFeed(refresh = true)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    isSearchMode = false
                    viewModel.loadFeed(refresh = true)
                }
                return false
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.green_primary)
            setBackgroundResource(R.color.background_dark)
            setOnRefreshListener {
                isSearchMode = false
                viewModel.loadFeed(refresh = true)
                viewModel.loadBreaking()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.feedState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedViewModel.FeedState.Loading -> {
                    binding.shimmerLayout.isVisible = true
                    binding.shimmerLayout.startShimmer()
                    binding.rvFeed.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmpty.isVisible = false
                }
                is FeedViewModel.FeedState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    binding.rvFeed.isVisible = true
                    binding.swipeRefresh.isRefreshing = false
                    articleAdapter.submitList(state.articles)
                    binding.tvEmpty.isVisible = state.articles.isEmpty()
                }
                is FeedViewModel.FeedState.LoadingMore -> {
                    binding.swipeRefresh.isRefreshing = false
                }
                is FeedViewModel.FeedState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmpty.isVisible = true
                    binding.tvEmpty.text = state.message
                }
            }
        }

        viewModel.breakingNews.observe(viewLifecycleOwner) { articles ->
            val hasBreaking = articles.isNotEmpty()
            binding.breakingSection.isVisible = hasBreaking
            if (hasBreaking) breakingAdapter.submitList(articles)
        }

        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedViewModel.SearchState.Loading -> {
                    binding.shimmerLayout.isVisible = true
                    binding.shimmerLayout.startShimmer()
                }
                is FeedViewModel.SearchState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    articleAdapter.submitList(state.articles)
                    binding.tvEmpty.isVisible = state.articles.isEmpty()
                    binding.tvEmpty.text = getString(R.string.no_results)
                }
                is FeedViewModel.SearchState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToDetail(article: Article) {
        viewModel.markRead(article.id)
        val action = FeedFragmentDirections.actionFeedFragmentToArticleDetailFragment(article)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}