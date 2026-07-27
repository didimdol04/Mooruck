package com.example.mooruckapp.ui.plant

import android.os.Bundle
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
import com.example.mooruckapp.ui.plant.adapter.PlantSearchAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlantSearchBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val SEARCH_DEBOUNCE_TIME = 500L

        // TODO: 인증키 추가 후 BuildConfig로 리팩토링
        const val REQUEST_KEY_PLANT_SELECTED = "request_key_plant_selected"
        const val BUNDLE_KEY_CONTENT_NO = "bundle_key_content_no"
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
        binding.recyclerViewPlants.setHasFixedSize(true)

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

        val apiKey = ""

        if (apiKey.isBlank()) {
            Toast.makeText(
                requireContext(),
                "API 인증키가 아직 등록되지 않았습니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        binding.progressBarSearch.visibility = View.VISIBLE
        binding.textViewEmptyResult.visibility = View.GONE

        try {
            val result = repository.searchPlants(
                apiKey = apiKey,
                keyword = keyword,
            )

            if (_binding == null) {
                return
            }

            val plants = result.response
                .body
                .items
                .item

            val currentKeyword = binding.editTextSearch.text
                .toString()
                .trim()

            if (currentKeyword != keyword) {
                return
            }

            plantSearchAdapter.submitList(plants)

            binding.textViewEmptyResult.visibility =
                if (plants.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (_binding != null) {
                Toast.makeText(
                    requireContext(),
                    "식물을 검색하는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            exception.printStackTrace()

        } finally {
            if (_binding != null) {
                binding.progressBarSearch.visibility = View.GONE
            }
        }
    }

    private fun onPlantSelected(selectedPlant: PlantItem) {
        val result = Bundle().apply {
            putString(
                BUNDLE_KEY_CONTENT_NO,
                selectedPlant.contentNo,
            )
        }

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_PLANT_SELECTED,
            result,
        )

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