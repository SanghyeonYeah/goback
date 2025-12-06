package com.example.studyplanner.ui.ranking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.databinding.ItemRankingBinding
import com.example.studyplanner.model.DailyRanking
import com.example.studyplanner.model.SeasonRanking

sealed class RankingItem {
    data class DailyItem(val ranking: DailyRanking) : RankingItem()
    data class SeasonItem(val ranking: SeasonRanking) : RankingItem()
}

class RankingAdapter(
    private val onChallengeClick: (String) -> Unit
) : ListAdapter<RankingItem, RankingAdapter.RankingViewHolder>(RankingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding, onChallengeClick)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RankingViewHolder(
        private val binding: ItemRankingBinding,
        private val onChallengeClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RankingItem) {
            when (item) {
                is RankingItem.DailyItem -> {
                    binding.rankTextView.text = "${item.ranking.rank}위"
                    binding.userIdTextView.text = item.ranking.userId
                    binding.scoreTextView.text = "${item.ranking.dailyScore}점"
                    binding.challengeButton.setOnClickListener {
                        onChallengeClick(item.ranking.userId)
                    }
                }
                is RankingItem.SeasonItem -> {
                    binding.rankTextView.text = "${item.ranking.rank}위"
                    binding.userIdTextView.text = item.ranking.userId
                    binding.scoreTextView.text = "${item.ranking.totalScore}점"
                    binding.challengeButton.setOnClickListener {
                        onChallengeClick(item.ranking.userId)
                    }
                }
            }
        }
    }

    class RankingDiffCallback : DiffUtil.ItemCallback<RankingItem>() {
        override fun areItemsTheSame(oldItem: RankingItem, newItem: RankingItem): Boolean {
            return when {
                oldItem is RankingItem.DailyItem && newItem is RankingItem.DailyItem ->
                    oldItem.ranking.userId == newItem.ranking.userId
                oldItem is RankingItem.SeasonItem && newItem is RankingItem.SeasonItem ->
                    oldItem.ranking.userId == newItem.ranking.userId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: RankingItem, newItem: RankingItem): Boolean {
            return oldItem == newItem
        }
    }
}