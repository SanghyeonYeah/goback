package com.example.studyplanner.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: 프로필 화면 구현
        // - 사용자 정보 표시 (이름, 학년, 디플로마)
        // - 총 점수, 정확도, 풀이한 문제 수 표시
        // - 과목별 성과 표시
        // - 설정 버튼 (학습 시간, 면학 시간 수정)
        // - 로그아웃 버튼
    }
}