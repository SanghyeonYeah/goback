package com.example.studyplanner.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener {
            logout()
        }

        binding.profileButton.setOnClickListener {
            Toast.makeText(requireContext(), "프로필 수정 기능", Toast.LENGTH_SHORT).show()
        }

        binding.notificationButton.setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정", Toast.LENGTH_SHORT).show()
        }

        binding.aboutButton.setOnClickListener {
            Toast.makeText(requireContext(), "앱 정보", Toast.LENGTH_SHORT).show()
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun logout() {
        // SharedPreferences에서 토큰 삭제
        Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
    }
}