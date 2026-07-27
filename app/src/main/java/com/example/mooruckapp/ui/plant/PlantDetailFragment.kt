package com.example.mooruckapp.ui.plant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.data.local.dao.UserPlantDao
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.databinding.FragmentPlantDetailBinding
import kotlinx.coroutines.launch

class PlantDetailFragment : Fragment() {

    // ViewBinding 객체
    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    // 전달받은 식물 ID를 저장한다.
    private var plantId: Long = INVALID_PLANT_ID

    // Room 데이터베이스
    private val database by lazy {
        AppDatabase.getInstance(requireContext())
    }

    // 식물 정보를 조회하는 DAO
    private val userPlantDao: UserPlantDao by lazy {
        database.userPlantDao()
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        // arguments에서 전달받은 식물 ID를 저장한다.
        plantId = arguments?.getLong(
            ARG_PLANT_ID,
            INVALID_PLANT_ID,
        ) ?: INVALID_PLANT_ID
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentPlantDetailBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState,
        )

        observePlant()
    }

    /**
     * 전달받은 식물 ID로 Room DB를 조회한다.
     */
    private fun observePlant() {

        // 잘못된 ID라면 조회하지 않는다.
        if (plantId == INVALID_PLANT_ID) {
            showLoadError()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED,
            ) {

                userPlantDao.observeById(
                    plantId,
                ).collect { plant ->

                    if (plant == null) {

                        showLoadError()

                    } else {

                        bindPlant(plant)
                    }
                }
            }
        }
    }

    /**
     * 조회한 식물 정보를 화면에 표시한다.
     */
    private fun bindPlant(
        plant: UserPlant,
    ) {

        binding.textPlantNickname.text =
            plant.nickname ?: "별명 없음"

        binding.textPlantName.text =
            plant.plantName

        binding.textLight.text =
            "광도 : ${plant.light}"

        binding.textHumidity.text =
            "습도 : ${plant.humidity}"

        binding.textTemperature.text =
            "온도 : ${plant.temperature}"

        binding.textWateringInterval.text =
            "물주기 : ${plant.wateringIntervalDays}일"

        // TODO: 심은 날짜 표시

        // TODO: 마지막 물 준 날짜 표시

        // TODO: 함께한 날짜 계산

        // TODO: 프로필 이미지 표시

        binding.textLoadError.visibility =
            View.GONE
    }

    /**
     * 식물 정보를 찾지 못했을 때 오류를 표시한다.
     */
    private fun showLoadError() {

        binding.textLoadError.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        // Bundle에서 사용할 Key
        private const val ARG_PLANT_ID = "plant_id"

        // 전달되지 않았음을 의미하는 값
        private const val INVALID_PLANT_ID = -1L

        /**
         * 식물 상세 화면 생성
         */
        fun newInstance(
            plantId: Long,
        ): PlantDetailFragment {

            return PlantDetailFragment().apply {

                arguments = Bundle().apply {

                    putLong(
                        ARG_PLANT_ID,
                        plantId,
                    )
                }
            }
        }
    }
}