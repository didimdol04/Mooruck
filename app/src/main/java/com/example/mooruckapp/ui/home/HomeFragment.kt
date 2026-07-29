package com.example.mooruckapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.data.local.entity.WateringRecord
//import com.example.mooruckapp.databinding.ActivityHomeBinding
//추가
import com.example.mooruckapp.databinding.FragmentHomeBinding
import com.example.mooruckapp.ui.diary.DiaryFragment
import com.example.mooruckapp.ui.mypage.MyPageFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
//추가2
import com.example.mooruckapp.ui.plant.PlantDetailFragment
import com.example.mooruckapp.ui.plant.PlantRegisterFragment

class HomeFragment : Fragment() {

    //private var _binding: ActivityHomeBinding? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var plantAdapter: HomePlantAdapter

    private val database by lazy {
        AppDatabase.getInstance(requireContext().applicationContext)
    }

    /*
     * DB에서 받아온 실제 식물 목록.
     * 물주기 버튼을 누른 뒤 화면을 다시 계산할 때 사용한다.
     */
    private var userPlants: List<UserPlant> = emptyList()

    /*
     * UserPlant를 홈 화면용 HomePlantItem으로 변환한 목록.
     * 검색할 때 이 목록을 기준으로 필터링한다.
     */
    private var homePlantItems: List<HomePlantItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentHomeBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        setupPlantList()
        observePlants()
        setupSearch()
        setupButtons()
    }

    private fun setupPlantList() {
        binding.recyclerViewPlants.layoutManager =
            GridLayoutManager(
                requireContext(),
                2,
            )

        plantAdapter = HomePlantAdapter(
            plants = emptyList(),

            /*
             * 화면 연결은 나중에 처리한다.
             * 현재는 카드 클릭 시 아무 동작도 하지 않는다.
             */
            onPlantClick = { plant ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PlantDetailFragment.newInstance(plant.id),
                    )
                    .addToBackStack(null)
                    .commit()
            },

            onWaterClick = { plant ->
                saveWateringRecord(plant)
            },
        )

        binding.recyclerViewPlants.adapter = plantAdapter
    }

    /**
     * UserPlant 테이블을 실시간으로 구독한다.
     *
     * 식물이 등록·수정·삭제되면 observeAll()이 새로운 목록을 전달하고,
     * 받은 목록을 HomePlantItem으로 변환해 RecyclerView에 표시한다.
     */
    private fun observePlants() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED,
            ) {
                database.userPlantDao()
                    .observeAll()
                    .collect { plants ->

                        userPlants = plants
                        refreshHomePlantItems()
                    }
            }
        }
    }

    /**
     * 실제 UserPlant 목록을 HomePlantItem 목록으로 변환한다.
     *
     * 각 식물의 마지막 물주기 기록을 조회하고,
     * wateringIntervalDays를 사용해 홈 카드 문구를 계산한다.
     */
    private suspend fun refreshHomePlantItems() {
        homePlantItems = userPlants
            .map { userPlant ->

                val lastWateredDate =
                    database.wateringRecordDao()
                        .getLastWateredDate(userPlant.id)

                Triple(
                    HomePlantItem(
                        id = userPlant.id,
                        plantName = userPlant.plantName,
                        nickname = userPlant.nickname,
                        profileImageUri = userPlant.profileImageUri,
                        wateringMessage = makeWateringMessage(
                            plant = userPlant,
                            lastWateredDate = lastWateredDate,
                        ),
                    ),
                    remainingWaterDays(
                        plant = userPlant,
                        lastWateredDate = lastWateredDate,
                    ),
                    userPlant.plantName,
                )
            }
            .sortedWith(
                compareBy<Triple<HomePlantItem, Long, String>>(
                    { it.second },
                    { it.third },
                ),
            )
            .map { it.first }

        applySearchFilter()
    }

    private fun setupSearch() {
        binding.editTextSearch.addTextChangedListener {
            applySearchFilter()
        }
    }

    /**
     * 현재 검색창에 입력된 문자를 기준으로 식물 이름과 별명을 검색한다.
     *
     * 등록된 식물이 하나도 없으면 빈 목록 안내 문구를 표시한다.
     */
    private fun applySearchFilter() {
        val keyword = binding.editTextSearch.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val filteredPlants =
            if (keyword.isBlank()) {
                homePlantItems
            } else {
                homePlantItems.filter { plant ->

                    plant.plantName.contains(
                        other = keyword,
                        ignoreCase = true,
                    ) ||
                            plant.nickname?.contains(
                                other = keyword,
                                ignoreCase = true,
                            ) == true
                }
            }

        binding.textEmptyPlants.visibility =
            if (homePlantItems.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        plantAdapter.updatePlants(filteredPlants)
    }

    /**
     * 물주기 버튼 클릭 시 오늘 날짜의 물주기 기록을 추가한다.
     */
    private fun saveWateringRecord(
        plant: HomePlantItem,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {

            val today = getTodayStartMillis()

            database.wateringRecordDao().insert(
                WateringRecord(
                    userPlantId = plant.id,
                    wateredDate = today,
                ),
            )

            /*
             * WateringRecord 테이블만 변경되면 UserPlant의 observeAll()은
             * 다시 호출되지 않으므로 물주기 문구를 직접 새로 계산한다.
             */
            refreshHomePlantItems()

            val displayName =
                plant.nickname
                    ?.takeIf { it.isNotBlank() }
                    ?: plant.plantName

            Snackbar.make(
                binding.root,
                "${displayName}의 물주기 날짜를 오늘로 기록했습니다.",
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    /**
     * 마지막 물주기 날짜와 물주기 간격으로 카드 문구를 만든다.
     */
    private fun makeWateringMessage(
        plant: UserPlant,
        lastWateredDate: Long?,
    ): String {

        val baseDate =
            lastWateredDate ?: plant.plantedDate

        val dueDate =
            baseDate +
                    TimeUnit.DAYS.toMillis(
                        plant.wateringIntervalDays.toLong(),
                    )

        val today = getTodayStartMillis()

        val dayDifference =
            TimeUnit.MILLISECONDS.toDays(
                dueDate - today,
            )

        return when {
            dayDifference > 0L -> {
                "${dayDifference}일 후 물을 주세요"
            }

            dayDifference == 0L -> {
                "오늘 물을 주세요"
            }

            else -> {
                "물주기 날짜가 ${-dayDifference}일 지났어요"
            }
        }
    }

    /**
     * 오늘 오전 0시의 epoch millis를 반환한다.
     */
    private fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun setupButtons() {
        binding.buttonAddPlant.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PlantRegisterFragment(),
                )
                .addToBackStack(null)
                .commit()
        }

        binding.buttonDiary.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    DiaryFragment(),
                )
                .addToBackStack(null)
                .commit()
        }

        binding.buttonMyPage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    MyPageFragment(),
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun remainingWaterDays(
        plant: UserPlant,
        lastWateredDate: Long?,
    ): Long {

        val baseDate =
            lastWateredDate ?: plant.plantedDate

        val dueDate =
            baseDate +
                    TimeUnit.DAYS.toMillis(
                        plant.wateringIntervalDays.toLong(),
                    )

        return TimeUnit.MILLISECONDS.toDays(
            dueDate - getTodayStartMillis(),
        )
    }

    override fun onDestroyView() {
        binding.recyclerViewPlants.adapter = null
        _binding = null

        super.onDestroyView()
    }
}