package com.example.mooruckapp.ui.plant

import android.net.Uri
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
import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.databinding.FragmentPlantDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlantDetailFragment : Fragment() {

    // ViewBinding 객체
    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    // 전달받은 식물 ID
    private var plantId: Long = INVALID_PLANT_ID

    // Room 데이터베이스
    private val database by lazy {
        AppDatabase.getInstance(requireContext())
    }

    // 식물 정보를 조회하는 DAO
    private val userPlantDao: UserPlantDao by lazy {
        database.userPlantDao()
    }

    // 물주기 기록을 조회하는 DAO
    private val wateringRecordDao: WateringRecordDao by lazy {
        database.wateringRecordDao()
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        // arguments에서 식물 ID를 가져온다.
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

        // Fragment의 ViewBinding을 생성한다.
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

        // 식물 기본 정보를 조회한다.
        observePlant()

        // 마지막으로 물 준 날짜를 조회한다.
        loadLastWateredDate()
    }

    // 전달받은 식물 ID로 식물 정보를 실시간 조회한다.
    private fun observePlant() {

        // 잘못된 ID라면 오류를 표시한다.
        if (plantId == INVALID_PLANT_ID) {
            showLoadError()
            return
        }

        // Fragment 화면이 활성화되어 있을 때만 Flow를 수집한다.
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED,
            ) {

                userPlantDao.observeById(
                    plantId,
                ).collect { plant ->

                    // 식물이 없으면 오류를 표시한다.
                    if (plant == null) {
                        showLoadError()
                        return@collect
                    }

                    // 조회한 식물 정보를 화면에 표시한다.
                    bindPlant(plant)
                }
            }
        }
    }

    // 현재 식물의 가장 최근 물주기 날짜를 조회한다.
    private fun loadLastWateredDate() {

        // 잘못된 ID라면 조회하지 않는다.
        if (plantId == INVALID_PLANT_ID) {
            return
        }

        // suspend DAO 함수를 실행하기 위해 코루틴을 시작한다.
        viewLifecycleOwner.lifecycleScope.launch {

            // 가장 최근 물주기 날짜를 가져온다.
            val lastWateredDate =
                wateringRecordDao.getLastWateredDate(plantId)

            // 기록 유무에 따라 화면에 표시한다.
            binding.textLastWateredDate.text =
                if (lastWateredDate == null) {
                    "마지막으로 물 준 날짜: 기록 없음"
                } else {
                    "마지막으로 물 준 날짜: ${formatDate(lastWateredDate)}"
                }
        }
    }

    // 저장된 이미지 URI를 식물 프로필 사진에 표시한다.
    private fun bindPlantImage(
        profileImageUri: String?,
    ) {

        // URI가 없으면 기본 이미지를 표시한다.
        if (profileImageUri.isNullOrBlank()) {
            binding.imagePlantProfile.setImageResource(
                android.R.drawable.ic_menu_gallery,
            )
            return
        }

        try {

            // DB에 저장된 문자열을 Uri 객체로 변환한다.
            val imageUri = Uri.parse(profileImageUri)

            // Uri가 가리키는 이미지를 화면에 표시한다.
            binding.imagePlantProfile.setImageURI(imageUri)

        } catch (exception: Exception) {

            // 이미지 접근에 실패하면 기본 이미지를 표시한다.
            binding.imagePlantProfile.setImageResource(
                android.R.drawable.ic_menu_gallery,
            )

            exception.printStackTrace()
        }
    }

    // 조회한 식물 정보를 각 View에 표시한다.
    private fun bindPlant(
        plant: UserPlant,
    ) {

        // 별명이 없으면 기본 문구를 표시한다.
        binding.textPlantNickname.text =
            plant.nickname?.takeIf { it.isNotBlank() } ?: "별명 없음"

        // 식물 이름을 표시한다.
        binding.textPlantName.text =
            plant.plantName

        // 식물에게 필요한 광도를 표시한다.
        binding.textLight.text =
            "광도: ${plant.light}"

        // 식물에게 필요한 습도를 표시한다.
        binding.textHumidity.text =
            "습도: ${plant.humidity}"

        // 식물에게 필요한 온도를 표시한다.
        binding.textTemperature.text =
            "온도: ${plant.temperature}"

        // 물주기 주기를 표시한다.
        binding.textWateringInterval.text =
            "물주기: ${plant.wateringIntervalDays}일마다"

        // 식물을 심은 날짜를 표시한다.
        binding.textPlantedDate.text =
            "심은 날짜: ${formatDate(plant.plantedDate)}"

        // 식물을 심은 날부터 오늘까지의 날짜를 표시한다.
        binding.textTogetherDays.text =
            "함께한 지 ${calculateTogetherDays(plant.plantedDate)}일째"

        // 정상적으로 조회했다면 오류 메시지를 숨긴다.
        binding.textLoadError.visibility =
            View.GONE

        // 식물 프로필 이미지를 표시한다.
        bindPlantImage(plant.profileImageUri)
    }

    // 밀리초 날짜를 yyyy.MM.dd 형태로 변환한다.
    private fun formatDate(
        timeMillis: Long,
    ): String {

        // 화면에 사용할 날짜 형식을 만든다.
        val formatter = SimpleDateFormat(
            "yyyy.MM.dd",
            Locale.KOREA,
        )

        // Long 값을 Date로 변환한 뒤 문자열로 반환한다.
        return formatter.format(
            Date(timeMillis),
        )
    }

    // 식물을 심은 날부터 오늘까지 함께한 일수를 계산한다.
    private fun calculateTogetherDays(
        plantedDate: Long,
    ): Long {

        // 심은 날짜의 시, 분, 초를 0으로 맞춘다.
        val plantedCalendar = Calendar.getInstance().apply {
            timeInMillis = plantedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 오늘 날짜의 시, 분, 초를 0으로 맞춘다.
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 오늘과 심은 날짜의 시간 차이를 구한다.
        val differenceMillis =
            todayCalendar.timeInMillis - plantedCalendar.timeInMillis

        // 밀리초 차이를 일수로 변환한다.
        val differenceDays =
            TimeUnit.MILLISECONDS.toDays(differenceMillis)

        // 심은 당일을 D+1로 표시하고 음수가 나오지 않게 처리한다.
        return (differenceDays + 1).coerceAtLeast(1)
    }

    // 식물 정보를 찾지 못했을 때 오류를 표시한다.
    private fun showLoadError() {

        binding.textLoadError.text =
            "식물 정보를 불러올 수 없습니다."

        binding.textLoadError.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Fragment의 View가 사라지면 Binding 참조를 제거한다.
        _binding = null
    }

    companion object {

        // Bundle에서 사용할 식물 ID Key
        private const val ARG_PLANT_ID = "plant_id"

        // 식물 ID가 전달되지 않았음을 나타내는 값
        private const val INVALID_PLANT_ID = -1L

        // 식물 ID를 담아 상세 Fragment를 생성한다.
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