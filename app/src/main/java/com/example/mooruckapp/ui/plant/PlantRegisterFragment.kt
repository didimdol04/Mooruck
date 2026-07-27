package com.example.mooruckapp.ui.plant

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mooruckapp.R
import com.example.mooruckapp.databinding.FragmentPlantRegisterBinding

class PlantRegisterFragment : Fragment(R.layout.fragment_plant_register) {

    // 실제 Binding 객체를 저장하는 변수
    private var _binding: FragmentPlantRegisterBinding? = null

    // 화면이 살아 있는 동안 Binding을 편하게 사용하기 위한 변수
    private val binding get() = _binding!!

    // Fragment의 XML 화면이 생성된 직후 호출되는 함수
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 Fragment의 View와 Binding 객체를 연결
        _binding = FragmentPlantRegisterBinding.bind(view)

        // 화면의 기본 클릭 이벤트를 설정
        setupClickListeners()
    }

    // 화면에 있는 버튼들의 클릭 이벤트를 연결하는 함수
    private fun setupClickListeners() {

        // 프로필 이미지 선택 버튼 클릭
        binding.btnSelectImage.setOnClickListener {
            // 다음 단계에서 이미지 선택 기능 구현
        }

        // 식물 검색 버튼 클릭
        binding.btnSearchPlant.setOnClickListener {
            // 이후 API 검색 기능 구현
        }

        // 직접 등록하기 버튼 클릭
        binding.btnManualInput.setOnClickListener {
            // 이후 직접 입력 화면 동작 구현
        }

        // 심은 날짜 영역 클릭
        binding.tvPlantedDate.setOnClickListener {
            // 다음 단계에서 날짜 선택 기능 구현
        }

        // 마지막 물 준 날짜 영역 클릭
        binding.tvLastWateredDate.setOnClickListener {
            // 다음 단계에서 날짜 선택 기능 구현
        }

        // 식물 등록 버튼 클릭
        binding.btnRegisterPlant.setOnClickListener {
            // 이후 입력값 검증과 Room 저장 기능 구현
        }
    }

    // Fragment의 화면이 사라질 때 호출되는 함수
    override fun onDestroyView() {
        super.onDestroyView()

        // 이전 화면의 View를 계속 참조하지 않도록 Binding을 해제
        _binding = null
    }
}