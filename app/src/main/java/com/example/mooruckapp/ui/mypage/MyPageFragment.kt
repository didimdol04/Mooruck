package com.example.mooruckapp.ui.mypage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.data.local.entity.User
import com.example.mooruckapp.domain.WateringScoreCalculator
import com.example.mooruckapp.notification.WateringNotificationScheduler
import com.example.mooruckapp.ui.common.WaterDropScoreView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 마이페이지 화면
 * 닉네임 조회/수정, 등록 식물 개수, 전체 물주기 점수(물방울 표시), 물주기 알림 설정
 */
class MyPageFragment : Fragment() {

    private lateinit var tvNickname: TextView
    private lateinit var btnEditNickname: TextView
    private lateinit var tvPlantCount: TextView
    private lateinit var waterDropScoreView: WaterDropScoreView
    private lateinit var tvScoreText: TextView
    private lateinit var switchNotification: Switch
    private lateinit var tvAppVersion: TextView

    private var currentNickname: String = ""

    /** 스위치를 켤 때(API 33+)만 필요한 알림 권한 요청 런처 */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            applyNotificationEnabled(true)
        } else {
            Toast.makeText(requireContext(), "알림 권한이 없으면 물주기 알림을 받을 수 없어요", Toast.LENGTH_SHORT).show()
            setSwitchCheckedSilently(false)
        }
    }

    private val notificationSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked && needsNotificationPermissionRequest()) {
            // 실제 활성화는 권한 요청 결과(notificationPermissionLauncher)에서 처리
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            applyNotificationEnabled(isChecked)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNickname = view.findViewById(R.id.tvNickname)
        btnEditNickname = view.findViewById(R.id.btnEditNickname)
        tvPlantCount = view.findViewById(R.id.tvPlantCount)
        waterDropScoreView = view.findViewById(R.id.waterDropScoreView)
        tvScoreText = view.findViewById(R.id.tvScoreText)
        switchNotification = view.findViewById(R.id.switchNotification)
        tvAppVersion = view.findViewById(R.id.tvAppVersion)

        // 앱 버전은 텍스트 고정 표기 (v1.0.0) - build.gradle의 buildConfig 기능이
        // 꺼져 있어 BuildConfig.VERSION_NAME은 사용하지 않음.
        tvAppVersion.text = "v1.0.0"
        btnEditNickname.setOnClickListener { showEditNicknameDialog() }

        // 매일 물주기 확인 작업 등록 (이미 등록돼 있으면 동작 X)
        WateringNotificationScheduler.schedule(requireContext())

        loadMyPageData()
    }

    private fun loadMyPageData() {
        val db = AppDatabase.getInstance(requireContext())

        lifecycleScope.launch {
            // 로그인 없는 앱이라, 아직 row가 없으면 여기서 기본 사용자 row를 만들어둔다.
            db.userDao().insertIfNotExists(User(nickname = "식집사"))

            val user = db.userDao().getUser() ?: return@launch
            currentNickname = user.nickname
            tvNickname.text = user.nickname

            // 리스너 달기 전에 먼저 초기값을 세팅해서, 세팅하면서 리스너가 불필요하게 호출되지 않게 함
            switchNotification.isChecked = user.wateringNotificationEnabled
            switchNotification.setOnCheckedChangeListener(notificationSwitchListener)

            val plants = db.userPlantDao().observeAll().first()
            tvPlantCount.text = "${plants.size}개"

            val scoreCalculator = WateringScoreCalculator(db.wateringRecordDao())
            val overallScore = scoreCalculator.calculateOverallScore(plants)
            waterDropScoreView.score = overallScore
            tvScoreText.text = "${overallScore}점"
        }
    }

    /** API 33 미만은 런타임 권한 자체가 없어서 항상 false */
    private fun needsNotificationPermissionRequest(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        return !granted
    }

    private fun applyNotificationEnabled(enabled: Boolean) {
        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).userDao().updateNotificationEnabled(enabled)
        }
    }

    /** 리스너를 잠깐 떼고 값을 바꿔서, 프로그램적으로 되돌릴 때 리스너가 다시 불리지 않게 함 */
    private fun setSwitchCheckedSilently(checked: Boolean) {
        switchNotification.setOnCheckedChangeListener(null)
        switchNotification.isChecked = checked
        switchNotification.setOnCheckedChangeListener(notificationSwitchListener)
    }

    private fun showEditNicknameDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentNickname)
            setSelection(currentNickname.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("닉네임 수정")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                val newNickname = editText.text.toString().trim()
                if (newNickname.isEmpty()) {
                    Toast.makeText(requireContext(), "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    AppDatabase.getInstance(requireContext()).userDao().updateNickname(newNickname)
                    currentNickname = newNickname
                    tvNickname.text = newNickname
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
