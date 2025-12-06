package com.cnsa.studyplanner.ui.ranking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cnsa.studyplanner.databinding.FragmentRankingBinding
import com.cnsa.studyplanner.data.model.SeasonRanking
import com.cnsa.studyplanner.ui.pvp.PvpWaitingActivity
import kotlinx.coroutines.launch

class RankingFragment : Fragment() {
    
    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RankingViewModel by viewModels()
    private lateinit var rankingAdapter: RankingAdapter
    
    private var currentTab = TAB_DAILY
    
    companion object {
        private const val TAB_DAILY = "daily"
        private const val TAB_SEASON = "season"
        private const val ARG_TYPE = "type"
        
        fun newInstance(type: String = TAB_DAILY) = RankingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TYPE, type)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        currentTab = arguments?.getString(ARG_TYPE) ?: TAB_DAILY
        
        setupViews()
        setupObservers()
        loadData()
    }
    
    private fun setupViews() {
        // 탭 전환
        binding.btnDailyRanking.setOnClickListener {
            switchTab(TAB_DAILY)
        }
        
        binding.btnSeasonRanking.setOnClickListener {
            switchTab(TAB_SEASON)
        }
        
        // 초기 탭 설정
        updateTabUI()
        
        // 랭킹 RecyclerView 설정
        setupRankingRecyclerView()
    }
    
    private fun setupRankingRecyclerView() {
        rankingAdapter = RankingAdapter(
            onUserClick = { ranking ->
                // PVP 도전
                showPvpChallenge(ranking)
            }
        )
        
        binding.rvRanking.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
        }
    }
    
    private fun setupObservers() {
        // 일일 랭킹
        viewModel.dailyRankings.observe(viewLifecycleOwner) { rankings ->
            if (currentTab == TAB_DAILY) {
                rankingAdapter.submitList(rankings)
                updateTopThree(rankings)
            }
        }
        
        // 시즌 랭킹
        viewModel.seasonRankings.observe(viewLifecycleOwner) { rankings ->
            if (currentTab == TAB_SEASON) {
                rankingAdapter.submitList(rankings)
                updateTopThree(rankings)
            }
        }
        
        // 로딩 상태
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 내 랭킹
        viewModel.userRanking.observe(viewLifecycleOwner) { ranking ->
            ranking?.let {
                binding.tvMyRank.text = "${it.rankPosition}위"
                binding.tvMyPoints.text = "${it.totalPoints}점"
            }
        }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            val seasonId = viewModel.getActiveSeasonId()
            if (seasonId != null) {
                when (currentTab) {
                    TAB_DAILY -> viewModel.loadDailyRanking(seasonId)
                    TAB_SEASON -> viewModel.loadSeasonRanking(seasonId)
                }
            }
        }
    }
    
    private fun switchTab(tab: String) {
        if (currentTab != tab) {
            currentTab = tab
            updateTabUI()
            loadData()
        }
    }
    
    private fun updateTabUI() {
        when (currentTab) {
            TAB_DAILY -> {
                binding.btnDailyRanking.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.btnSeasonRanking.setBackgroundResource(R.drawable.bg_tab_unselected)
            }
            TAB_SEASON -> {
                binding.btnDailyRanking.setBackgroundResource(R.drawable.bg_tab_unselected)
                binding.btnSeasonRanking.setBackgroundResource(R.drawable.bg_tab_selected)
            }
        }
    }
    
    private fun updateTopThree(rankings: List<SeasonRanking>) {
        if (rankings.isEmpty()) {
            binding.layoutTopThree.visibility = View.GONE
            return
        }
        
        binding.layoutTopThree.visibility = View.VISIBLE
        
        // 1위
        if (rankings.isNotEmpty()) {
            binding.tvFirst.text = rankings[0].username
            binding.tvFirstPoints.text = "${rankings[0].totalPoints}점"
        }
        
        // 2위
        if (rankings.size > 1) {
            binding.tvSecond.text = rankings[1].username
            binding.tvSecondPoints.text = "${rankings[1].totalPoints}점"
        }
        
        // 3위
        if (rankings.size > 2) {
            binding.tvThird.text = rankings[2].username
            binding.tvThirdPoints.text = "${rankings[2].totalPoints}점"
        }
    }
    
    private fun showPvpChallenge(ranking: SeasonRanking) {
        // PVP 도전 확인 다이얼로그
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("PVP 도전")
            .setMessage("${ranking.username}님에게 PVP 도전하시겠습니까?")
            .setPositiveButton("도전") { _, _ ->
                startPvpMatch(ranking.userId)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun startPvpMatch(opponentId: Int) {
        val intent = Intent(requireContext(), PvpWaitingActivity::class.java)
        intent.putExtra("opponent_id", opponentId)
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// RankingAdapter.kt
class RankingAdapter(
    private val onUserClick: (SeasonRanking) -> Unit
) : androidx.recyclerview.widget.ListAdapter<SeasonRanking, RankingAdapter.ViewHolder>(
    RankingDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }
    
    inner class ViewHolder(
        private val binding: ItemRankingBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(ranking: SeasonRanking, position: Int) {
            binding.tvRank.text = "${ranking.rankPosition}위"
            binding.tvUsername.text = ranking.username
            binding.tvPoints.text = "${ranking.totalPoints}점"
            
            // 순위 변동 표시
            when {
                ranking.rankChange > 0 -> {
                    binding.tvRankChange.text = "+${ranking.rankChange}"
                    binding.tvRankChange.setTextColor(
                        binding.root.context.getColor(android.R.color.holo_green_dark)
                    )
                    binding.tvRankChange.visibility = View.VISIBLE
                }
                ranking.rankChange < 0 -> {
                    binding.tvRankChange.text = "${ranking.rankChange}"
                    binding.tvRankChange.setTextColor(
                        binding.root.context.getColor(android.R.color.holo_red_dark)
                    )
                    binding.tvRankChange.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvRankChange.visibility = View.GONE
                }
            }
            
            // 상위 3위 배경색 강조
            when (ranking.rankPosition) {
                1 -> binding.root.setBackgroundResource(R.drawable.bg_rank_first)
                2 -> binding.root.setBackgroundResource(R.drawable.bg_rank_second)
                3 -> binding.root.setBackgroundResource(R.drawable.bg_rank_third)
                else -> binding.root.setBackgroundResource(R.drawable.bg_rank_normal)
            }
            
            binding.root.setOnClickListener {
                onUserClick(ranking)
            }
        }
    }
}

class RankingDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<SeasonRanking>() {
    override fun areItemsTheSame(oldItem: SeasonRanking, newItem: SeasonRanking) =
        oldItem.userId == newItem.userId
    
    override fun areContentsTheSame(oldItem: SeasonRanking, newItem: SeasonRanking) =
        oldItem == newItem
}

// RankingViewModel.kt
class RankingViewModel : androidx.lifecycle.ViewModel() {
    private val _dailyRankings = androidx.lifecycle.MutableLiveData<List<SeasonRanking>>()
    val dailyRankings: androidx.lifecycle.LiveData<List<SeasonRanking>> = _dailyRankings
    
    private val _seasonRankings = androidx.lifecycle.MutableLiveData<List<SeasonRanking>>()
    val seasonRankings: androidx.lifecycle.LiveData<List<SeasonRanking>> = _seasonRankings
    
    private val _userRanking = androidx.lifecycle.MutableLiveData<SeasonRanking>()
    val userRanking: androidx.lifecycle.LiveData<SeasonRanking> = _userRanking
    
    private val _isLoading = androidx.lifecycle.MutableLiveData<Boolean>()
    val isLoading: androidx.lifecycle.LiveData<Boolean> = _isLoading
    
    fun loadDailyRanking(seasonId: Int) {
        // Repository를 통해 일일 랭킹 로드
    }
    
    fun loadSeasonRanking(seasonId: Int) {
        // Repository를 통해 시즌 랭킹 로드
    }
    
    fun getActiveSeasonId(): Int? {
        // 현재 활성 시즌 ID 반환
        return null
    }
}
