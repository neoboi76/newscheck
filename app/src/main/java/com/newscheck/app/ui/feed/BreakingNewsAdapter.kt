package com.newscheck.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.newscheck.app.R
import com.newscheck.app.data.model.Article
import com.newscheck.app.databinding.ItemBreakingBinding
import com.newscheck.app.utils.DateUtils

class BreakingNewsAdapter(
    private val onArticleClick: (Article) -> Unit
) : ListAdapter<Article, BreakingNewsAdapter.BreakingViewHolder>(BreakingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakingViewHolder {
        val binding = ItemBreakingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BreakingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BreakingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BreakingViewHolder(
        private val binding: ItemBreakingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
            binding.tvTitle.text = article.title
            binding.tvSource.text = article.sourceName ?: "Unknown"
            binding.tvTime.text = DateUtils.formatRelative(article.publishedAt)
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
}

class BreakingDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
}