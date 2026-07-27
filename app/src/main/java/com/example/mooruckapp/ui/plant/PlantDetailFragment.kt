package com.example.mooruckapp.ui.plant

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mooruckapp.databinding.FragmentPlantDetailBinding

class PlantDetailFragment : Fragment() {

    // ViewBinding 객체
    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    // 전달받은 식물 ID를 저장한다.
    private var plantId: Long = INVALID_PLANT_ID

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        // arguments에서 전달받은 식물 ID를 저장한다.
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
        super.onViewCreated(view, savedInstanceState)

        // TODO: PlantListFragment 구현 후 전달받은 plantId를 이용해
        // Room에서 식물 정보를 조회한다.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        // Bundle에서 사용할 Key
        private const val ARG_PLANT_ID = "plant_id"

        // 전달되지 않았음을 의미하는 값
        private const val INVALID_PLANT_ID = -1L

        /**
         * 식물 상세 화면 생성
         */
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