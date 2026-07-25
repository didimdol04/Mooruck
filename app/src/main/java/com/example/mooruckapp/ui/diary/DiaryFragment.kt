package com.example.mooruckapp.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import kotlinx.coroutines.launch
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mooruckapp.data.local.GrowthDiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryFragment : Fragment() {

    private lateinit var rvDiary: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DiaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_diary 레이아웃을 화면으로 만듦
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDiary = view.findViewById(R.id.rvDiary)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        // 어댑터 초기 설정 (빈 목록으로 시작)
        adapter = DiaryAdapter(
            diaryList = emptyList(),
            onItemClick = { diary ->
                showDetailDialog(diary)
            },
            onMoreClick = { diary, anchor ->
                showPopupMenu(diary, anchor)
            },
        )
        rvDiary.layoutManager = LinearLayoutManager(requireContext())
        rvDiary.adapter = adapter

        loadDiaries()
    }

    // DB에서 일지를 불러와 목록에 표시
    private fun loadDiaries() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).growthDiaryDao()
            val list = dao.getAll()

            adapter.updateList(list)

            // 일지가 없으면 안내 문구 표시
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvDiary.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvDiary.visibility = View.VISIBLE
            }
        }
    }

    // 화면에 다시 돌아왔을 때 목록 새로고침
    override fun onResume() {
        super.onResume()
        loadDiaries()
    }
    // 점 세 개 누르면 수정/삭제 메뉴 표시
    private fun showPopupMenu(diary: GrowthDiary, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("수정")
        popup.menu.add("삭제")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "수정" -> {
                    openEditScreen(diary)
                    true
                }
                "삭제" -> {
                    confirmDelete(diary)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // 삭제 확인 다이얼로그
    private fun confirmDelete(diary: GrowthDiary) {
        AlertDialog.Builder(requireContext())
            .setTitle("일지 삭제")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteDiary(diary)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // DB에서 일지 삭제
    // DB에서 일지 삭제
    private fun deleteDiary(diary: GrowthDiary) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).growthDiaryDao()
            dao.delete(diary)

            Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
            loadDiaries()
        }
    }

    // 수정 화면 열기 (기존 일지 내용을 전달)
    private fun openEditScreen(diary: GrowthDiary) {
        val fragment = DiaryWriteFragment()
        fragment.arguments = Bundle().apply {
            putLong("diaryId", diary.id)
        }
        // 컨테이너 ID는 MainActivity 구조 확정 후 연결
        // parentFragmentManager.beginTransaction()
        //     .replace(컨테이너ID, fragment)
        //     .addToBackStack(null)
        //     .commit()
    }

    // 일지 전체 내용 팝업
    private fun showDetailDialog(diary: GrowthDiary) {
        val view = layoutInflater.inflate(R.layout.dialog_diary_detail, null)

        val tvDate = view.findViewById<TextView>(R.id.tvDetailDate)
        val tvContent = view.findViewById<TextView>(R.id.tvDetailContent)

        val format = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
        tvDate.text = format.format(Date(diary.diaryDate))
        tvContent.text = diary.content

        // 사진이 있으면 표시
        val ivPhoto = view.findViewById<ImageView>(R.id.ivDetailPhoto)
        if (diary.imageUrl.isNotEmpty()) {
            ivPhoto.visibility = View.VISIBLE
            ivPhoto.setImageURI(android.net.Uri.parse(diary.imageUrl))
        }

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("닫기", null)
            .show()
    }
}