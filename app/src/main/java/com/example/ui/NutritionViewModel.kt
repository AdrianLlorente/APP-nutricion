package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = NutritionRepository(db.nutritionDao())

    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val groups = repository.allGroups.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate

    private val _activeUser = MutableStateFlow<User?>(null)
    val activeUser: StateFlow<User?> = _activeUser

    private val _activeGroup = MutableStateFlow<Group?>(null)
    val activeGroup: StateFlow<Group?> = _activeGroup

    private val _isCollaborativeMode = MutableStateFlow(true)
    val isCollaborativeMode: StateFlow<Boolean> = _isCollaborativeMode

    // Combine date, user, group, and collaborative mode to reactively load the food logs
    val currentLogs: StateFlow<List<NutritionLog>> = combine(
        _selectedDate,
        _activeUser,
        _activeGroup,
        _isCollaborativeMode
    ) { date, user, group, collaborative ->
        if (collaborative && group != null) {
            repository.getLogsForGroupAndDate(group.id, date)
        } else if (user != null) {
            repository.getLogsForUserAndDate(user.id, date)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All logs to populate the stats dashboard
    val allLogs: StateFlow<List<NutritionLog>> = combine(
        _activeUser,
        _activeGroup,
        _isCollaborativeMode
    ) { user, group, collaborative ->
        if (collaborative && group != null) {
            repository.getLogsForGroup(group.id)
        } else if (user != null) {
            repository.getLogsForUser(user.id)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    private val _extractionError = MutableStateFlow<String?>(null)
    val extractionError: StateFlow<String?> = _extractionError

    private val _coachAnalysisText = MutableStateFlow<String?>(null)
    val coachAnalysisText: StateFlow<String?> = _coachAnalysisText

    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading

    init {
        // Prepopulate db
        viewModelScope.launch {
            repository.allUsers.first().let { existingUsers ->
                if (existingUsers.isEmpty()) {
                    prepopulateDatabase()
                } else {
                    _activeUser.value = existingUsers.firstOrNull()
                    repository.allGroups.first().let { existingGroups ->
                        _activeGroup.value = existingGroups.firstOrNull()
                    }
                }
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val defaultGroup = Group("team_couple", "Reto Pareja Fit 🥑💪")
        repository.insertGroup(defaultGroup)

        val ana = User("ana", "Ana (Tú)", "🥑", "team_couple")
        val carlos = User("carlos", "Carlos (Pareja)", "🏃‍♂️", "team_couple")
        val eduardo = User("eduardo", "Eduardo (Coach)", "📋", "team_couple")

        repository.insertUser(ana)
        repository.insertUser(carlos)
        repository.insertUser(eduardo)

        _activeUser.value = ana
        _activeGroup.value = defaultGroup

        // Preload some logs over last 3 days
        val today = getCurrentDateString()
        val yesterday = getRelativeDateString(-1)
        val dayBefore = getRelativeDateString(-2)

        // Today's logs
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = today, foodName = "Tortilla de espinacas y tostada integral", calories = 320.0, carbs = 25.0, proteins = 18.0, fats = 14.0, sugar = 1.0, micronutrients = "Hierro, Calcio, Vitamina A", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = today, foodName = "Batido de proteínas de fresa y avena", calories = 450.0, carbs = 48.0, proteins = 35.0, fats = 8.0, sugar = 3.5, micronutrients = "Potasio, Magnesio", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = today, foodName = "Salmón a la plancha con puré de calabaza", calories = 520.0, carbs = 30.0, proteins = 38.0, fats = 22.0, sugar = 2.0, micronutrients = "Omega-3, Vitamina D, Fósforo", mealType = "Lunch"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = today, foodName = "Pechuga de pollo con arroz integral y brócoli", calories = 580.0, carbs = 65.0, proteins = 45.0, fats = 10.0, sugar = 0.5, micronutrients = "Hierro, Vitamina C, Zinc", mealType = "Lunch"))

        // Yesterday's logs
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = yesterday, foodName = "Yogur griego con frutos rojos y chía", calories = 240.0, carbs = 18.0, proteins = 14.0, fats = 10.0, sugar = 5.0, micronutrients = "Calcio, Antioxidantes", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = yesterday, foodName = "Tostadas con aguacate y huevo poché", calories = 410.0, carbs = 32.0, proteins = 16.0, fats = 20.0, sugar = 1.2, micronutrients = "Grasas saludables, Potasio", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = yesterday, foodName = "Ensalada César con pollo y aderezo ligero", calories = 380.0, carbs = 15.0, proteins = 28.0, fats = 18.0, sugar = 1.5, micronutrients = "Vitamina K, Calcio", mealType = "Lunch"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = yesterday, foodName = "Pasta integral boloñesa de pavo", calories = 620.0, carbs = 75.0, proteins = 38.0, fats = 12.0, sugar = 4.0, micronutrients = "Licopeño, Hierro", mealType = "Lunch"))
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = yesterday, foodName = "Sopa de verduras y merluza al vapor", calories = 290.0, carbs = 20.0, proteins = 25.0, fats = 6.0, sugar = 2.5, micronutrients = "Yodo, Fibra, Vitamina A", mealType = "Dinner"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = yesterday, foodName = "Revuelto de claras con champiñones y pavo", calories = 260.0, carbs = 8.0, proteins = 32.0, fats = 5.0, sugar = 1.0, micronutrients = "Riboflavina, Selenio", mealType = "Dinner"))

        // Day before yesterday
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = dayBefore, foodName = "Tazón de avena con plátano y nueces", calories = 340.0, carbs = 52.0, proteins = 10.0, fats = 12.0, sugar = 8.0, micronutrients = "Fibra soluble, Omega-6", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "carlos", groupId = "team_couple", date = dayBefore, foodName = "Gachas de avena hiperproteicas", calories = 480.0, carbs = 55.0, proteins = 30.0, fats = 10.0, sugar = 4.5, micronutrients = "Magnesio, Hierro", mealType = "Breakfast"))
        repository.insertLog(NutritionLog(userId = "ana", groupId = "team_couple", date = dayBefore, foodName = "Tacos de lechuga con carne picada picante", calories = 420.0, carbs = 12.0, proteins = 30.0, fats = 24.0, sugar = 1.0, micronutrients = "Zinc, Hierro", mealType = "Dinner"))
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun selectUser(user: User) {
        _activeUser.value = user
    }

    fun selectGroup(group: Group?) {
        _activeGroup.value = group
        if (group == null) {
            _isCollaborativeMode.value = false
        }
    }

    fun toggleCollaborativeMode(collaborative: Boolean) {
        _isCollaborativeMode.value = collaborative
    }

    fun addManualLog(
        foodName: String,
        calories: Double,
        carbs: Double,
        proteins: Double,
        fats: Double,
        sugar: Double,
        micronutrients: String,
        mealType: String
    ) {
        val user = _activeUser.value ?: return
        val group = _activeGroup.value
        viewModelScope.launch {
            val log = NutritionLog(
                userId = user.id,
                groupId = group?.id,
                date = _selectedDate.value,
                foodName = foodName,
                calories = calories,
                carbs = carbs,
                proteins = proteins,
                fats = fats,
                sugar = sugar,
                micronutrients = micronutrients,
                mealType = mealType
            )
            repository.insertLog(log)
        }
    }

    fun addSmartLog(userText: String, onCompleted: (Boolean) -> Unit) {
        val user = _activeUser.value
        if (user == null) {
            onCompleted(false)
            return
        }
        val group = _activeGroup.value
        val dateString = _selectedDate.value

        _isExtracting.value = true
        _extractionError.value = null

        viewModelScope.launch {
            try {
                val result = GeminiService.extractNutrition(userText)
                if (result != null) {
                    val log = NutritionLog(
                        userId = user.id,
                        groupId = group?.id,
                        date = dateString,
                        foodName = result.foodName,
                        calories = result.calories,
                        carbs = result.carbs,
                        proteins = result.proteins,
                        fats = result.fats,
                        sugar = result.sugar,
                        micronutrients = result.micronutrients,
                        mealType = result.mealType
                    )
                    repository.insertLog(log)
                    onCompleted(true)
                } else {
                    _extractionError.value = "No se pudo interpretar el alimento con IA de Gemini. Verifica la API Key en el panel de Secrets de AI Studio."
                    onCompleted(false)
                }
            } catch (e: Exception) {
                _extractionError.value = "Error en Gemini: ${e.localizedMessage}"
                onCompleted(false)
            } finally {
                _isExtracting.value = false
            }
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    fun clearExtractionError() {
        _extractionError.value = null
    }

    fun fetchCoachAnalysis() {
        _isCoachLoading.value = true
        viewModelScope.launch {
            val allLogsList = allLogs.value
            val summary = if (allLogsList.isEmpty()) {
                "No hay registros de alimentación guardados en el sistema todavía."
            } else {
                allLogsList.groupBy { it.date }.map { (date, logs) ->
                    val totalCalories = logs.sumOf { it.calories }
                    val totalCarbs = logs.sumOf { it.carbs }
                    val totalProteins = logs.sumOf { it.proteins }
                    val totalFats = logs.sumOf { it.fats }
                    val totalSugar = logs.sumOf { it.sugar }
                    "Fecha: $date -> Total calorías: $totalCalories kcal, Carbohidratos: ${totalCarbs}g, Proteínas: ${totalProteins}g, Grasas: ${totalFats}g, Azúcar: ${totalSugar}g. Detalle: " +
                            logs.joinToString(", ") { "${it.foodName} (${it.calories} kcal por ${it.userId})" }
                }.joinToString("\n")
            }

            val analysis = GeminiService.generateCoachAnalysis(summary)
            _coachAnalysisText.value = analysis
            _isCoachLoading.value = false
        }
    }

    fun dismissCoachAnalysis() {
        _coachAnalysisText.value = null
    }

    fun createNewUser(name: String, emoji: String) {
        val id = "user_" + UUID.randomUUID().toString().take(6)
        val user = User(id = id, name = name, avatarEmoji = emoji, groupId = "team_couple")
        viewModelScope.launch {
            repository.insertUser(user)
            _activeUser.value = user
        }
    }

    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getRelativeDateString(offset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
