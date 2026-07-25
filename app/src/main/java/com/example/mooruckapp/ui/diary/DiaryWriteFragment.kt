package com.example.mooruckapp.ui.diary

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.data.local.GrowthDiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DiaryWriteFragment : Fragment() {

    private lateinit var tvDate: TextView
    private lateinit var etContent: EditText
    private lateinit var btnSave: Button
    private lateinit var ivPhoto: ImageView

    // 선택된 날짜 (기본값: 오늘)
    private var selectedDate: Long = System.currentTimeMillis()

    // 임시로 식물 ID 고정 (나중에 실제 선택된 식물로 교체)
    private val plantId: Long = 1

    // 수정할 일지 (없으면 새 글 작성)
    private var editingDiary: GrowthDiary? = null

    // 선택된 사진 경로
    private var selectedImageUri: String = ""

    // 갤러리에서 사진 선택
    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // 앱이 계속 접근할 수 있도록 권한 유지
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedImageUri = uri.toString()
            ivPhoto.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diary_write, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDate = view.findViewById(R.id.tvDate)
        etContent = view.findViewById(R.id.etContent)
        btnSave = view.findViewById(R.id.btnSave)
        ivPhoto = view.findViewById(R.id.ivPhoto)

        // 처음엔 오늘 날짜 표시
        updateDateText()

        // 수정 모드면 기존 내용 채우기
        arguments?.getLong("diaryId", -1)?.let { diaryId ->
            if (diaryId != -1L) {
                loadDiaryForEdit(diaryId)
            }
        }

        // 날짜 클릭 → 달력 띄우기
        tvDate.setOnClickListener { showDatePicker() }

        // 저장 버튼
        btnSave.setOnClickListener { saveDiary() }

        // 사진 클릭 → 갤러리 열기
        ivPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    // 선택된 날짜를 화면에 표시
    private fun updateDateText() {
        val format = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
        tvDate.text = format.format(selectedDate)
    }

    // 수정할 일지 불러오기
    private fun loadDiaryForEdit(diaryId: Long) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).growthDiaryDao()
            val diary = dao.getById(diaryId)
            if (diary != null) {
                editingDiary = diary
                selectedDate = diary.diaryDate
                updateDateText()
                etContent.setText(diary.content)
                if (diary.imageUrl.isNotEmpty()) {
                    selectedImageUri = diary.imageUrl
                    ivPhoto.setImageURI(android.net.Uri.parse(diary.imageUrl))
                }
            }
        }
    }

    // 날짜 선택 달력
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                selectedDate = cal.timeInMillis
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 일지 저장
    private fun saveDiary() {
        val content = etContent.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).growthDiaryDao()

            val existing = editingDiary
            if (existing != null) {
                val updated = existing.copy(
                    diaryDate = selectedDate,
                    content = content,
                    imageUrl = selectedImageUri,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated)
                Toast.makeText(requireContext(), "수정되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                val diary = GrowthDiary(
                    userPlantId = plantId,
                    diaryDate = selectedDate,
                    content = content,
                    imageUrl = selectedImageUri
                )
                dao.insert(diary)
                Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()
            }

            parentFragmentManager.popBackStack()
        }
    }
}