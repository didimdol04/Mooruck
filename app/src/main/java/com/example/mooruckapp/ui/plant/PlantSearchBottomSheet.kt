package com.example.mooruckapp.ui.plant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mooruckapp.databinding.BottomSheetPlantSearchBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlantSearchBottomSheet : BottomSheetDialogFragment() {

    // ViewBinding
    private var _binding: BottomSheetPlantSearchBinding? = null
    private val binding get() = _binding!!

    // 화면 생성
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = BottomSheetPlantSearchBinding.inflate(inflater, container, false)

        return binding.root
    }

    // 화면 생성 완료
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {

        // TODO
        // RecyclerView 설정
        // 검색 이벤트 연결

    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}