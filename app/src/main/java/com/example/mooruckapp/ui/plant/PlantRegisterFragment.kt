package com.example.mooruckapp.ui.plant

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.mooruckapp.R
import com.example.mooruckapp.databinding.FragmentPlantRegisterBinding

class PlantRegisterFragment : Fragment(R.layout.fragment_plant_register) {

    // XML의 View에 접근하기 위한 Binding 객체
    private var _binding: FragmentPlantRegisterBinding? = null

    // 화면이 존재하는 동안 Binding을 편하게 사용하기 위한 프로퍼티
    private val binding get() = _binding!!

    // 사용자가 선택한 이미지의 위치를 저장하는 변수
    private var selectedImageUri: Uri? = null

    // 갤러리를 열고 사용자가 선택한 이미지 결과를 받아오는 객체
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->

            // 이미지를 선택하지 않고 갤러리를 닫으면 imageUri가 null이 됨
            if (imageUri != null) {

                // 선택한 이미지의 위치를 변수에 저장
                selectedImageUri = imageUri

                // 선택한 이미지를 프로필 ImageView에 표시
                binding.ivPlantProfile.setImageURI(imageUri)
            }
        }

    // Fragment의 XML 화면이 생성된 후 호출되는 함수
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 Fragment의 View와 ViewBinding을 연결
        _binding = FragmentPlantRegisterBinding.bind(view)

        // 화면의 클릭 이벤트 연결
        setupClickListeners()
    }

    // 화면에서 사용하는 클릭 이벤트를 설정하는 함수
    private fun setupClickListeners() {

        // 프로필 이미지 선택 버튼 클릭
        binding.btnSelectImage.setOnClickListener {

            // 이미지 파일만 선택할 수 있도록 갤러리를 실행
            imagePickerLauncher.launch("image/*")
        }

        // 식물 검색 버튼 클릭
        binding.btnSearchPlant.setOnClickListener {
        }

        // 직접 등록하기 버튼 클릭
        binding.btnManualInput.setOnClickListener {
        }

        // 심은 날짜 영역 클릭
        binding.tvPlantedDate.setOnClickListener {
        }

        // 마지막 물 준 날짜 영역 클릭
        binding.tvLastWateredDate.setOnClickListener {
        }

        // 식물 등록 버튼 클릭
        binding.btnRegisterPlant.setOnClickListener {
        }
    }

    // Fragment의 XML 화면이 제거될 때 호출되는 함수
    override fun onDestroyView() {
        super.onDestroyView()

        // 제거된 화면을 Binding이 계속 참조하지 않도록 해제
        _binding = null
    }
}