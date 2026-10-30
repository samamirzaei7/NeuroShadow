package com.harvard.neuroshadow

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// ============================================================================
// 1. NEURO-PSYCHOANALYTIC DATA MODELS & STATE MACHINE
// ============================================================================

enum class AppScreen {
    ONBOARDING_SHADOW,     // Phase 1: The Mirror Room (Uncovering Secondary Gain)
    INSIGHT_CARD,          // Phase 2: Immunity Map & 3-Second Haptic Neuro-Contract
    DAILY_NEURO_LOOP,      // Phase 3: Daily Polyvagal Check-in, JITAI & Execution
    RECOVERY_LAB           // Phase 4: Zero-Shame Recovery Lab (AVE Neutralizer)
}

enum class NervousSystemState(
    val title: String,
    val subtitle: String,
    val bgColor: Color,
    val accentColor: Color
) {
    SYMPATHETIC(
        title = "Sympathetic (Anxious / Overwhelmed)",
        subtitle = "Amygdala arousal detected. Limbic friction is high. Scaling down to a 2-minute survival micro-habit to bypass threat detection.",
        bgColor = Color(0xFF1A2421),
        accentColor = Color(0xFF64B5F6)
    ),
    VENTRAL_VAGAL(
        title = "Ventral Vagal (Grounded & Focused)",
        subtitle = "Prefrontal cortex is online and regulated. Optimal window for your Standard Goal execution.",
        bgColor = Color(0xFF121824),
        accentColor = Color(0xFF7E57C2)
    ),
    DORSAL_VAGAL(
        title = "Dorsal Vagal (Exhausted / Numb / Foggy)",
        subtitle = "System is in energy conservation mode. Today is not about conquering peaks; it is strictly about keeping the synaptic pathway alive.",
        bgColor = Color(0xFF1F1B24),
        accentColor = Color(0xFF81C784)
    ),
    HIGH_DRIVE(
        title = "High Dopaminergic Drive (Flow State)",
        subtitle = "Peak autonomic readiness. Deploying your Ceiling Goal for maximum neuroplasticity.",
        bgColor = Color(0xFF1A1128),
        accentColor = Color(0xFFFFB300)
    )
}

data class PsychoanalyticGoal(
    val rawGoal: String = "",
    val escapeBehavior: String = "",
    val coreFear: String = "",
    val competingCommitment: String = "",
    val ceilingTask: String = "",
    val standardTask: String = "",
    val survivalTask: String = "",
    val ifObstacle: String = "",
    val thenAntidote: String = "",
    val resilienceScore: Int = 100,
    val synapticDensity: Int = 0,
    val vulnerabilityHour: Int = 16 // Default 16:00 (4:00 PM circadian prefrontal dip)
)

// ============================================================================
// 2. HARVARD IMMUNITY-TO-CHANGE AI ENGINE & DOPAMINE RPE ALGORITHM
// ============================================================================

object PsychoanalyticAiEngine {
    private const val GEMINI_API_KEY = ""

    private val systemInstruction = """
        You are a Harvard Professor of Psychoanalysis and Neuroscience specializing in Robert Kegan's 'Immunity to Change' and Gabriele Oettingen's 'WOOP / Implementation Intentions'.
        Analyze the user's conscious goal, their escape behavior (procrastination), and their underlying fear.
        Respond STRICTLY with a valid JSON object containing these exact keys in clinical English:
        {
          "competingCommitment": "A profound 1-2 sentence psychoanalytic insight explaining the secondary gain—how their escape behavior is secretly protecting them from their core fear.",
          "ceilingTask": "A concrete, high-focus 60-90 minute version of their goal for high-energy days.",
          "standardTask": "A realistic 30-45 minute baseline version of their goal for normal days.",
          "survivalTask": "A 2-minute micro-action with zero limbic friction for exhausted or anxious days.",
          "ifObstacle": "I feel the urge to engage in [escape behavior] because of [core fear]",
          "thenAntidote": "I will immediately start the 120-second timer and produce a deliberately imperfect draft"
        }
    """.trimIndent()

    suspend fun analyzeUnconscious(
        rawGoal: String,
        escapeBehavior: String,
        coreFear: String
    ): PsychoanalyticGoal {
        if (GEMINI_API_KEY.isNotBlank()) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = GEMINI_API_KEY
                )
                val prompt = """
                    $systemInstruction
                    Conscious Goal: $rawGoal
                    Escape Behavior: $escapeBehavior
                    Core Unconscious Fear: $coreFear
                """.trimIndent()

                val response = model.generateContent(prompt).text ?: ""
                val cleanJson = response.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleanJson}")

                return PsychoanalyticGoal(
                    rawGoal = rawGoal,
                    escapeBehavior = escapeBehavior,
                    coreFear = coreFear,
                    competingCommitment = json.optString("competingCommitment"),
                    ceilingTask = json.optString("ceilingTask"),
                    standardTask = json.optString("standardTask"),
                    survivalTask = json.optString("survivalTask"),
                    ifObstacle = json.optString("ifObstacle"),
                    thenAntidote = json.optString("thenAntidote")
                )
            } catch (e: Exception) {
                // Fallback to offline clinical engine
            }
        }

        delay(1200)
        return PsychoanalyticGoal(
            rawGoal = rawGoal,
            escapeBehavior = escapeBehavior,
            coreFear = coreFear,
            competingCommitment = "Your unconscious uses '$escapeBehavior' not out of laziness, but as an active psychological shield to protect you from '$coreFear'. As long as you hold back, your ego preserves the comforting illusion of 'I could succeed if I really tried.'",
            ceilingTask = "Execute 60-90 minutes of deep, uninterrupted work on: $rawGoal (Phone in another room).",
            standardTask = "Complete 30 minutes of focused, non-perfectionist execution on: $rawGoal.",
            survivalTask = "2-Minute Survival Protocol: Open your workspace for '$rawGoal' and do 1% of the task as imperfectly as possible.",
            ifObstacle = "my ego triggers '$escapeBehavior' to avoid facing '$coreFear'",
            thenAntidote = "I will trigger the 120-second SOS timer and give myself permission to create terrible output"
        )
    }

    fun generateDopamineReward(state: NervousSystemState, synapticCount: Int): String {
        val roll = Random.nextFloat()
        return when {
            roll < 0.35f -> "⚡ Dopamine RPE Spike (Unexpected Insight): You just overrode your Default Mode Network (DMN) right when resistance peaked. Myelin sheathing on this neural pathway just thickened significantly."
            state == NervousSystemState.SYMPATHETIC || state == NervousSystemState.DORSAL_VAGAL ->
                "🧠 Limbic Friction Victory: Executing a 2-minute micro-habit while dysregulated rewires your Basal Ganglia far more powerfully than 2 hours of work on an easy day."
            else -> "🌱 Identity Consolidation: Synaptic repetition #${synapticCount + 1} encoded in the prefrontal cortex. You are shifting from 'someone who wishes' to 'someone who executes.'"
        }
    }
}

// ============================================================================
// 3. LOCAL PERSISTENCE LAYER (SharedPreferences)
// ============================================================================

class NeuroStorage(context: Context) {
    private val prefs = context.getSharedPreferences("neuro_shadow_en_prefs", Context.MODE_PRIVATE)

    fun saveGoal(goal: PsychoanalyticGoal) {
        prefs.edit().apply {
            putString("rawGoal", goal.rawGoal)
            putString("escapeBehavior", goal.escapeBehavior)
            putString("coreFear", goal.coreFear)
            putString("competingCommitment", goal.competingCommitment)
            putString("ceilingTask", goal.ceilingTask)
            putString("standardTask", goal.standardTask)
            putString("survivalTask", goal.survivalTask)
            putString("ifObstacle", goal.ifObstacle)
            putString("thenAntidote", goal.thenAntidote)
            putInt("resilienceScore", goal.resilienceScore)
            putInt("synapticDensity", goal.synapticDensity)
            putInt("vulnerabilityHour", goal.vulnerabilityHour)
            apply()
        }
    }

    fun loadGoal(): PsychoanalyticGoal? {
        val raw = prefs.getString("rawGoal", null) ?: return null
        return PsychoanalyticGoal(
            rawGoal = raw,
            escapeBehavior = prefs.getString("escapeBehavior", "") ?: "",
            coreFear = prefs.getString("coreFear", "") ?: "",
            competingCommitment = prefs.getString("competingCommitment", "") ?: "",
            ceilingTask = prefs.getString("ceilingTask", "") ?: "",
            standardTask = prefs.getString("standardTask", "") ?: "",
            survivalTask = prefs.getString("survivalTask", "") ?: "",
            ifObstacle = prefs.getString("ifObstacle", "") ?: "",
            thenAntidote = prefs.getString("thenAntidote", "") ?: "",
            resilienceScore = prefs.getInt("resilienceScore", 100),
            synapticDensity = prefs.getInt("synapticDensity", 0),
            vulnerabilityHour = prefs.getInt("vulnerabilityHour", 16)
        )
    }

    fun clearGoal() {
        prefs.edit().clear().apply()
    }
}

// ============================================================================
// 4. WORKMANAGER JITAI ENGINE (Smart Interventional Notifications)
// ============================================================================

object NeuroNotificationHelper {
    const val CHANNEL_ID = "neuro_antidote_jitai_channel"
    const val NOTIFICATION_ID = 4040
    const val ACTION_QUICK_SYNAPSE = "com.harvard.neuroshadow.ACTION_QUICK_SYNAPSE"
    const val EXTRA_LAUNCH_SOS = "EXTRA_LAUNCH_SOS"

    fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prefrontal Antidote Interventions (JITAI)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers your If-Then neural antidote during high-risk procrastination windows."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAntidoteIntervention(context: Context, goal: PsychoanalyticGoal) {
        ensureChannelExists(context)

        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Action 1: Open App directly into the 120-Second SOS Timer Modal
        val sosIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAUNCH_SOS, true)
        }
        val sosPendingIntent = PendingIntent.getActivity(
            context,
            101,
            sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Zero-Friction Background Execution (+1 Synapse without opening app)
        val quickSynapseIntent = Intent(context, AntidoteActionReceiver::class.java).apply {
            action = ACTION_QUICK_SYNAPSE
        }
        val quickSynapsePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            quickSynapseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigBodyText = buildString {
            append("🧠 Shadow Alert: Your brain is entering its vulnerability window for '${goal.escapeBehavior}'.\n\n")
            append("⚡ YOUR IF-THEN ANTIDOTE:\n")
            append("\"IF ${goal.ifObstacle}, THEN ${goal.thenAntidote}.\"\n\n")
            append("🛡️ 2-Min Survival Version: ${goal.survivalTask}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🛑 Ego Defense Intercept: ${goal.escapeBehavior}")
            .setContentText("IF ${goal.ifObstacle} -> Deploy your 2-minute antidote now.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigBodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(sosPendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                "⏱️ 120s SOS Timer",
                sosPendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "⚡ Did 2-Min Survival (+1)",
                quickSynapsePendingIntent
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * Background Worker triggered by WorkManager during the user's high-risk procrastination hour.
 */
class AntidoteNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val storage = NeuroStorage(applicationContext)
        val goal = storage.loadGoal() ?: return Result.success()

        NeuroNotificationHelper.showAntidoteIntervention(applicationContext, goal)
        return Result.success()
    }
}

/**
 * BroadcastReceiver that handles the "⚡ Did 2-Min Survival (+1)" action button directly from
 * the notification shade, rewarding the user with a Dopamine RPE message without app friction.
 */
class AntidoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == NeuroNotificationHelper.ACTION_QUICK_SYNAPSE) {
            val storage = NeuroStorage(context)
            val currentGoal = storage.loadGoal() ?: return
            val updatedGoal = currentGoal.copy(
                synapticDensity = currentGoal.synapticDensity + 1,
                resilienceScore = currentGoal.resilienceScore + 5
            )
            storage.saveGoal(updatedGoal)

            // Replace the intervention notification with an instant Dopamine RPE reward
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val rewardNotification = NotificationCompat.Builder(context, NeuroNotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("⚡ +1 Synapse & +5 Resilience Encoded!")
                .setContentText("You bypassed limbic friction directly from the lock screen. Basal ganglia updated.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(NeuroNotificationHelper.NOTIFICATION_ID, rewardNotification)
        }
    }
}

/**
 * Schedules daily Just-In-Time Adaptive Interventions (JITAI) at the exact hour of prefrontal fatigue.
 */
object InterventionScheduler {
    private const val DAILY_WORK_NAME = "neuro_shadow_daily_jitai_work"
    private const val TEST_WORK_NAME = "neuro_shadow_test_jitai_work"

    fun scheduleDailyIntervention(context: Context, targetHour24: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelayMillis = target.timeInMillis - now.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<AntidoteNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(DAILY_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    fun triggerInstantTestIntervention(context: Context) {
        val testWorkRequest = OneTimeWorkRequestBuilder<AntidoteNotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TEST_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            testWorkRequest
        )
    }
}

// ============================================================================
// 5. MAIN ACTIVITY & ROOT COMPOSE CONTAINER (With Deep-Link SOS Intent Support)
// ============================================================================

class MainActivity : ComponentActivity() {
    private var launchSosFromNotification by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroNotificationHelper.ensureChannelExists(this)
        launchSosFromNotification = intent?.getBooleanExtra(NeuroNotificationHelper.EXTRA_LAUNCH_SOS, false) == true

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFF9575CD),
                        secondary = Color(0xFF64B5F6),
                        background = Color(0xFF121824),
                        surface = Color(0xFF1E2638)
                    )
                ) {
                    NeuroShadowApp(
                        autoOpenSos = launchSosFromNotification,
                        onSosConsumed = { launchSosFromNotification = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NeuroNotificationHelper.EXTRA_LAUNCH_SOS, false)) {
            launchSosFromNotification = true
        }
    }
}

@Composable
fun NeuroShadowApp(
    autoOpenSos: Boolean,
    onSosConsumed: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember { NeuroStorage(context) }
    var currentGoal by remember { mutableStateOf(storage.loadGoal()) }

    var currentScreen by remember {
        mutableStateOf(
            if (currentGoal == null) AppScreen.ONBOARDING_SHADOW else AppScreen.DAILY_NEURO_LOOP
        )
    }

    var nervousState by remember { mutableStateOf(NervousSystemState.VENTRAL_VAGAL) }

    // Request Android 13+ Notification Permission cleanly on startup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Permission handled */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (currentScreen == AppScreen.DAILY_NEURO_LOOP) nervousState.bgColor else Color(0xFF0F141C),
        animationSpec = tween(durationMillis = 700),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .padding(16.dp)
    ) {
        when (currentScreen) {
            AppScreen.ONBOARDING_SHADOW -> {
                OnboardingShadowScreen(
                    onAnalysisComplete = { analyzedGoal ->
                        currentGoal = analyzedGoal
                        currentScreen = AppScreen.INSIGHT_CARD
                    }
                )
            }

            AppScreen.INSIGHT_CARD -> {
                currentGoal?.let { goal ->
                    InsightCardScreen(
                        goal = goal,
                        onUpdateGoal = { updated ->
                            currentGoal = updated
                            storage.saveGoal(updated)
                            // Automatically schedule daily JITAI WorkManager intervention
                            InterventionScheduler.scheduleDailyIntervention(context, updated.vulnerabilityHour)
                            currentScreen = AppScreen.DAILY_NEURO_LOOP
                        }
                    )
                }
            }

            AppScreen.DAILY_NEURO_LOOP -> {
                currentGoal?.let { goal ->
                    DailyNeuroLoopScreen(
                        goal = goal,
                        nervousState = nervousState,
                        autoOpenSos = autoOpenSos,
                        onSosConsumed = onSosConsumed,
                        onStateChange = { nervousState = it },
                        onGoalUpdated = { updatedGoal ->
                            currentGoal = updatedGoal
                            storage.saveGoal(updatedGoal)
                        },
                        onMissedDayTriggered = {
                            currentScreen = AppScreen.RECOVERY_LAB
                        },
                        onResetGoal = {
                            storage.clearGoal()
                            currentGoal = null
                            currentScreen = AppScreen.ONBOARDING_SHADOW
                        }
                    )
                }
            }

            AppScreen.RECOVERY_LAB -> {
                currentGoal?.let { goal ->
                    ZeroShameRecoveryScreen(
                        goal = goal,
                        onRecovered = { updatedGoal ->
                            currentGoal = updatedGoal
                            storage.saveGoal(updatedGoal)
                            nervousState = NervousSystemState.DORSAL_VAGAL
                            currentScreen = AppScreen.DAILY_NEURO_LOOP
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. PHASE 1: THE MIRROR ROOM (Psychoanalytic Onboarding)
// ============================================================================

@Composable
fun OnboardingShadowScreen(onAnalysisComplete: (PsychoanalyticGoal) -> Unit) {
    var rawGoal by remember { mutableStateOf("") }
    var escapeBehavior by remember { mutableStateOf("") }
    var coreFear by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val escapeSuggestions = listOf(
        "Over-researching & perfectionism",
        "Doomscrolling & social media",
        "Productive procrastination (cleaning/admin)",
        "Sudden fatigue & brain fog"
    )
    val fearSuggestions = listOf(
        "Discovering I lack real talent",
        "Being judged or criticized publicly",
        "Losing my current comfort & freedom",
        "Failing even after giving 100% effort"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "The Mirror Room (Shadow Audit)",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2536)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "⚠️ Neuroscience Warning: Positive fantasy about a goal triggers premature dopamine release in the ventral striatum, sedating your drive to act. Before wiring your goal, we must expose your ego's 'Competing Commitment' and secondary gain.",
                color = Color(0xFFB0BEC5),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        OutlinedTextField(
            value = rawGoal,
            onValueChange = { rawGoal = it },
            label = { Text("1. What is your conscious goal? (e.g., Practice 3D modeling 1 hr/day)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Text(
            text = "2. When it is time to execute, what is your go-to escape mechanism?",
            color = Color(0xFFE0E0E0),
            fontSize = 14.sp
        )

        escapeSuggestions.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { escapeBehavior = suggestion },
                        label = { Text(suggestion, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = escapeBehavior,
            onValueChange = { escapeBehavior = it },
            label = { Text("My Escape / Sabotage Behavior") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Text(
            text = "3. If you dropped all excuses tomorrow and gave this everything you had, what uncomfortable risk or truth would you be exposed to?",
            color = Color(0xFFE0E0E0),
            fontSize = 14.sp
        )

        fearSuggestions.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { coreFear = suggestion },
                        label = { Text(suggestion, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = coreFear,
            onValueChange = { coreFear = it },
            label = { Text("My Core Unconscious Fear") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (rawGoal.isNotBlank() && escapeBehavior.isNotBlank() && coreFear.isNotBlank()) {
                    isAnalyzing = true
                    scope.launch {
                        val analyzed = PsychoanalyticAiEngine.analyzeUnconscious(rawGoal, escapeBehavior, coreFear)
                        isAnalyzing = false
                        onAnalysisComplete(analyzed)
                    }
                }
            },
            enabled = !isAnalyzing && rawGoal.isNotBlank() && escapeBehavior.isNotBlank() && coreFear.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Mapping Unconscious Resistance...")
            } else {
                Text("Deconstruct Shadow & Wire Neural Loop", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// 7. PHASE 2: IMMUNITY MAP, ELASTIC CALIBRATION & 3-SEC HAPTIC CONTRACT
// ============================================================================

@Composable
fun InsightCardScreen(
    goal: PsychoanalyticGoal,
    onUpdateGoal: (PsychoanalyticGoal) -> Unit
) {
    var ceilingTask by remember { mutableStateOf(goal.ceilingTask) }
    var standardTask by remember { mutableStateOf(goal.standardTask) }
    var survivalTask by remember { mutableStateOf(goal.survivalTask) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val steps = 30
            for (i in 1..steps) {
                if (!isHolding) break
                delay(100)
                holdProgress = i / steps.toFloat()
                if (i % 5 == 0) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
            if (holdProgress >= 1f) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onUpdateGoal(
                    goal.copy(
                        ceilingTask = ceilingTask,
                        standardTask = standardTask,
                        survivalTask = survivalTask
                    )
                )
            }
        } else {
            holdProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Psychological Immunity Map & Elastic Calibration",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261C35)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF9575CD), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("👁️ Unconscious Secondary Gain (Kegan Model):", color = Color(0xFFD1C4E9), fontWeight = FontWeight.Bold)
                Text(goal.competingCommitment, color = Color.White, fontSize = 14.sp, lineHeight = 22.sp)
                HorizontalDivider(color = Color(0xFF4A3B63))
                Text("⚡ Basal Ganglia If-Then Wiring (WOOP):", color = Color(0xFF81D4FA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("\"IF ${goal.ifObstacle}, THEN ${goal.thenAntidote}.\"", color = Color(0xFFE1F5FE), fontSize = 13.sp)
            }
        }

        Text("Calibrate Your 3 Elastic Execution Tiers (Editable):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        OutlinedTextField(
            value = ceilingTask,
            onValueChange = { ceilingTask = it },
            label = { Text("🔥 Ceiling Tier (High Dopamine / Flow Days)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = standardTask,
            onValueChange = { standardTask = it },
            label = { Text("⚖️ Standard Tier (Regulated Baseline Days)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = survivalTask,
            onValueChange = { survivalTask = it },
            label = { Text("🛡️ Neural Survival Tier (<2 Mins - Anxious/Exhausted Days)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF283593))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            tryAwaitRelease()
                            isHolding = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(holdProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF7E57C2), Color(0xFF26A69A))
                        )
                    )
                    .align(Alignment.CenterStart)
            )
            Text(
                text = if (isHolding) "Encoding into Prefrontal Cortex... (${(holdProgress * 100).toInt()}%)"
                else "Hold for 3 Seconds: Acknowledge My Shadow Trap",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ============================================================================
// 8. PHASE 3: DAILY NEURO-LOOP + WORKMANAGER JITAI SCHEDULER CARD
// ============================================================================

@Composable
fun DailyNeuroLoopScreen(
    goal: PsychoanalyticGoal,
    nervousState: NervousSystemState,
    autoOpenSos: Boolean,
    onSosConsumed: () -> Unit,
    onStateChange: (NervousSystemState) -> Unit,
    onGoalUpdated: (PsychoanalyticGoal) -> Unit,
    onMissedDayTriggered: () -> Unit,
    onResetGoal: () -> Unit
) {
    val context = LocalContext.current
    var showSosModal by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(120) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var rewardMessage by remember { mutableStateOf<String?>(null) }
    var selectedHour by remember { mutableIntStateOf(goal.vulnerabilityHour) }

    // If user tapped "120s SOS Timer" directly from the notification, auto-launch the modal
    LaunchedEffect(autoOpenSos) {
        if (autoOpenSos) {
            countdownSeconds = 120
            isTimerRunning = true
            showSosModal = true
            onSosConsumed()
        }
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
        if (countdownSeconds == 0) {
            isTimerRunning = false
        }
    }

    val activeTaskText = when (nervousState) {
        NervousSystemState.HIGH_DRIVE -> goal.ceilingTask
        NervousSystemState.VENTRAL_VAGAL -> goal.standardTask
        NervousSystemState.SYMPATHETIC, NervousSystemState.DORSAL_VAGAL -> goal.survivalTask
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Neural Resilience Score & Synaptic Density
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("🛡️ Resilience: ${goal.resilienceScore}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("⚡ Synapses: ${goal.synapticDensity}", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            TextButton(onClick = onResetGoal) {
                Text("New Goal", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
        }

        // Section 1: Morning Autonomic Nervous System Check-in
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Autonomic Nervous System Check-In:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                NervousSystemState.entries.forEach { state ->
                    val isSelected = state == nervousState
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) state.accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) state.accentColor else Color(0x44FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures { onStateChange(state) }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(state.accentColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Text(
                    text = nervousState.subtitle,
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Section 2: Progressive Disclosure of Single Calibrated Next Action
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, nervousState.accentColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "🎯 Today's Biologically Calibrated Protocol:",
                    color = nervousState.accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = activeTaskText,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val newGoal = goal.copy(synapticDensity = goal.synapticDensity + 1)
                            onGoalUpdated(newGoal)
                            rewardMessage = PsychoanalyticAiEngine.generateDopamineReward(nervousState, goal.synapticDensity)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))
                    ) {
                        Text("Executed (+Synapse)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            countdownSeconds = 120
                            isTimerRunning = false
                            showSosModal = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFCA28))
                    ) {
                        Text("🚨 I Feel Resistance", fontSize = 13.sp)
                    }
                }
            }
        }

        // Variable RPE Dopamine Feedback Card
        rewardMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3B2B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF66BB6A), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(msg, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }

        // Section 3: WorkManager JITAI Smart Antidote Notification Scheduler
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF5C6BC0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔔 Smart Antidote Interceptor (WorkManager JITAI)",
                    color = Color(0xFF9FA8DA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Select your daily High-Risk Procrastination Hour. WorkManager will intercept your '${goal.escapeBehavior}' with an interactive If-Then notification:",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vulnerability Window: ${String.format("%02d:00", selectedHour)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                                val updated = goal.copy(vulnerabilityHour = selectedHour)
                                onGoalUpdated(updated)
                                InterventionScheduler.scheduleDailyIntervention(context, selectedHour)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("-1h", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedHour = (selectedHour + 1) % 24
                                val updated = goal.copy(vulnerabilityHour = selectedHour)
                                onGoalUpdated(updated)
                                InterventionScheduler.scheduleDailyIntervention(context, selectedHour)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+1h", color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = {
                        InterventionScheduler.triggerInstantTestIntervention(context)
                        Toast.makeText(
                            context,
                            "JITAI Scheduled! Minimize the app—notification arrives in 5 seconds.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                ) {
                    Text("⚡ Test Smart Intervention Notification (5s Delay)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Trigger for Zero-Shame Recovery Lab
        TextButton(
            onClick = onMissedDayTriggered,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Missed yesterday or broke momentum? Enter the Zero-Shame Recovery Lab",
                color = Color(0xFF90A4AE),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    // SOS Modal: Real-Time Ego Defense Interception + 120-Second Timer
    if (showSosModal) {
        AlertDialog(
            onDismissRequest = { showSosModal = false },
            containerColor = Color(0xFF1E1B2E),
            title = {
                Text(
                    text = "🛑 Ego Defense Mechanism Detected",
                    color = Color(0xFFFFCA28),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your Shadow Audit Reminder:\n\"${goal.competingCommitment}\"",
                        color = Color(0xFFD1C4E9),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    HorizontalDivider(color = Color(0xFF4A3B63))
                    Text(
                        text = "Your Pre-Wired Antidote:\n${goal.thenAntidote}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF311B92))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Limbic Friction Breaker: ${countdownSeconds / 60}:${String.format("%02d", countdownSeconds % 60)}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isTimerRunning = !isTimerRunning },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                ) {
                    Text(if (isTimerRunning) "Pause Timer" else "Start 120s Imperfect Action")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosModal = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

// ============================================================================
// 9. PHASE 4: ZERO-SHAME RECOVERY LAB (Counteracting the Abstinence Violation Effect)
// ============================================================================

@Composable
fun ZeroShameRecoveryScreen(
    goal: PsychoanalyticGoal,
    onRecovered: (PsychoanalyticGoal) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    val reasons = listOf(
        "The task tier was too large for my available nervous system energy",
        "My unconscious fear & perfectionism silently took over",
        "An external life disruption or unpredictable stressor occurred"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "🧪 Zero-Shame Recovery Lab",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF80CBC4)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2833)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Traditional apps punish missed days by resetting your 'Streak' to zero—triggering the Abstinence Violation Effect (AVE), activating amygdala shame, and shutting down your prefrontal cortex. In neuroscience, a lapse is merely laboratory data. Returning today proves your neuroplasticity is intact.",
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Text(
            text = "Which variable triggered the resistance over the past 24-48 hours?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        reasons.forEach { reason ->
            val isSelected = selectedReason == reason
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF2E4053) else Color(0xFF17202A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) Color(0xFF80CBC4) else Color.Gray,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { selectedReason = reason }
                    }
            ) {
                Text(
                    text = reason,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Button(
            onClick = {
                val updated = goal.copy(
                    resilienceScore = goal.resilienceScore + 15
                )
                onRecovered(updated)
            },
            enabled = selectedReason.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Log Lab Data & Claim +15 Neural Resilience", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
