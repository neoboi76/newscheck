package com.newscheck.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.newscheck.app.R
import com.newscheck.app.data.model.NewsCategory
import com.newscheck.app.data.model.UserResponse
import com.newscheck.app.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadProfile()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ProfileState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.profileContent.isVisible = false
                    binding.notLoggedIn.isVisible = false
                }
                is ProfileViewModel.ProfileState.Success -> {
                    binding.progressBar.isVisible = false
                    binding.profileContent.isVisible = true
                    binding.subscriptionsSection.isVisible = true
                    binding.notLoggedIn.isVisible = false
                    bindProfile(state.user)
                }
                is ProfileViewModel.ProfileState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.profileContent.isVisible = false
                    binding.notLoggedIn.isVisible = true
                }
            }
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
        }
    }

    private fun bindProfile(user: UserResponse) {
        binding.tvUsername.text = user.username
        binding.tvEmail.text = user.email
        binding.tvInitial.text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        binding.tvSubscriptionCount.text = "${user.subscriptions.size} topics subscribed"

        binding.chipGroupSubscriptions.removeAllViews()
        if (user.subscriptions.isEmpty()) {
            binding.tvNoSubs.isVisible = true
        } else {
            binding.tvNoSubs.isVisible = false
            user.subscriptions.forEach { slug ->
                val category = NewsCategory.fromSlug(slug)
                val chip = Chip(requireContext()).apply {
                    text = "${category.emoji} ${category.displayName}"
                    isClickable = false
                    setChipBackgroundColorResource(R.color.surface_dark)
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    chipStrokeWidth = 1f
                    setChipStrokeColorResource(R.color.green_primary)
                }
                binding.chipGroupSubscriptions.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}