package com.example.studyplanner.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentRankingBinding
import com.google.android.material.tabs.TabLayout

class RankingFragment : Fragment() {

    private lateinit var binding: FragmentRankingBinding
    private lateinit var viewModel: RankingViewModel
    private lateinit var adapter: RankingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(RankingViewModel::class.java)

        adapter = RankingAdapter { userId ->
            // PVP 도전
            viewModel.challengeUser(userId)
        }

        binding.rankingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.rankingRecyclerView.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadDailyRanking()
                    1 -> viewModel.loadSeasonRanking()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        observeViewModel()
        viewModel.loadDailyRanking()
    }

    private fun observeViewModel() {
        viewModel.rankings.observe(viewLifecycleOwner) { rankings ->
            adapter.submitList(rankings)
        }
    }
}