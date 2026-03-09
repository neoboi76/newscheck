package com.newscheck.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.newscheck.app.R
import com.newscheck.app.data.model.Article
import com.newscheck.app.databinding.ItemArticleBinding
import com.newscheck.app.databinding.ItemArticleFeaturedBinding
import com.newscheck.app.utils.DateUtils

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit
) : ListAdapter<Article, RecyclerView.ViewHolder>(ArticleDiffCallback()) {

    companion object {
        private const val TYPE_FEATURED = 0
        private const val TYPE_NORMAL   = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0 && getItem(0).imageUrl != null) TYPE_FEATURED else TYPE_NORMAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FEATURED) {
            FeaturedViewHolder(ItemArticleFeaturedBinding.inflate(inflater, parent, false))
        } else {
            ArticleViewHolder(ItemArticleBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = getItem(position)
        when (holder) {
            is FeaturedViewHolder -> holder.bind(article)
            is ArticleViewHolder  -> holder.bind(article)
        }
    }

    inner class FeaturedViewHolder(
        private val binding: ItemArticleFeaturedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
            binding.tvTitle.text = article.title
            binding.tvSource.text = article.sourceName ?: "Unknown"
            binding.tvTime.text = DateUtils.formatRelative(article.publishedAt)
            binding.tvCategory.text = article.category.replaceFirstChar { it.uppercase() }
            binding.chipBreaking.isVisible = article.breaking
            binding.ivReadIndicator.isVisible = article.read
            article.imageUrl?.let { url ->
                binding.ivThumbnail.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_image)
                    error(R.drawable.placeholder_image)
                }
            } ?: binding.ivThumbnail.setImageResource(R.drawable.placeholder_image)
            binding.root.setOnClickListener { onArticleClick(article) }
        }
    }

    inner class ArticleViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
            binding.tvTitle.text = article.title
            binding.tvSource.text = article.sourceName ?: "Unknown"
            binding.tvTime.text = DateUtils.formatRelative(article.publishedAt)
            binding.tvDescription.text = article.description ?: ""
            binding.tvDescription.isVisible = !article.description.isNullOrBlank()
            binding.chipBreaking.isVisible = article.breaking
            binding.ivReadIndicator.isVisible = article.read
            article.imageUrl?.let { url ->
                binding.ivThumbnail.isVisible = true
                binding.ivThumbnail.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_image)
                    error(R.drawable.placeholder_image)
                }
            } ?: run {
                binding.ivThumbnail.isVisible = false
            }
            binding.root.setOnClickListener { onArticleClick(article) }
        }
    }
}

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
}