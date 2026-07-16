package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NutritionLog
import com.example.data.User
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NutritionViewModel,
    onLogout: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeGroup by viewModel.activeGroup.collectAsState()
    val isCollaborativeMode by viewModel.isCollaborativeMode.collectAsState()
    val logs by viewModel.currentLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val users by viewModel.users.collectAsState()

    val coachAnalysisText by viewModel.coachAnalysisText.collectAsState()
    val isCoachLoading by viewModel.isCoachLoading.collectAsState()

    var showAddManualDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Diario/Calendario, 1 = Estadísticas

    // Calculate daily totals for stats
    val totalCalories = logs.sumOf { it.calories }
    val totalCarbs = logs.sumOf { it.carbs }
    val totalProteins = logs.sumOf { it.proteins }
    val totalFats = logs.sumOf { it.fats }
    val totalSugar = logs.sumOf { it.sugar }

    val calorieTarget = 2000.0
    val proteinTarget = 100.0
    val sugarLimit = 50.0

    Scaffold(
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { showAddManualDialog = true },
                    containerColor = TealPrimary,
                    contentColor = WhiteSurface,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Manual")
                }
            }
        },
        containerColor = CreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Custom Clean Minimalism Header Section (mimicking HTML template)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isCollaborativeMode && activeGroup != null) {
                            "GRUPO: ${activeGroup?.name?.uppercase()}"
                        } else {
                            "MI ESPACIO DIARIO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Hola, ${activeUser?.name ?: "Usuario"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Collaborative Overlapping Users + Custom Logout Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCollaborativeMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-8).dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            users.take(3).forEachIndexed { index, u ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index % 3) {
                                                0 -> Color(0xFFD1FAE5) // Soft emerald
                                                1 -> Color(0xFFFFEDD5) // Soft orange
                                                else -> Color(0xFFDBEAFE) // Soft blue
                                            }
                                        )
                                        .border(1.5.dp, WhiteSurface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(u.avatarEmoji, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // Logout/Profile Switcher Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(WhiteSurface)
                            .border(1.dp, BorderLight, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Cambiar Perfil / Salir",
                            tint = TextMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Screen Toggles: Shared vs Individual & Tabs
            HeaderControls(
                viewModel = viewModel,
                isCollaborativeMode = isCollaborativeMode,
                users = users,
                activeUser = activeUser,
                currentTab = currentTab,
                onTabChanged = { currentTab = it }
            )

            // Dynamic view based on tab selection
            if (currentTab == 0) {
                // Calendar and Meals Log Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    item {
                        InteractiveCalendar(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.selectDate(it) },
                            viewModel = viewModel
                        )
                    }

                    item {
                        // AI Input Section in line
                        GeminiInputComponent(
                            viewModel = viewModel,
                            onSuccess = {
                                // optional feedback or scroll
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Registros del Día (${selectedDate})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (logs.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    } else {
                        items(logs) { log ->
                            val logUser = users.find { it.id == log.userId }
                            MealLogCard(
                                log = log,
                                user = logUser,
                                onDelete = { viewModel.deleteLog(log.id) }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // Statistics and Health Coach Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatsSummaryCard(
                            totalCalories = totalCalories,
                            calorieTarget = calorieTarget,
                            totalCarbs = totalCarbs,
                            totalProteins = totalProteins,
                            proteinTarget = proteinTarget,
                            totalFats = totalFats,
                            totalSugar = totalSugar,
                            sugarLimit = sugarLimit
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        NutritionCoachSection(
                            isCoachLoading = isCoachLoading,
                            coachAnalysisText = coachAnalysisText,
                            onTriggerCoach = { viewModel.fetchCoachAnalysis() },
                            onDismiss = { viewModel.dismissCoachAnalysis() }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        VisualChartsCard(allLogs = allLogs, selectedDate = selectedDate)
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // Manual Log Dialog
    if (showAddManualDialog) {
        AddManualLogDialog(
            onDismiss = { showAddManualDialog = false },
            onConfirm = { name, cal, carbs, prot, fat, sug, micro, type ->
                viewModel.addManualLog(name, cal, carbs, prot, fat, sug, micro, type)
                showAddManualDialog = false
            }
        )
    }
}

@Composable
fun HeaderControls(
    viewModel: NutritionViewModel,
    isCollaborativeMode: Boolean,
    users: List<User>,
    activeUser: User?,
    currentTab: Int,
    onTabChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CreamBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Shared vs Individual Toggle Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(BorderLight.copy(alpha = 0.5f))
                .border(1.dp, BorderLight, RoundedCornerShape(30.dp))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isCollaborativeMode) TealPrimary else Color.Transparent)
                    .clickable { viewModel.toggleCollaborativeMode(true) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Calendario Compartido 👥",
                    color = if (isCollaborativeMode) WhiteSurface else TextMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (!isCollaborativeMode) TealPrimary else Color.Transparent)
                    .clickable { viewModel.toggleCollaborativeMode(false) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mi Calendario 👤",
                    color = if (!isCollaborativeMode) WhiteSurface else TextMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Row (Logs vs Stats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onTabChanged(0) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == 0) SageLight else WhiteSurface,
                    contentColor = TealPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (currentTab == 0) TealPrimary else BorderLight
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (currentTab == 0) TealPrimary else TextLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Diario de Comida",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (currentTab == 0) TealPrimary else TextMedium
                )
            }

            Button(
                onClick = { onTabChanged(1) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == 1) SageLight else WhiteSurface,
                    contentColor = TealPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (currentTab == 1) TealPrimary else BorderLight
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (currentTab == 1) TealPrimary else TextLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estadísticas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (currentTab == 1) TealPrimary else TextMedium
                )
            }
        }
    }
}

@Composable
fun InteractiveCalendar(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    viewModel: NutritionViewModel
) {
    // Generate dates for current week (Centered around today)
    val dates = remember {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val list = mutableListOf<Pair<String, String>>() // Pair of DateString, DayAbbreviation
        
        // Go back 3 days and forward 3 days
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val numFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val dayAbbrev = dayFormat.format(cal.time).uppercase() + " " + numFormat.format(cal.time)
            list.add(Pair(dateStr, dayAbbrev))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Calendario Semanal",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dates.forEach { (dateStr, label) ->
                    val isSelected = selectedDate == dateStr
                    val parts = label.split(" ")
                    val dayName = parts.getOrNull(0) ?: ""
                    val dayNum = parts.getOrNull(1) ?: ""

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) TealPrimary else Color.Transparent
                            )
                            .clickable { onDateSelected(dateStr) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .width(40.dp)
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) WhiteSurface else TextLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayNum,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) WhiteSurface else TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Dot for entries presence (simulated simple dot)
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SoftGold else SageSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay comidas registradas hoy",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Describe tu plato en la caja inteligente de arriba o presiona el botón '+' para agregarlo manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMedium,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun MealLogCard(
    log: NutritionLog,
    user: User?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal type icon in a stylish rounded square
            val (bgColor, tintColor, icon) = when (log.mealType.lowercase()) {
                "breakfast" -> Triple(SageLight, TealPrimary, Icons.Default.WbSunny)
                "lunch" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), Icons.Default.Restaurant)
                "dinner" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Icons.Default.NightsStay)
                else -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Icons.Default.Fastfood)
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, tintColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log.foodName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Colaborador badge
                    if (user != null) {
                        Surface(
                            color = SageLight,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SageBorder),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user.avatarEmoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = user.name.substringBefore(" "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Macros breakdown
                val mealTypeNameStr = when (log.mealType.lowercase()) {
                    "breakfast" -> "Desayuno"
                    "lunch" -> "Almuerzo"
                    "dinner" -> "Cena"
                    else -> "Snack"
                }

                Text(
                    text = "$mealTypeNameStr  •  ${log.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Modern horizontal wrap list of macro pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MacroPill(label = "P: ${log.proteins.toInt()}g", color = SalmonPink)
                    MacroPill(label = "C: ${log.carbs.toInt()}g", color = SoftBlue)
                    MacroPill(label = "G: ${log.fats.toInt()}g", color = SoftGold)
                    if (log.sugar > 0) {
                        MacroPill(label = "A: ${log.sugar.toInt()}g", color = SunsetOrange)
                    }
                }

                if (log.micronutrients.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Micros: ${log.micronutrients}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = SunsetOrange.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MacroPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatsSummaryCard(
    totalCalories: Double,
    calorieTarget: Double,
    totalCarbs: Double,
    totalProteins: Double,
    proteinTarget: Double,
    totalFats: Double,
    totalSugar: Double,
    sugarLimit: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Resumen Nutricional del Día",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Calories circle and progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular progress indicator using Canvas
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressValue = (totalCalories / calorieTarget).toFloat().coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = BorderLight.copy(alpha = 0.5f),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = TealPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * progressValue,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${totalCalories.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "/${calorieTarget.toInt()} kcal",
                            fontSize = 11.sp,
                            color = TextLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(22.dp))

                // Summary stats list
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proteínas (Meta: ${proteinTarget.toInt()}g)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMedium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (totalProteins / proteinTarget).toFloat().coerceIn(0f, 1f) },
                        color = SalmonPink,
                        trackColor = BorderLight.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${totalProteins.toInt()}g consumidos",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Azúcares (Límite: ${sugarLimit.toInt()}g)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMedium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (totalSugar / sugarLimit).toFloat().coerceIn(0f, 1f) },
                        color = if (totalSugar > sugarLimit) SunsetOrange else SoftGold,
                        trackColor = BorderLight.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${totalSugar.toInt()}g consumidos",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalSugar > sugarLimit) SunsetOrange else TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Divider(color = BorderLight)

            Spacer(modifier = Modifier.height(16.dp))

            // Other macro breakdown in 3 beautiful equal columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Carbohidratos", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${totalCarbs.toInt()}g", fontSize = 16.sp, color = TextDark, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Grasas", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${totalFats.toInt()}g", fontSize = 16.sp, color = TextDark, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Azúcares", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${totalSugar.toInt()}g", fontSize = 16.sp, color = if (totalSugar > sugarLimit) SunsetOrange else TextDark, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun NutritionCoachSection(
    isCoachLoading: Boolean,
    coachAnalysisText: String?,
    onTriggerCoach: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SageLight),
        border = BorderStroke(1.dp, SageBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(WhiteSurface)
                        .border(1.dp, SageBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Health Coach con Gemini IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealDark
                )
            }

            Text(
                text = "Tu coach privado analiza tus comidas y las de tu grupo de los últimos días para generar recomendaciones nutricionales personalizadas.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMedium,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (coachAnalysisText == null) {
                Button(
                    onClick = onTriggerCoach,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = WhiteSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isCoachLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isCoachLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WhiteSurface, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizando tendencias...")
                    } else {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar Análisis Nutricional ✨", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    color = WhiteSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = coachAnalysisText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COACH INSIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            
                            TextButton(
                                onClick = onDismiss
                            ) {
                                Text("Cerrar", color = SunsetOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualChartsCard(
    allLogs: List<NutritionLog>,
    selectedDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Evolución de Calorías Diarias",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Histórico de calorías registradas para visualizar tendencias colaborativas.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Group calories by date
            val groupedData = remember(allLogs) {
                allLogs.groupBy { it.date }.mapValues { (_, logs) ->
                    logs.sumOf { it.calories }
                }.toSortedMap()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(CreamBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (groupedData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Inserta datos en el calendario para ver el gráfico.", style = MaterialTheme.typography.bodySmall, color = TextMedium)
                    }
                } else {
                    val points = groupedData.values.toList()
                    val labels = groupedData.keys.toList()
                    val maxVal = (points.maxOrNull() ?: 2000.0).coerceAtLeast(2000.0)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val stepX = if (points.size > 1) width / (points.size - 1) else width
                        val path = Path()

                        points.forEachIndexed { index, cal ->
                            val x = index * stepX
                            val y = height - ((cal / maxVal).toFloat() * (height - 20f))
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            // Draw point circles
                            drawCircle(
                                color = SalmonPink,
                                radius = 5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        // Draw path lines
                        drawPath(
                            path = path,
                            color = TealPrimary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Labels under chart
            if (groupedData.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    groupedData.keys.forEach { date ->
                        val label = date.substringAfter("-") // MM-DD
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, Double, Double, String, String) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var proteins by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var micronutrients by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("Breakfast") }

    val mealTypes = listOf("Breakfast" to "Desayuno 🥞", "Lunch" to "Almuerzo 🥩", "Dinner" to "Cena 🥗", "Snack" to "Snack 🍊")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Alimento Manualmente",
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Nombre del alimento") },
                        placeholder = { Text("ej: Filete de lenguado") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = BorderLight,
                            focusedLabelColor = TealPrimary
                        )
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text("Calorías (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                        OutlinedTextField(
                            value = proteins,
                            onValueChange = { proteins = it },
                            label = { Text("Proteínas (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            label = { Text("Carbohidratos (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                        OutlinedTextField(
                            value = fats,
                            onValueChange = { fats = it },
                            label = { Text("Grasas (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sugar,
                            onValueChange = { sugar = it },
                            label = { Text("Azúcar (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                        OutlinedTextField(
                            value = micronutrients,
                            onValueChange = { micronutrients = it },
                            label = { Text("Micronutrientes") },
                            placeholder = { Text("Hierro, Vitamina C...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = TealPrimary
                            )
                        )
                    }
                }

                item {
                    Text(
                        text = "Tipo de Comida:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mealTypes.forEach { (typeKey, label) ->
                            val isSelected = mealType == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) TealPrimary else WhiteSurface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TealPrimary else BorderLight,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { mealType = typeKey }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.split(" ").last(), // just show emoji
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (foodName.isNotBlank()) {
                        onConfirm(
                            foodName,
                            calories.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            proteins.toDoubleOrNull() ?: 0.0,
                            fats.toDoubleOrNull() ?: 0.0,
                            sugar.toDoubleOrNull() ?: 0.0,
                            micronutrients,
                            mealType
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Añadir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SunsetOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}
