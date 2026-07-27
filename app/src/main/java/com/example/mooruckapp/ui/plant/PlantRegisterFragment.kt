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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 식물 등록 방식을 검색 등록과 직접 등록으로 구분한다.
private enum class RegisterMode {
    SEARCH,
    MANUAL,
}

class PlantRegisterFragment : Fragment(R.layout.fragment_plant_register) {

    private var _binding: FragmentPlantRegisterBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedPlantedDate: Long? = null
    private var selectedLastWateredDate: Long? = null

    private var registerMode = RegisterMode.SEARCH

    // 갤러리에서 이미지를 선택한다.
    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { imageUri ->

            // 이미지가 선택되지 않았거나 화면이 종료된 경우 처리하지 않는다.
            if (imageUri == null || _binding == null) {
                return@registerForActivityResult
            }

            try {

                // 앱을 종료한 뒤에도 해당 이미지 URI를 읽을 수 있도록 권한을 유지한다.
                requireContext().contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )

            } catch (exception: SecurityException) {

                // 일부 기기나 이미지 제공자는 권한 유지 기능을 지원하지 않을 수 있다.
                exception.printStackTrace()
            }

            // 선택한 이미지 URI를 저장한다.
            selectedImageUri = imageUri

            // 등록 화면에 선택한 이미지를 미리 보여준다.
            binding.ivPlantProfile.setImageURI(imageUri)
        }

    private val userPlantDao by lazy {
        AppDatabase.getInstance(requireContext()).userPlantDao()
    }

    // 물주기 기록 테이블에 접근할 DAO를 가져온다.
    private val wateringRecordDao by lazy {
        AppDatabase
            .getInstance(requireContext())
            .wateringRecordDao()
    }

    // 성장 일지 테이블에 접근할 DAO를 가져온다.
    private val growthDiaryDao by lazy {
        AppDatabase
            .getInstance(requireContext())
            .growthDiaryDao()
    }

    // Fragment 화면 생성 후 ViewBinding과 초기 동작을 설정한다.
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPlantRegisterBinding.bind(view)

        setupInitialState()
        setupClickListeners()
    }

    // 식물 등록 화면을 검색 모드로 초기화한다.
    private fun setupInitialState() {
        showSearchMode(clearInformation = false)
    }

    // 식물 등록 화면에서 사용하는 클릭 이벤트를 설정한다.
    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            // 이미지 파일만 선택할 수 있도록 문서 선택기를 연다.
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

        binding.cbUnknownLastWateredDate.setOnCheckedChangeListener { _, isChecked ->
            handleUnknownLastWateredDate(isChecked)
        }

        binding.btnRegisterPlant.setOnClickListener {
            if (!validateInputs()) {
                return@setOnClickListener
            }

            saveUserPlant()
        }
    }

    // 화면에 입력된 값으로 저장할 UserPlant 객체를 생성한다.
    private fun createUserPlant(): UserPlant {
        val nickname = binding.etNickname.text
            .toString()
            .trim()
            .ifBlank { null }

        val light = binding.etLight.text
            .toString()
            .trim()
            .ifBlank { "정보 없음" }

        val humidity = binding.etHumidity.text
            .toString()
            .trim()
            .ifBlank { "정보 없음" }

        val temperature = binding.etTemperature.text
            .toString()
            .trim()
            .ifBlank { "정보 없음" }

        return UserPlant(
            plantName = binding.etPlantName.text.toString().trim(),
            nickname = nickname,
            profileImageUri = selectedImageUri?.toString(),
            light = light,
            humidity = humidity,
            temperature = temperature,
            wateringIntervalDays =
                binding.etWateringInterval.text.toString().trim().toInt(),
            plantedDate = requireNotNull(selectedPlantedDate),
        )
    }

    // 식물과 첫 물주기 기록, 첫 성장 일지를 Room DB에 순서대로 저장한다.
    private fun saveUserPlant() {
        val userPlant = createUserPlant()

        binding.btnRegisterPlant.isEnabled = false

        lifecycleScope.launch {
            try {
                // 식물을 저장하고 자동 생성된 식물 ID를 받는다.
                val userPlantId =
                    userPlantDao.insert(userPlant)

                // 생성된 식물 ID로 첫 물주기 기록을 저장한다.
                saveFirstWateringRecord(userPlantId)

                // 생성된 식물 ID로 첫 성장 일지를 저장한다.
                saveFirstGrowthDiary(userPlantId)

                binding.btnRegisterPlant.isEnabled = true

                showMessage(
                    "식물 등록이 완료되었어요. 식물 번호: $userPlantId",
                )
                // TODO: 홈 화면 구현 후 HomeFragment로 이동
            } catch (exception: Exception) {
                binding.btnRegisterPlant.isEnabled = true

                showMessage(
                    "식물 등록 중 오류가 발생했어요.",
                )
            }
        }
    }

    // UserPlant 저장 후 첫 물주기 기록 저장 단계로 전달한다.
    private fun onUserPlantSaved(plantId: Long) {
        showMessage("저장된 식물 번호: $plantId")
    }

    // 등록한 식물의 첫 물주기 기록 객체를 생성한다.
    private fun createFirstWateringRecord(
        userPlantId: Long,
    ): WateringRecord {
        return WateringRecord(
            userPlantId = userPlantId,
            wateredDate = requireNotNull(selectedLastWateredDate),
        )
    }

    // 등록한 식물의 첫 물주기 기록을 Room DB에 저장한다.
    private suspend fun saveFirstWateringRecord(
        userPlantId: Long,
    ) {
        val wateringRecord =
            createFirstWateringRecord(userPlantId)

        wateringRecordDao.insert(wateringRecord)
    }

    // 등록한 식물의 첫 성장 일지 객체를 생성한다.
    private fun createFirstGrowthDiary(
        userPlantId: Long,
    ): GrowthDiary {
        return GrowthDiary(
            userPlantId = userPlantId,
            diaryDate = requireNotNull(selectedPlantedDate),
            content = "식물과 함께한 첫날이에요.",
            imageUrl = selectedImageUri?.toString().orEmpty(),
        )
    }

    // 등록한 식물의 첫 성장 일지를 Room DB에 저장한다.
    private suspend fun saveFirstGrowthDiary(
        userPlantId: Long,
    ) {
        val growthDiary =
            createFirstGrowthDiary(userPlantId)

        growthDiaryDao.insert(growthDiary)
    }

    // 검색어를 검사하고 추후 API 검색 결과가 표시될 영역을 연다.
    private fun searchPlant() {
        val keyword = binding.etSearchPlantName.text.toString().trim()

        if (keyword.isBlank()) {
            binding.etSearchPlantName.error =
                "검색할 식물 이름을 입력해 주세요."

            binding.etSearchPlantName.requestFocus()
            return
        }

        binding.etSearchPlantName.error = null
        binding.rvPlantSearchResult.visibility = View.VISIBLE

        showMessage("'$keyword' 검색 기능은 API 연결 단계에서 구현할 예정이에요.")
    }

    // API 검색 결과를 이용하는 검색 등록 모드로 화면을 변경한다.
    private fun showSearchMode(clearInformation: Boolean = true) {
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
        }

        binding.btnManualInput.text = "직접 등록하기"
    }

    // 사용자가 식물 기본 정보를 직접 작성할 수 있는 모드로 화면을 변경한다.
    private fun showManualMode() {
        registerMode = RegisterMode.MANUAL

        binding.etSearchPlantName.error = null
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

    // 식물 기본 정보 입력란의 활성화 상태와 투명도를 변경한다.
    private fun setPlantInformationEnabled(isEnabled: Boolean) {
        val alpha = if (isEnabled) 1.0f else 0.6f

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

    // 식물 이름과 관리 정보 입력란을 초기화한다.
    private fun clearPlantInformation() {
        binding.etPlantName.text?.clear()
        binding.etLight.text?.clear()
        binding.etHumidity.text?.clear()
        binding.etTemperature.text?.clear()
        binding.etWateringInterval.text?.clear()

        binding.etPlantName.error = null
        binding.etLight.error = null
        binding.etHumidity.error = null
        binding.etTemperature.error = null
        binding.etWateringInterval.error = null
    }

    // 심은 날짜를 선택할 수 있는 달력을 표시한다.
    private fun showPlantedDatePicker() {
        val calendar = Calendar.getInstance().apply {
            selectedPlantedDate?.let {
                timeInMillis = it
            }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = createDateCalendar(
                    year = year,
                    month = month,
                    dayOfMonth = dayOfMonth,
                )

                selectedPlantedDate = selectedCalendar.timeInMillis
                binding.tvPlantedDate.text =
                    formatDate(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    // 마지막으로 물을 준 날짜를 선택할 수 있는 달력을 표시한다.
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
                val selectedCalendar = createDateCalendar(
                    year = year,
                    month = month,
                    dayOfMonth = dayOfMonth,
                )

                selectedLastWateredDate =
                    selectedCalendar.timeInMillis

                binding.tvLastWateredDate.text =
                    formatDate(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )

        datePickerDialog.datePicker.maxDate =
            System.currentTimeMillis()

        datePickerDialog.show()
    }

    // 선택한 연도와 월, 일을 자정 기준의 Calendar 객체로 만든다.
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

    // 마지막 물 준 날짜를 모르는 경우 오늘 날짜를 자동으로 적용한다.
    private fun handleUnknownLastWateredDate(isChecked: Boolean) {
        if (isChecked) {
            val todayCalendar = Calendar.getInstance().apply {
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

    // 필수 입력값과 물주기 간격이 올바른지 검사한다.
    private fun validateInputs(): Boolean {
        val plantName =
            binding.etPlantName.text.toString().trim()

        val nickname =
            binding.etNickname.text.toString().trim()

        val wateringIntervalText =
            binding.etWateringInterval.text.toString().trim()

        binding.etPlantName.error = null
        binding.etNickname.error = null
        binding.etWateringInterval.error = null

        if (plantName.isBlank()) {
            val message = when (registerMode) {
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
            val message = when (registerMode) {
                RegisterMode.SEARCH ->
                    "식물을 검색하고 관리 정보를 선택해 주세요."

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
            showMessage("마지막으로 물 준 날짜를 선택해 주세요.")
            return false
        }

        return true
    }

    // 밀리초로 저장된 날짜를 화면에 표시할 문자열로 변환한다.
    private fun formatDate(timeInMillis: Long): String {
        val dateFormat = SimpleDateFormat(
            "yyyy. MM. dd.",
            Locale.KOREA,
        )

        return dateFormat.format(timeInMillis)
    }

    // 사용자에게 짧은 안내 메시지를 표시한다.
    private fun showMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT,
        ).show()
    }

    // Fragment 화면이 사라질 때 ViewBinding 참조를 제거한다.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}