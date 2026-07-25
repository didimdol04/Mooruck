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

        // 날짜 클릭 → 달력 띄우기
        tvDate.setOnClickListener { showDatePicker() }

        // 저장 버튼
        btnSave.setOnClickListener { saveDiary() }
    }

    // 선택된 날짜를 화면에 표시
    private fun updateDateText() {
        val format = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
        tvDate.text = format.format(selectedDate)
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

        // 내용 미입력 시 저장 차단
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val diary = GrowthDiary(
            userPlantId = plantId,
            diaryDate = selectedDate,
            content = content,
            imageUrl = ""
        )

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).growthDiaryDao()
            dao.insert(diary)

            Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()

            // 이전 화면(목록)으로 돌아가기
            parentFragmentManager.popBackStack()
        }
    }
}