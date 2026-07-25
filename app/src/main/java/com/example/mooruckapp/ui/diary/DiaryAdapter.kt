package com.example.mooruckapp.ui.diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.GrowthDiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryAdapter(
    private var diaryList: List<GrowthDiary>,
    private val onItemClick: (GrowthDiary) -> Unit,
    private val onMoreClick: (GrowthDiary, View) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    // 카드 한 장에 들어가는 뷰들
    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvWeekday: TextView = itemView.findViewById(R.id.tvWeekday)
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        val layoutCard: View = itemView.findViewById(R.id.layoutCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        // item_diary 레이아웃을 불러와서 카드 생성
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaryList[position]

        // 저장된 날짜를 "25일", "수요일" 형태로 표시
        val date = Date(diary.diaryDate)
        holder.tvDay.text = SimpleDateFormat("d일", Locale.KOREAN).format(date)
        holder.tvWeekday.text = SimpleDateFormat("EEEE", Locale.KOREAN).format(date)

        holder.tvContent.text = diary.content

        // 사진은 이후 Glide로 연결 예정

        // 카드를 누르면 전체 내용 팝업
        holder.layoutCard.setOnClickListener { onItemClick(diary) }

        // 점 세 개를 누르면 수정/삭제 메뉴
        holder.btnMore.setOnClickListener { onMoreClick(diary, holder.btnMore) }
    }

    override fun getItemCount(): Int = diaryList.size

    // 목록이 바뀌면 새로 그림
    fun updateList(newList: List<GrowthDiary>) {
        diaryList = newList
        notifyDataSetChanged()
    }
}