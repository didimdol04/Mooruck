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

    // 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간이다.
    // 500L은 500밀리초, 즉 0.5초를 의미한다.
    private companion object {
        const val SEARCH_DEBOUNCE_TIME = 500L
    }

    // Retrofit API 호출을 담당하는 Repository다.
    private val repository = PlantRepository()

    // 현재 실행 중이거나 검색을 기다리고 있는 코루틴 작업이다.
    // 사용자가 새 문자를 입력하면 기존 작업을 취소하기 위해 사용한다.
    private var searchJob: Job? = null

    // RecyclerView에 검색 결과를 표시하는 Adapter다.
    private val plantSearchAdapter = PlantSearchAdapter { selectedPlant ->

        // 사용자가 검색 결과를 선택하면 실행된다.
        onPlantSelected(selectedPlant)
    }

    // Fragment의 ViewBinding 객체다.
    private var _binding: BottomSheetPlantSearchBinding? = null

    // _binding에 편리하게 접근하기 위한 프로퍼티다.
    private val binding get() = _binding!!

    // BottomSheet 화면을 생성한다.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        // bottom_sheet_plant_search.xml과 ViewBinding을 연결한다.
        _binding = BottomSheetPlantSearchBinding.inflate(
            inflater,
            container,
            false
        )

        // 생성된 화면의 최상위 View를 반환한다.
        return binding.root
    }

    // 화면 생성이 끝난 뒤 RecyclerView와 자동 검색을 설정한다.
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 화면에 필요한 초기 설정을 실행한다.
        initViews()
    }

    // RecyclerView와 검색창 이벤트를 설정한다.
    private fun initViews() {

        // 검색 결과를 세로 방향으로 배치한다.
        binding.recyclerViewPlants.layoutManager =
            LinearLayoutManager(requireContext())

        // RecyclerView에 Adapter를 연결한다.
        binding.recyclerViewPlants.adapter = plantSearchAdapter

        // RecyclerView 자체 크기가 검색 결과에 따라 변하지 않는다고 알려준다.
        binding.recyclerViewPlants.setHasFixedSize(true)

        // 검색창의 문자열이 변경될 때마다 실행된다.
        binding.editTextSearch.doAfterTextChanged { editable ->

            // 입력된 문자열을 가져오고 앞뒤 공백을 제거한다.
            val keyword = editable
                ?.toString()
                ?.trim()
                .orEmpty()

            // 이전에 대기 중이거나 실행 중이던 검색 작업을 취소한다.
            searchJob?.cancel()

            // 검색어가 비어 있다면 검색 결과를 초기화한다.
            if (keyword.isBlank()) {

                // RecyclerView에서 기존 검색 결과를 제거한다.
                plantSearchAdapter.submitList(emptyList())

                // 검색 중 표시를 숨긴다.
                binding.progressBarSearch.visibility = View.GONE

                // 빈 검색 결과 안내 문구도 숨긴다.
                binding.textViewEmptyResult.visibility = View.GONE

                // 새로운 검색 작업을 만들지 않고 종료한다.
                return@doAfterTextChanged
            }

            // 새로운 자동 검색 작업을 시작한다.
            searchJob = viewLifecycleOwner.lifecycleScope.launch {

                // 사용자가 입력을 멈추는지 0.5초 동안 기다린다.
                delay(SEARCH_DEBOUNCE_TIME)

                // 0.5초 동안 추가 입력이 없었다면 API를 호출한다.
                searchPlants(keyword)
            }
        }
    }

    // 농사로 식물 검색 API를 호출한다.
    private suspend fun searchPlants(keyword: String) {

        // 인증키가 발급되면 BuildConfig 등에서 가져오도록 변경한다.
        val apiKey = ""

        // 인증키가 등록되지 않았다면 API를 호출하지 않는다.
        if (apiKey.isBlank()) {

            Toast.makeText(
                requireContext(),
                "API 인증키가 아직 등록되지 않았습니다.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // API 호출을 시작하기 전에 로딩 표시를 보여준다.
        binding.progressBarSearch.visibility = View.VISIBLE

        // 새로운 검색이 시작되면 이전 빈 결과 안내 문구를 숨긴다.
        binding.textViewEmptyResult.visibility = View.GONE

        try {

            // Repository를 통해 식물 검색 API를 호출한다.
            val result = repository.searchPlants(
                apiKey = apiKey,
                keyword = keyword
            )

            // BottomSheet 화면이 이미 제거되었다면 이후 UI 작업을 하지 않는다.
            if (_binding == null) {
                return
            }

            // API 응답에서 실제 식물 목록을 꺼낸다.
            val plants = result.response
                .body
                .items
                .item

            // 현재 검색창의 검색어를 다시 확인한다.
            val currentKeyword = binding.editTextSearch.text
                .toString()
                .trim()

            // API 요청 중 검색어가 바뀌었다면 이전 결과를 화면에 표시하지 않는다.
            if (currentKeyword != keyword) {
                return
            }

            // 검색 결과를 Adapter에 전달해 RecyclerView를 갱신한다.
            plantSearchAdapter.submitList(plants)

            // 검색 결과가 없는지 확인한다.
            if (plants.isEmpty()) {

                // 검색 결과가 없다는 안내 문구를 보여준다.
                binding.textViewEmptyResult.visibility = View.VISIBLE
            } else {

                // 검색 결과가 있다면 안내 문구를 숨긴다.
                binding.textViewEmptyResult.visibility = View.GONE
            }

        } catch (exception: CancellationException) {

            // 새 검색어가 입력되어 기존 검색 작업이 취소된 경우다.
            // 정상적인 취소이므로 오류 메시지를 띄우지 않고 다시 전달한다.
            throw exception

        } catch (exception: Exception) {

            // BottomSheet 화면이 아직 존재할 때만 오류 메시지를 보여준다.
            if (_binding != null) {
                Toast.makeText(
                    requireContext(),
                    "식물을 검색하는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Logcat에서 실제 오류 원인을 확인할 수 있도록 출력한다.
            exception.printStackTrace()

        } finally {

            // BottomSheet 화면이 남아 있다면 로딩 표시를 숨긴다.
            if (_binding != null) {
                binding.progressBarSearch.visibility = View.GONE
            }
        }
    }

    // 사용자가 검색 결과에서 식물을 선택했을 때 실행된다.
    private fun onPlantSelected(selectedPlant: PlantItem) {

        // 현재는 선택 결과가 정상적으로 전달되는지 확인하기 위해 Toast를 띄운다.
        Toast.makeText(
            requireContext(),
            "${selectedPlant.title}을(를) 선택했습니다.",
            Toast.LENGTH_SHORT
        ).show()

        // 다음 단계에서는 식물의 컨텐츠 번호로 상세 API를 호출한다.
        // getPlantDetail(selectedPlant.contentNo)
    }

    // BottomSheet의 View가 제거될 때 호출된다.
    override fun onDestroyView() {

        // 대기 중이거나 실행 중인 자동 검색 작업을 취소한다.
        searchJob?.cancel()

        // Job이 제거된 화면을 참조하지 않도록 null로 변경한다.
        searchJob = null

        // RecyclerView가 Adapter를 계속 참조하지 않도록 연결을 해제한다.
        binding.recyclerViewPlants.adapter = null

        // 부모 클래스의 onDestroyView()를 호출한다.
        super.onDestroyView()

        // ViewBinding이 제거된 View를 붙잡지 않도록 null로 변경한다.
        _binding = null
    }
}