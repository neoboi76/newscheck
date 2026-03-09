package com.newscheck.app.ui.article

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.newscheck.app.R
import com.newscheck.app.data.model.NewsCategory
import com.newscheck.app.databinding.FragmentArticleDetailBinding
import com.newscheck.app.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArticleDetailFragment : Fragment() {

    private var _binding: FragmentArticleDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ArticleDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val article = args.article

        binding.apply {
            tvTitle.text = article.title
            tvDescription.text = article.description ?: ""
            tvSource.text = article.sourceName ?: "Unknown Source"
            tvAuthor.text = article.author?.let { "By $it" } ?: ""
            tvDate.text = DateUtils.formatFull(article.publishedAt)
            tvCategory.text = NewsCategory.fromSlug(article.category).let {
                "${it.emoji} ${it.displayName}"
            }

            article.imageUrl?.let { url ->
                ivHero.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_image)
                }
            } ?: run {
                ivHero.setImageResource(R.drawable.placeholder_image)
            }

            btnBack.setOnClickListener { findNavController().navigateUp() }

            btnReadFull.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    startActivity(intent)
                } catch (e: Exception) { }
            }

            btnShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.url}")
                }
                startActivity(Intent.createChooser(shareIntent, "Share article"))
            }

            if (article.breaking) {
                chipBreaking.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}