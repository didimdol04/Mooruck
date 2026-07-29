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
import com.example.mooruckapp.ui.diary.DiaryFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlantDetailFragment : Fragment() {

    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var plantId: Long = INVALID_PLANT_ID

    private var currentPlant: UserPlant? = null

    private val database by lazy {
        AppDatabase.getInstance(requireContext())
    }

    private val userPlantDao: UserPlantDao by lazy {
        database.userPlantDao()
    }

    private val wateringRecordDao: WateringRecordDao by lazy {
        database.wateringRecordDao()
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

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

        loadLastWateredDate()

        binding.buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.buttonEditNickname.setOnClickListener {
            showNicknameEditDialog()
        }

        binding.buttonEditImage.setOnClickListener {
            openImagePicker()
        }

        binding.buttonMore.setOnClickListener {
            showMoreMenu()
        }

        binding.buttonGrowthDiary.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    DiaryFragment(),
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showMoreMenu() {

        val popupMenu = PopupMenu(
            requireContext(),
            binding.buttonMore,
        )

        popupMenu.menuInflater.inflate(
            R.menu.menu_plant_detail,
            popupMenu.menu,
        )

        popupMenu.setOnMenuItemClickListener { menuItem ->

            when (menuItem.itemId) {

                R.id.menuDeletePlant -> {
                    showDeleteConfirmDialog()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDeleteConfirmDialog() {

        val plant = currentPlant

        if (plant == null) {

            Toast.makeText(
                requireContext(),
                "식물 정보를 불러오는 중입니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

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

                deletePlant(plant)
            }
            .show()
    }

    private fun deletePlant(
        plant: UserPlant,
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

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

    private fun observePlant() {

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
                        return@collect
                    }

                    bindPlant(plant)
                }
            }
        }
    }

    private fun loadLastWateredDate() {

        if (plantId == INVALID_PLANT_ID) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val lastWateredDate =
                wateringRecordDao.getLastWateredDate(plantId)

            binding.textLastWateredDate.text =
                if (lastWateredDate == null) {
                    "마지막으로 물 준 날짜: 기록 없음"
                } else {
                    "마지막으로 물 준 날짜: ${formatDate(lastWateredDate)}"
                }
        }
    }

    private fun bindPlantImage(
        profileImageUri: String?,
    ) {

        if (profileImageUri.isNullOrBlank()) {
            binding.imagePlantProfile.setImageResource(
                android.R.drawable.ic_menu_gallery,
            )
            return
        }

        try {

            val imageUri = Uri.parse(profileImageUri)

            binding.imagePlantProfile.setImageURI(imageUri)

        } catch (exception: Exception) {

            binding.imagePlantProfile.setImageResource(
                android.R.drawable.ic_menu_gallery,
            )

            exception.printStackTrace()
        }
    }

    private fun bindPlant(
        plant: UserPlant,
    ) {

        currentPlant = plant

        binding.textPlantNickname.text =
            plant.nickname?.takeIf { it.isNotBlank() } ?: "별명 없음"

        binding.textPlantName.text =
            plant.plantName

        binding.textLight.text =
            "광도: ${plant.light}"

        binding.textHumidity.text =
            "습도: ${plant.humidity}"

        binding.textTemperature.text =
            "온도: ${plant.temperature}"

        binding.textWateringInterval.text =
            "물주기: ${plant.wateringIntervalDays}일마다"

        binding.textPlantedDate.text =
            "심은 날짜: ${formatDate(plant.plantedDate)}"

        binding.textTogetherDays.text =
            "함께한 지 ${calculateTogetherDays(plant.plantedDate)}일째"

        binding.textLoadError.visibility =
            View.GONE

        bindPlantImage(plant.profileImageUri)
    }

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { imageUri ->

            if (imageUri == null) {
                return@registerForActivityResult
            }

            val plant = currentPlant
                ?: return@registerForActivityResult

            try {

                requireContext().contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )

            } catch (exception: SecurityException) {

                exception.printStackTrace()
            }

            updatePlantProfileImage(
                plant = plant,
                imageUri = imageUri,
            )
        }

    private fun showNicknameEditDialog() {

        val plant = currentPlant

        if (plant == null) {
            Toast.makeText(
                requireContext(),
                "식물 정보를 불러오는 중입니다.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        val editText = EditText(requireContext()).apply {

            setText(plant.nickname.orEmpty())

            setSelection(text.length)

            hint = "새 별명을 입력해주세요"

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

                val newNickname =
                    editText.text.toString().trim()

                updatePlantNickname(
                    plant = plant,
                    newNickname = newNickname,
                )
            }
            .show()
    }

    private fun updatePlantNickname(
        plant: UserPlant,
        newNickname: String,
    ) {

        if (newNickname == plant.nickname.orEmpty()) {
            return
        }

        if (newNickname.length > MAX_NICKNAME_LENGTH) {

            Toast.makeText(
                requireContext(),
                "별명은 ${MAX_NICKNAME_LENGTH}자 이하로 입력해주세요.",
                Toast.LENGTH_SHORT,
            ).show()

            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val updatedPlant = plant.copy(
                nickname = newNickname.ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            )

            userPlantDao.update(updatedPlant)

            Toast.makeText(
                requireContext(),
                "별명이 수정되었습니다.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openImagePicker() {

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

    private fun updatePlantProfileImage(
        plant: UserPlant,
        imageUri: Uri,
    ) {

        val newImageUri =
            imageUri.toString()

        if (newImageUri == plant.profileImageUri) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val updatedPlant = plant.copy(
                profileImageUri = newImageUri,
                updatedAt = System.currentTimeMillis(),
            )

            userPlantDao.update(updatedPlant)

            Toast.makeText(
                requireContext(),
                "프로필 이미지가 수정되었습니다.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun formatDate(
        timeMillis: Long,
    ): String {

        val formatter = SimpleDateFormat(
            "yyyy.MM.dd",
            Locale.KOREA,
        )

        return formatter.format(
            Date(timeMillis),
        )
    }

    private fun calculateTogetherDays(
        plantedDate: Long,
    ): Long {

        val plantedCalendar = Calendar.getInstance().apply {
            timeInMillis = plantedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val differenceMillis =
            todayCalendar.timeInMillis - plantedCalendar.timeInMillis

        val differenceDays =
            TimeUnit.MILLISECONDS.toDays(differenceMillis)

        return (differenceDays + 1).coerceAtLeast(1)
    }

    private fun showLoadError() {

        binding.textLoadError.text =
            "식물 정보를 불러올 수 없습니다."

        binding.textLoadError.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_PLANT_ID = "plant_id"

        private const val INVALID_PLANT_ID = -1L

        private const val MAX_NICKNAME_LENGTH = 20

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