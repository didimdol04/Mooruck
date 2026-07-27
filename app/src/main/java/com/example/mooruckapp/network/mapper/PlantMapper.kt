object PlantMapper {

    fun waterCycleCodeToDays(code: String): Int {
        return when (code) {
            "053001" -> 1
            "053002" -> 3
            "053003" -> 7
            "053004" -> 14
            else -> 7
        }
    }
}