package com.example.mooruckapp.ui.plant

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mooruckapp.databinding.BottomSheetPlantSearchBinding
import com.example.mooruckapp.network.dto.PlantItem
import com.example.mooruckapp.repository.PlantRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlantSearchBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val SEARCH_DEBOUNCE_TIME = 500L

        const val REQUEST_KEY_PLANT_SELECTED =
            "request_key_plant_selected"

        const val BUNDLE_KEY_CONTENT_NO =
            "bundle_key_content_no"

        const val BUNDLE_KEY_PLANT_NAME =
            "bundle_key_plant_name"
    }

    private val repository = PlantRepository()

    private var searchJob: Job? = null

    private val plantSearchAdapter = PlantSearchAdapter { selectedPlant ->
        onPlantSelected(selectedPlant)
    }

    private var _binding: BottomSheetPlantSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetPlantSearchBinding.inflate(
            inflater,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {
        binding.recyclerViewPlants.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerViewPlants.adapter = plantSearchAdapter

        binding.editTextSearch.doAfterTextChanged { editable ->

            val keyword = editable
                ?.toString()
                ?.trim()
                .orEmpty()

            searchJob?.cancel()

            if (keyword.isBlank()) {
                plantSearchAdapter.submitList(emptyList())

                binding.progressBarSearch.visibility = View.GONE
                binding.textViewEmptyResult.visibility = View.GONE

                return@doAfterTextChanged
            }

            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(SEARCH_DEBOUNCE_TIME)
                searchPlants(keyword)
            }
        }
    }

    private suspend fun searchPlants(keyword: String) {
        binding.progressBarSearch.visibility = View.VISIBLE
        binding.textViewEmptyResult.visibility = View.GONE

        try {
            val result = repository.searchPlants(keyword)

            if (_binding == null) return

            val currentKeyword = binding.editTextSearch.text
                .toString()
                .trim()

            if (currentKeyword != keyword) return

            val plants = result.plants

            Log.d(
                "PlantSearch",
                "검색어=$keyword, 결과 수=${plants.size}, 결과=${plants.map { it.title }}"
            )

            plantSearchAdapter.submitList(plants)

            binding.textViewEmptyResult.visibility =
                if (plants.isEmpty()) View.VISIBLE else View.GONE

        } catch (exception: CancellationException) {
            throw exception

        } catch (exception: Exception) {
            Log.e("PlantSearch", "식물 검색 실패", exception)

            if (_binding != null) {
                plantSearchAdapter.submitList(emptyList())

                Toast.makeText(
                    requireContext(),
                    "식물 검색에 실패했습니다.",
                    Toast.LENGTH_LONG,
                ).show()
            }

        } finally {
            if (_binding != null) {
                binding.progressBarSearch.visibility = View.GONE
            }
        }
    }

    private fun onPlantSelected(selectedPlant: PlantItem) {

        val result = Bundle().apply {

            // 상세 API 호출에 사용할 식물 번호
            putString(
                BUNDLE_KEY_CONTENT_NO,
                selectedPlant.contentNo,
            )

            // 등록 화면에 바로 표시할 식물 이름
            putString(
                BUNDLE_KEY_PLANT_NAME,
                selectedPlant.title,
            )
        }

        // 등록 화면으로 선택 결과 전달
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_PLANT_SELECTED,
            result,
        )

        // BottomSheet 닫기
        dismiss()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        searchJob = null

        binding.recyclerViewPlants.adapter = null

        super.onDestroyView()

        _binding = null
    }
}