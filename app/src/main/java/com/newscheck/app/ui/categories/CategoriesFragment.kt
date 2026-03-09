package com.newscheck.app.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.newscheck.app.R
import com.newscheck.app.data.model.Article
import com.newscheck.app.data.model.NewsCategory
import com.newscheck.app.databinding.FragmentCategoriesBinding
import com.newscheck.app.ui.feed.ArticleAdapter
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var articleAdapter: ArticleAdapter
    private var selectedCategory = NewsCategory.GENERAL.slug

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryChips()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupCategoryChips() {
        NewsCategory.values().forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = "${category.emoji} ${category.displayName}"
                isCheckable = true
                isChecked = category.slug == selectedCategory
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_selector)
                chipStrokeWidth = 1f
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_selector)

                setOnClickListener {
                    if (selectedCategory != category.slug) {
                        selectedCategory = category.slug
                        updateChipStates()
                        viewModel.loadCategory(category.slug)
                    }
                }

                setOnLongClickListener {
                    viewModel.toggleSubscription(category.slug)
                    true
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun updateChipStates() {
        for (i in 0 until binding.chipGroup.childCount) {
            val chip = binding.chipGroup.getChildAt(i) as? Chip ?: continue
            val category = NewsCategory.values()[i]
            chip.isChecked = category.slug == selectedCategory
        }
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter { article -> navigateToDetail(article) }
        binding.rvArticles.apply {
            adapter = articleAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    if (lm.findLastVisibleItemPosition() >= lm.itemCount - 3) {
                        viewModel.loadCategory(selectedCategory)
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.articlesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CategoriesViewModel.ArticlesState.Loading -> {
                    binding.shimmerLayout.isVisible = true
                    binding.shimmerLayout.startShimmer()
                    binding.rvArticles.isVisible = false
                }
                is CategoriesViewModel.ArticlesState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    binding.rvArticles.isVisible = true
                    articleAdapter.submitList(state.articles)
                    binding.tvEmpty.isVisible = state.articles.isEmpty()
                }
                is CategoriesViewModel.ArticlesState.LoadingMore -> {}
                is CategoriesViewModel.ArticlesState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.isVisible = false
                    binding.tvEmpty.isVisible = true
                    binding.tvEmpty.text = state.message
                }
            }
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) { subs ->
            updateSubscriptionIndicators(subs)
        }

        viewModel.subscribeResult.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateSubscriptionIndicators(subscriptions: Set<String>) {
        for (i in 0 until binding.chipGroup.childCount) {
            val chip = binding.chipGroup.getChildAt(i) as? Chip ?: continue
            val category = NewsCategory.values()[i]
            chip.isCloseIconVisible = category.slug in subscriptions
        }
    }

    private fun navigateToDetail(article: Article) {
        val action = CategoriesFragmentDirections
            .actionCategoriesFragmentToArticleDetailFragment(article)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}