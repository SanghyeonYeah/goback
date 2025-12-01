package com.example.studyplanner.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentSignupBinding
import com.example.studyplanner.model.SignupRequest

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var viewModel: LoginViewModel

    private val diplomaList = listOf(
        "IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)",
        "인문학", "국제어문", "사회과학", "경제경영", "IB(인문)",
        "예술", "체육"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        setupDiplomaSpinner()
        setupGradeSpinner()

        binding.signupButton.setOnClickListener {
            validateAndSignup()
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        observeViewModel()
    }

    private fun setupDiplomaSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, diplomaList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.diplomaSpinner.adapter = adapter
    }

    private fun setupGradeSpinner() {
        val gradeList = listOf("1학년", "2학년", "3학년")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, gradeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.gradeSpinner.adapter = adapter
    }

    private fun validateAndSignup() {
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        val studentId = binding.studentIdEditText.text.toString().trim()
        val diploma = binding.diplomaSpinner.selectedItem.toString()
        val gradeText = binding.gradeSpinner.selectedItem.toString()
        val grade = gradeText.first().toString().toInt()

        when {
            username.isEmpty() -> {
                Toast.makeText(requireContext(), "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            password.isEmpty() -> {
                Toast.makeText(requireContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            password != confirmPassword -> {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
            studentId.isEmpty() -> {
                Toast.makeText(requireContext(), "학번을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val signupRequest = SignupRequest(
                    username = username,
                    password = password,
                    studentId = studentId,
                    diploma = diploma,
                    grade = grade
                )
                viewModel.signup(signupRequest)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.signupResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { authData ->
                Toast.makeText(requireContext(), "회원가입 성공", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
            }
            result.onFailure { exception ->
                Toast.makeText(requireContext(), "회원가입 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}