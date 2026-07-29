package com.example.mooruckapp.ui.plant

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.data.local.GrowthDiary
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.data.local.entity.WateringRecord
import com.example.mooruckapp.databinding.FragmentPlantRegisterBinding
import com.example.mooruckapp.network.dto.PlantDetail
import com.example.mooruckapp.network.mapper.PlantMapper
import com.example.mooruckapp.repository.PlantRepository
import com.example.mooruckapp.ui.plant.PlantSearchBottomSheet.Companion.BUNDLE_KEY_CONTENT_NO
import com.example.mooruckapp.ui.plant.PlantSearchBottomSheet.Companion.REQUEST_KEY_PLANT_SELECTED
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class RegisterMode {
    SEARCH,
    MANUAL,
}

class PlantRegisterFragment : Fragment(R.layout.fragment_plant_register) {

    private val repository = PlantRepository()

    private var detailJob: Job? = null

    private var _binding: FragmentPlantRegisterBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedPlantedDate: Long? = null
    private var selectedLastWateredDate: Long? = null

    private var registerMode = RegisterMode.SEARCH

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { imageUri ->

            if (imageUri == null || _binding == null) {
                return@registerForActivityResult
            }

            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (exception: SecurityException) {
                exception.printStackTrace()
            }

            selectedImageUri = imageUri
            binding.ivPlantProfile.setImageURI(imageUri)
        }

    private val userPlantDao by lazy {
        AppDatabase
            .getInstance(requireContext())
            .userPlantDao()
    }

    private val wateringRecordDao by lazy {
        AppDatabase
            .getInstance(requireContext())
            .wateringRecordDao()
    }

    private val growthDiaryDao by lazy {
        AppDatabase
            .getInstance(requireContext())
            .growthDiaryDao()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPlantRegisterBinding.bind(view)

        observePlantSelection()
        setupInitialState()
        setupClickListeners()

        binding.buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observePlantSelection() {
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_PLANT_SELECTED,
            viewLifecycleOwner,
        ) { _, bundle ->

            val contentNo =
                bundle.getString(BUNDLE_KEY_CONTENT_NO)

            if (contentNo.isNullOrBlank()) {
                showMessage("식물 정보를 불러올 수 없어요.")
                return@setFragmentResultListener
            }

            getPlantDetail(contentNo)
        }
    }

    private fun setupInitialState() {
        showSearchMode(clearInformation = false)
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch(
                arrayOf("image/*"),
            )
        }

        binding.btnSearchPlant.setOnClickListener {
            searchPlant()
        }

        binding.btnManualInput.setOnClickListener {
            when (registerMode) {
                RegisterMode.SEARCH -> showManualMode()
                RegisterMode.MANUAL -> showSearchMode()
            }
        }

        binding.tvPlantedDate.setOnClickListener {
            showPlantedDatePicker()
        }

        binding.tvLastWateredDate.setOnClickListener {
            showLastWateredDatePicker()
        }

        binding.cbUnknownLastWateredDate
            .setOnCheckedChangeListener { _, isChecked ->
                handleUnknownLastWateredDate(isChecked)
            }

        binding.btnRegisterPlant.setOnClickListener {
            if (!validateInputs()) {
                return@setOnClickListener
            }

            saveUserPlant()
        }
    }

    private fun searchPlant() {
        PlantSearchBottomSheet().show(
            parentFragmentManager,
            PlantSearchBottomSheet::class.java.simpleName,
        )
    }

    private fun getPlantDetail(contentNo: String) {
        detailJob?.cancel()

        detailJob = viewLifecycleOwner.lifecycleScope.launch {
            setDetailLoading(true)

            try {
                // 선택한 식물 번호로 상세 정보를 요청해.
                val plantDetail = repository.getPlantDetail(
                    contentNo = contentNo,
                )

                // API 결과를 등록 화면에 입력해.
                fillPlantInformation(plantDetail)

            } catch (exception: CancellationException) {
                // 새로운 요청으로 기존 작업이 취소된 경우 정상적으로 다시 던져.
                throw exception

            } catch (exception: Exception) {
                exception.printStackTrace()

                if (_binding != null) {
                    showMessage(
                        "식물 상세 정보를 불러오는 중 오류가 발생했어요.",
                    )
                }

            } finally {
                if (_binding != null) {
                    setDetailLoading(false)
                }
            }
        }
    }

    private fun fillPlantInformation(
        plantDetail: PlantDetail,
    ) {
        // 검색 결과로 불러온 정보이므로 검색 모드로 설정해.
        registerMode = RegisterMode.SEARCH

        // API로 채운 정보는 사용자가 수정하지 못하게 유지해.
        setPlantInformationEnabled(false)

        val wateringIntervalDays =
            if (plantDetail.springWaterCode.isNotBlank()) {
                PlantMapper.waterCycleCodeToDays(
                    plantDetail.springWaterCode,
                )
            } else {
                null
            }

        binding.etSearchPlantName.setText(
            plantDetail.name,
        )

        binding.etPlantName.setText(
            plantDetail.name,
        )

        binding.etLight.setText(
            plantDetail.lightDemand.ifBlank {
                "정보 없음"
            },
        )

        binding.etHumidity.setText(
            plantDetail.humidity.ifBlank {
                "정보 없음"
            },
        )

        binding.etTemperature.setText(
            plantDetail.temperature.ifBlank {
                "정보 없음"
            },
        )

        binding.etWateringInterval.setText(
            wateringIntervalDays?.toString().orEmpty(),
        )

        clearPlantInformationErrors()

        showMessage(
            "${plantDetail.name} 정보를 불러왔어요.",
        )
    }

    private fun setDetailLoading(isLoading: Boolean) {
        binding.btnSearchPlant.isEnabled = !isLoading
        binding.btnManualInput.isEnabled = !isLoading
        binding.btnRegisterPlant.isEnabled = !isLoading

        binding.btnSearchPlant.alpha =
            if (isLoading) 0.5f else 1.0f
    }

    private fun showSearchMode(
        clearInformation: Boolean = true,
    ) {
        registerMode = RegisterMode.SEARCH

        binding.etSearchPlantName.isEnabled = true
        binding.btnSearchPlant.isEnabled = true

        binding.etSearchPlantName.alpha = 1.0f
        binding.btnSearchPlant.alpha = 1.0f

        binding.tvSearchGuide.text =
            "식물을 검색하면 관리 정보가 자동으로 입력돼요."

        binding.rvPlantSearchResult.visibility = View.GONE

        setPlantInformationEnabled(false)

        if (clearInformation) {
            clearPlantInformation()
            binding.etSearchPlantName.text?.clear()
        }

        binding.btnManualInput.text = "직접 등록하기"
    }

    private fun showManualMode() {
        detailJob?.cancel()

        registerMode = RegisterMode.MANUAL

        binding.etSearchPlantName.error = null
        binding.etSearchPlantName.text?.clear()
        binding.etSearchPlantName.isEnabled = false
        binding.btnSearchPlant.isEnabled = false

        binding.etSearchPlantName.alpha = 0.5f
        binding.btnSearchPlant.alpha = 0.5f

        binding.tvSearchGuide.text =
            "식물 정보를 아래 입력란에 직접 작성해 주세요."

        binding.rvPlantSearchResult.visibility = View.GONE

        clearPlantInformation()
        setPlantInformationEnabled(true)

        binding.etPlantName.requestFocus()
        binding.btnManualInput.text = "검색으로 등록하기"
    }

    private fun setPlantInformationEnabled(
        isEnabled: Boolean,
    ) {
        val alpha =
            if (isEnabled) 1.0f else 0.6f

        binding.etPlantName.isEnabled = isEnabled
        binding.etLight.isEnabled = isEnabled
        binding.etHumidity.isEnabled = isEnabled
        binding.etTemperature.isEnabled = isEnabled
        binding.etWateringInterval.isEnabled = isEnabled

        binding.etPlantName.alpha = alpha
        binding.etLight.alpha = alpha
        binding.etHumidity.alpha = alpha
        binding.etTemperature.alpha = alpha
        binding.etWateringInterval.alpha = alpha
    }

    private fun clearPlantInformation() {
        binding.etPlantName.text?.clear()
        binding.etLight.text?.clear()
        binding.etHumidity.text?.clear()
        binding.etTemperature.text?.clear()
        binding.etWateringInterval.text?.clear()

        clearPlantInformationErrors()
    }

    private fun clearPlantInformationErrors() {
        binding.etPlantName.error = null
        binding.etLight.error = null
        binding.etHumidity.error = null
        binding.etTemperature.error = null
        binding.etWateringInterval.error = null
    }

    private fun createUserPlant(): UserPlant {
        val nickname =
            binding.etNickname.text
                .toString()
                .trim()
                .ifBlank { null }

        val light =
            binding.etLight.text
                .toString()
                .trim()
                .ifBlank { "정보 없음" }

        val humidity =
            binding.etHumidity.text
                .toString()
                .trim()
                .ifBlank { "정보 없음" }

        val temperature =
            binding.etTemperature.text
                .toString()
                .trim()
                .ifBlank { "정보 없음" }

        return UserPlant(
            plantName = binding.etPlantName.text
                .toString()
                .trim(),
            nickname = nickname,
            profileImageUri =
                selectedImageUri?.toString(),
            light = light,
            humidity = humidity,
            temperature = temperature,
            wateringIntervalDays =
                binding.etWateringInterval.text
                    .toString()
                    .trim()
                    .toInt(),
            plantedDate =
                requireNotNull(selectedPlantedDate),
        )
    }

    private fun saveUserPlant() {
        val userPlant = createUserPlant()

        binding.btnRegisterPlant.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userPlantId =
                    userPlantDao.insert(userPlant)

                saveFirstWateringRecord(userPlantId)
                saveFirstGrowthDiary(userPlantId)

                if (_binding == null) {
                    return@launch
                }

                binding.btnRegisterPlant.isEnabled = true

                showMessage(
                    "식물 등록이 완료되었어요. 식물 번호: $userPlantId",
                )

                parentFragmentManager.popBackStack()

            } catch (exception: Exception) {
                exception.printStackTrace()

                if (_binding != null) {
                    binding.btnRegisterPlant.isEnabled = true

                    showMessage(
                        "식물 등록 중 오류가 발생했어요.",
                    )
                }
            }
        }
    }

    private fun createFirstWateringRecord(
        userPlantId: Long,
    ): WateringRecord {
        return WateringRecord(
            userPlantId = userPlantId,
            wateredDate =
                requireNotNull(selectedLastWateredDate),
        )
    }

    private suspend fun saveFirstWateringRecord(
        userPlantId: Long,
    ) {
        val wateringRecord =
            createFirstWateringRecord(userPlantId)

        wateringRecordDao.insert(wateringRecord)
    }

    private fun createFirstGrowthDiary(
        userPlantId: Long,
    ): GrowthDiary {
        return GrowthDiary(
            userPlantId = userPlantId,
            diaryDate =
                requireNotNull(selectedPlantedDate),
            content = "식물과 함께한 첫날이에요.",
            imageUrl =
                selectedImageUri?.toString().orEmpty(),
        )
    }

    private suspend fun saveFirstGrowthDiary(
        userPlantId: Long,
    ) {
        val growthDiary =
            createFirstGrowthDiary(userPlantId)

        growthDiaryDao.insert(growthDiary)
    }

    private fun showPlantedDatePicker() {
        val calendar = Calendar.getInstance().apply {
            selectedPlantedDate?.let {
                timeInMillis = it
            }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val selectedCalendar =
                    createDateCalendar(
                        year = year,
                        month = month,
                        dayOfMonth = dayOfMonth,
                    )

                selectedPlantedDate =
                    selectedCalendar.timeInMillis

                binding.tvPlantedDate.text =
                    formatDate(
                        selectedCalendar.timeInMillis,
                    )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )

        datePickerDialog.datePicker.maxDate =
            System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun showLastWateredDatePicker() {
        if (binding.cbUnknownLastWateredDate.isChecked) {
            return
        }

        val calendar = Calendar.getInstance().apply {
            selectedLastWateredDate?.let {
                timeInMillis = it
            }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val selectedCalendar =
                    createDateCalendar(
                        year = year,
                        month = month,
                        dayOfMonth = dayOfMonth,
                    )

                selectedLastWateredDate =
                    selectedCalendar.timeInMillis

                binding.tvLastWateredDate.text =
                    formatDate(
                        selectedCalendar.timeInMillis,
                    )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )

        datePickerDialog.datePicker.maxDate =
            System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun createDateCalendar(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun handleUnknownLastWateredDate(
        isChecked: Boolean,
    ) {
        if (isChecked) {
            val todayCalendar =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            selectedLastWateredDate =
                todayCalendar.timeInMillis

            binding.tvLastWateredDate.text =
                formatDate(todayCalendar.timeInMillis)

            binding.tvLastWateredDate.isEnabled = false
            binding.tvLastWateredDate.alpha = 0.5f
        } else {
            selectedLastWateredDate = null

            binding.tvLastWateredDate.text =
                "날짜를 선택해 주세요"

            binding.tvLastWateredDate.isEnabled = true
            binding.tvLastWateredDate.alpha = 1.0f
        }
    }

    private fun validateInputs(): Boolean {
        val plantName =
            binding.etPlantName.text
                .toString()
                .trim()

        val nickname =
            binding.etNickname.text
                .toString()
                .trim()

        val wateringIntervalText =
            binding.etWateringInterval.text
                .toString()
                .trim()

        binding.etPlantName.error = null
        binding.etNickname.error = null
        binding.etWateringInterval.error = null

        if (plantName.isBlank()) {
            val message =
                when (registerMode) {
                    RegisterMode.SEARCH ->
                        "식물을 검색하고 검색 결과를 선택해 주세요."

                    RegisterMode.MANUAL ->
                        "식물 이름을 입력해 주세요."
                }

            binding.etPlantName.error = message

            if (registerMode == RegisterMode.MANUAL) {
                binding.etPlantName.requestFocus()
            }

            showMessage(message)
            return false
        }

        if (plantName.length > 50) {
            binding.etPlantName.error =
                "식물 이름은 50자 이내로 입력해 주세요."

            binding.etPlantName.requestFocus()
            return false
        }

        if (nickname.length > 30) {
            binding.etNickname.error =
                "별명은 30자 이내로 입력해 주세요."

            binding.etNickname.requestFocus()
            return false
        }

        if (wateringIntervalText.isBlank()) {
            val message =
                when (registerMode) {
                    RegisterMode.SEARCH ->
                        "식물을 검색하고 관리 정보를 불러와 주세요."

                    RegisterMode.MANUAL ->
                        "물주기 간격을 입력해 주세요."
                }

            binding.etWateringInterval.error = message

            if (registerMode == RegisterMode.MANUAL) {
                binding.etWateringInterval.requestFocus()
            }

            showMessage(message)
            return false
        }

        val wateringIntervalDays =
            wateringIntervalText.toIntOrNull()

        if (wateringIntervalDays == null) {
            binding.etWateringInterval.error =
                "물주기 간격은 숫자로 입력해 주세요."

            binding.etWateringInterval.requestFocus()
            return false
        }

        if (wateringIntervalDays !in 1..365) {
            binding.etWateringInterval.error =
                "물주기 간격은 1일에서 365일 사이로 입력해 주세요."

            binding.etWateringInterval.requestFocus()
            return false
        }

        if (selectedPlantedDate == null) {
            showMessage("심은 날짜를 선택해 주세요.")
            return false
        }

        if (selectedLastWateredDate == null) {
            showMessage(
                "마지막으로 물 준 날짜를 선택해 주세요.",
            )
            return false
        }

        return true
    }

    private fun formatDate(
        timeInMillis: Long,
    ): String {
        val dateFormat = SimpleDateFormat(
            "yyyy. MM. dd.",
            Locale.KOREA,
        )

        return dateFormat.format(timeInMillis)
    }

    private fun showMessage(
        message: String,
    ) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        detailJob?.cancel()
        detailJob = null

        super.onDestroyView()

        _binding = null
    }
}