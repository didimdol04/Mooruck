package com.example.mooruckapp.ui.plant

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mooruckapp.R
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

    // 현재 화면에 표시 중인 식물 정보를 보관한다.
    private var currentPlant: UserPlant? = null

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

        // 뒤로가기 버튼을 누르면 이전 화면으로 돌아간다.
        binding.buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 별명을 누르면 별명 수정창을 연다.
        binding.buttonEditNickname.setOnClickListener {
            showNicknameEditDialog()
        }

        // 프로필 이미지를 누르면 이미지 선택창을 연다.
        binding.buttonEditImage.setOnClickListener {
            openImagePicker()
        }

        // 더보기 버튼을 누르면 메뉴를 표시한다.
        binding.buttonMore.setOnClickListener {
            showMoreMenu()
        }
    }

    // 더보기 버튼 아래에 팝업 메뉴를 표시한다.
    private fun showMoreMenu() {

        // buttonMore를 기준으로 PopupMenu를 만든다.
        val popupMenu = PopupMenu(
            requireContext(),
            binding.buttonMore,
        )

        // 식물 상세 메뉴 XML을 PopupMenu에 연결한다.
        popupMenu.menuInflater.inflate(
            R.menu.menu_plant_detail,
            popupMenu.menu,
        )

        // 사용자가 선택한 메뉴를 처리한다.
        popupMenu.setOnMenuItemClickListener { menuItem ->

            when (menuItem.itemId) {

                // 식물 삭제 메뉴를 누르면 확인 다이얼로그를 표시한다.
                R.id.menuDeletePlant -> {
                    showDeleteConfirmDialog()
                    true
                }

                // 처리하지 않은 메뉴이다.
                else -> false
            }
        }

        // PopupMenu를 화면에 표시한다.
        popupMenu.show()
    }

    // 식물을 삭제하기 전에 사용자에게 한 번 더 확인한다.
    private fun showDeleteConfirmDialog() {

        // 현재 식물 정보가 없으면 삭제할 수 없다.
        val plant = currentPlant

        if (plant == null) {

            Toast.makeText(
                requireContext(),
                "식물 정보를 불러오는 중입니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        // 별명이 있으면 별명을, 없으면 식물 이름을 표시한다.
        val displayName =
            plant.nickname?.takeIf { it.isNotBlank() }
                ?: plant.plantName

        AlertDialog.Builder(requireContext())
            .setTitle("식물 삭제")
            .setMessage(
                "'$displayName'을(를) 삭제할까요?\n" +
                        "물주기 기록과 성장 일지도 함께 삭제됩니다.",
            )
            .setNegativeButton(
                "취소",
                null,
            )
            .setPositiveButton(
                "삭제",
            ) { _, _ ->

                // 사용자가 삭제를 확인하면 Room에서 삭제한다.
                deletePlant(plant)
            }
            .show()
    }

    // 식물과 연결된 데이터를 Room에서 삭제한다.
    private fun deletePlant(
        plant: UserPlant,
    ) {

        // DAO의 suspend 삭제 함수를 실행하기 위해 코루틴을 시작한다.
        viewLifecycleOwner.lifecycleScope.launch {

            // UserPlant를 삭제한다.
            userPlantDao.delete(plant)

            Toast.makeText(
                requireContext(),
                "식물이 삭제되었습니다.",
                Toast.LENGTH_SHORT,
            ).show()


            // TODO: PlantListFragment 구현 후 popBackStack()으로 변경한다.
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PlantRegisterFragment(),
                    )
                .commit()
        }
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

        // 수정 기능에서 사용할 수 있도록 현재 식물을 보관한다.
        currentPlant = plant

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

    // 갤러리에서 새로운 프로필 이미지를 선택한다.
    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { imageUri ->

            // 사용자가 이미지를 선택하지 않으면 종료한다.
            if (imageUri == null) {
                return@registerForActivityResult
            }

            // 현재 식물 정보가 아직 조회되지 않았다면 종료한다.
            val plant = currentPlant
                ?: return@registerForActivityResult

            try {

                // 앱을 다시 실행해도 이미지에 접근할 수 있도록 권한을 유지한다.
                requireContext().contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )

            } catch (exception: SecurityException) {

                // 일부 이미지 제공자는 영구 권한을 지원하지 않을 수 있다.
                exception.printStackTrace()
            }

            // 선택한 이미지 URI를 Room에 저장한다.
            updatePlantProfileImage(
                plant = plant,
                imageUri = imageUri,
            )
        }

    // 별명을 입력할 수 있는 수정 다이얼로그를 표시한다.
    private fun showNicknameEditDialog() {

        // 식물 정보가 아직 조회되지 않았다면 수정하지 않는다.
        val plant = currentPlant

        if (plant == null) {
            Toast.makeText(
                requireContext(),
                "식물 정보를 불러오는 중입니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        // 별명을 입력받을 EditText를 생성한다.
        val editText = EditText(requireContext()).apply {

            // 기존 별명을 입력창에 표시한다.
            setText(plant.nickname.orEmpty())

            // 커서를 문자열 마지막으로 이동한다.
            setSelection(text.length)

            // 입력창에 안내 문구를 표시한다.
            hint = "새 별명을 입력해주세요"

            // 다이얼로그 안에서 여백을 준다.
            setPadding(
                48,
                24,
                48,
                24,
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle("별명 수정")
            .setView(editText)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장") { _, _ ->

                // 사용자가 입력한 별명의 앞뒤 공백을 제거한다.
                val newNickname =
                    editText.text.toString().trim()

                // 별명 길이 등을 검사한 뒤 Room을 수정한다.
                updatePlantNickname(
                    plant = plant,
                    newNickname = newNickname,
                )
            }
            .show()
    }

    // 식물의 별명만 변경해 Room에 저장한다.
    private fun updatePlantNickname(
        plant: UserPlant,
        newNickname: String,
    ) {

        // 기존 별명과 같으면 DB를 수정하지 않는다.
        if (newNickname == plant.nickname.orEmpty()) {
            return
        }

        // 너무 긴 별명은 저장하지 않는다.
        if (newNickname.length > MAX_NICKNAME_LENGTH) {

            Toast.makeText(
                requireContext(),
                "별명은 ${MAX_NICKNAME_LENGTH}자 이하로 입력해주세요.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        // suspend update 함수를 호출하기 위해 코루틴을 실행한다.
        viewLifecycleOwner.lifecycleScope.launch {

            // 기존 식물 객체에서 별명과 수정 시각만 변경한다.
            val updatedPlant = plant.copy(
                nickname = newNickname.ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            )

            // 변경한 식물 정보를 Room에 저장한다.
            userPlantDao.update(updatedPlant)

            Toast.makeText(
                requireContext(),
                "별명이 수정되었습니다.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 이미지 파일만 선택할 수 있도록 문서 선택기를 연다.
    private fun openImagePicker() {

        // 식물 정보가 조회된 뒤에만 수정할 수 있다.
        if (currentPlant == null) {

            Toast.makeText(
                requireContext(),
                "식물 정보를 불러오는 중입니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        imagePickerLauncher.launch(
            arrayOf("image/*"),
        )
    }

    // 식물의 프로필 이미지 URI만 변경해 Room에 저장한다.
    private fun updatePlantProfileImage(
        plant: UserPlant,
        imageUri: Uri,
    ) {

        // Uri 객체를 Room에 저장할 문자열로 변환한다.
        val newImageUri =
            imageUri.toString()

        // 기존 이미지와 같으면 DB를 수정하지 않는다.
        if (newImageUri == plant.profileImageUri) {
            return
        }

        // suspend update 함수를 호출하기 위해 코루틴을 실행한다.
        viewLifecycleOwner.lifecycleScope.launch {

            // 기존 식물 객체에서 이미지와 수정 시각만 변경한다.
            val updatedPlant = plant.copy(
                profileImageUri = newImageUri,
                updatedAt = System.currentTimeMillis(),
            )

            // 변경한 식물 정보를 Room에 저장한다.
            userPlantDao.update(updatedPlant)

            Toast.makeText(
                requireContext(),
                "프로필 이미지가 수정되었습니다.",
                Toast.LENGTH_SHORT,
            ).show()
        }
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

        // 입력 가능한 최대 별명 길이
        private const val MAX_NICKNAME_LENGTH = 20

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