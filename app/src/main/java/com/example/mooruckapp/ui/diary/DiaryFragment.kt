package com.example.mooruckapp.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import kotlinx.coroutines.launch

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
                // 카드 클릭 시 동작 (팝업은 이후 구현)
            },
            onMoreClick = { diary, anchor ->
                // 점 세 개 클릭 시 동작 (수정/삭제는 이후 구현)
            }
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
}