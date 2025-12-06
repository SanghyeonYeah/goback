package com.cnsa.studyplanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cnsa.studyplanner.databinding.ActivityMainBinding
import com.cnsa.studyplanner.ui.calendar.CalendarFragment
import com.cnsa.studyplanner.ui.home.HomeFragment
import com.cnsa.studyplanner.ui.plan.PlanFragment
import com.cnsa.studyplanner.ui.profile.ProfileFragment
import com.cnsa.studyplanner.ui.ranking.RankingFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        // 초기 Fragment 설정
        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance())
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ranking -> {
                    loadFragment(RankingFragment.newInstance())
                    true
                }
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment.newInstance())
                    true
                }
                R.id.nav_home -> {
                    loadFragment(HomeFragment.newInstance())
                    true
                }
                R.id.nav_plan -> {
                    loadFragment(PlanFragment.newInstance())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }
        
        // 홈을 기본 선택
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    fun navigateToPlan() {
        binding.bottomNavigation.selectedItemId = R.id.nav_plan
    }
}
