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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// 1. BILINGUAL CLINICAL ARCHITECTURE & PSYCHOANALYTIC ARCHETYPES
// ============================================================================

enum class AppLang { FA, EN }

enum class BottomTab(val titleFa: String, val titleEn: String, val icon: String) {
    SCANNER("اسکنر سایه", "Shadow Scan", "👁️"),
    TRIANGLE("نقشه روان", "Psyche Map", "🔺"),
    NEURO_LOOP("حلقه عصبی", "Neuro-Loop", "⚡"),
    SOMATIC_90S("تخلیه ۹۰ ثانیه", "90s Reset", "🫁")
}

// Step 1: Conscious Goal Archetypes (Tap-only)
enum class GoalArchetype(
    val id: Int,
    val titleFa: String, val descFa: String,
    val titleEn: String, val descEn: String,
    val icon: String,
    val ceilingFa: String, val standardFa: String, val survivalFa: String,
    val ceilingEn: String, val standardEn: String, val survivalEn: String
) {
    CREATIVE_MASTERY(
        0,
        "خلق اثر، طراحی و مهارت تخصصی", "طراحی سه‌بعدی، برنامه‌نویسی، هنر یا ساخت محصول شخصی",
        "Creative & Technical Mastery", "3D design, coding, art, or building a signature project",
        "💎",
        "۹۰ دقیقه خلق و طراحی عمیق در حالت پرواز (بدون چک کردن رفرنس‌ها)",
        "۳۵ دقیقه اجرای متمرکز روی پروژه اصلی بدون ویرایش وسواسی",
        "نسخه بقای ۲ دقیقه‌ای: فقط باز کردن نرم‌افزار/فایل پروژه و کشیدن ۱ خط یا تغییر کوچک",
        "90 mins deep creation in Airplane Mode (Zero reference checking)",
        "35 mins focused execution on core project without obsessive editing",
        "2-Min Survival: Just open the software/project file and make 1 tiny flawed stroke"
    ),
    VISIBILITY_BRAND(
        1,
        "دیده شدن، تولید محتوا و برند شخصی", "انتشار کارها، یوتیوب/اینستاگرام، ارائه خود به بازار",
        "Visibility, Content & Personal Brand", "Publishing work, YouTube/Socials, presenting to the market",
        "🎬",
        "ضبط، تدوین یا آماده‌سازی کامل ۱ محتوای اصلی به مدت ۷۵ دقیقه",
        "۳۰ دقیقه کار روی سناریو، فیلم‌برداری یا آماده‌سازی پست بدون کمال‌گرایی",
        "نسخه بقای ۲ دقیقه‌ای: نوشتن فقط ۳ جمله خام یا گرفتن ۳۰ ثانیه تصویر آزمایشی",
        "75 mins full recording, editing, or shipping 1 flagship piece",
        "30 mins working on script, filming, or drafting without perfectionism",
        "2-Min Survival: Write just 3 raw lines or shoot a 30-second test clip"
    ),
    CAREER_WEALTH(
        2,
        "جهش شغلی، درآمدزایی و استقلال مالی", "راه انداختن کسب‌وکار، مذاکره، فروش یا ارتقای جایگاه مالی",
        "Career Leap & Financial Autonomy", "Launching a venture, high-stakes applications, sales, or wealth building",
        "🚀",
        "۶۰ دقیقه اقدام مستقیم درآمدزا (ارسال پیشنهاد، تماس، یا توسعه محصول)",
        "۳۰ دقیقه پیگیری متمرکزِ یک قدم مالی یا شغلیِ مشخص",
        "نسخه بقای ۲ دقیقه‌ای: باز کردن فایل رزومه/مالی و انجام ۱ اقدام ۶۰ ثانیه‌ای",
        "60 mins direct revenue-generating action (pitching, building, closing)",
        "30 mins focused execution on one concrete career/financial milestone",
        "2-Min Survival: Open your tracker/document and take 1 sixty-second action"
    ),
    MIND_BODY_VITALITY(
        3,
        "تناسب اندام، مطالعه عمیق و نظم زیستی", "ورزش منظم، مطالعه تخصصی، مدیتیشن یا تغییر سبک زندگی",
        "Vitality, Deep Study & Bio-Rhythm", "Consistent training, academic mastery, or lifestyle restructuring",
        "🧬",
        "۶۰ دقیقه تمرین کامل یا مطالعه عمیق همراه با یادداشت‌برداری فعال",
        "۳۰ دقیقه حرکت بدنی یا مطالعه متمرکز بدون گوشی",
        "نسخه بقای ۲ دقیقه‌ای: ۵ حرکت کششی یا خواندنِ فقط ۱ پاراگراف از کتاب",
        "60 mins full training session or deep active-recall study block",
        "30 mins focused physical movement or distraction-free study",
        "2-Min Survival: Do 5 stretches or read just 1 single paragraph"
    )
}

// Step 2: Ego Defense Mechanisms (Triangle of Conflict - Top Right Vertex)
enum class DefenseMechanism(
    val id: Int,
    val titleFa: String, val detailFa: String,
    val titleEn: String, val detailEn: String,
    val clinicalNameFa: String, val clinicalNameEn: String,
    val icon: String
) {
    PERFECTIONIST_PARALYSIS(
        0,
        "تله کمال‌گرایی و تحقیقِ بی‌پایان", "مدام حس می‌کنم هنوز آماده نیستم و باید بیشتر آموزش ببینم یا ابزارم کامل‌تر شود.",
        "Perfectionist Prep & Endless Research", "I feel I'm not ready yet and must watch more tutorials or refine the plan first.",
        "عقلانی‌سازی و کمال‌گرایی دفاعی", "Intellectualization & Defensive Perfectionism",
        "🔍"
    ),
    DOPAMINE_ANESTHESIA(
        1,
        "بی‌حسی دوپامینی (اسکرول و گوشی)", "ناخودآگاه قفل گوشی را باز می‌کنم و غرق در شبکه‌های اجتماعی، خبرها یا ویدیوها می‌شوم.",
        "Dopamine Anesthesia (Scrolling)", "I reflexively unlock my phone and numb out with reels, feeds, or stimulation.",
        "سرکوب حسی و اجتناب دوپامینی", "Affective Numbing & Dopamine Avoidance",
        "📱"
    ),
    DORSAL_SHUTDOWN(
        2,
        "خستگی ناگهانی، خواب‌آلودگی و کرختی", "به محض اینکه می‌خواهم شروع کنم، انرژی بدنم خالی می‌شود و مغزم قفل می‌کند.",
        "Sudden Fatigue, Brain Fog & Heaviness", "The moment I sit to start, my energy crashes and my mind goes foggy.",
        "خاموشی دورسال واگال (سوماتیزاسیون)", "Dorsal Vagal Shutdown (Somatization)",
        "🌫️"
    ),
    RESCUER_BUSYWORK(
        3,
        "پناه بردن به کارهای فرعی یا کمک به دیگران", "به جای کار اصلی، اتاق را مرتب می‌کنم، کارهای ریزِ اداری را انجام می‌دهم یا دغدغه دیگران را حل می‌کنم.",
        "Productive Procrastination & Rescuing", "I clean, organize, do minor chores, or solve other people's problems instead.",
        "جابه‌جایی (Displacement) و نقش ناجی", "Displacement & Karpman Rescuer Role",
        "🧹"
    )
}

// Step 3: Somatic Anxiety Channel (ISTDP Triangle of Conflict - Top Left Vertex)
enum class SomaticChannel(
    val id: Int,
    val titleFa: String, val descFa: String,
    val titleEn: String, val descEn: String,
    val istpdPathwayFa: String, val istpdPathwayEn: String,
    val icon: String
) {
    STRIATED_CHEST_THROAT(
        0,
        "قفسه سینه، گلو و فک (عضلات مخطط)", "سنگینی نفس، تپش قلب، قفل شدن فک، یا حس بغض و فشار در گلو و شانه‌ها.",
        "Chest, Throat & Jaw (Striated Muscle)", "Shallow breath, tight chest, clenched jaw, or tension in shoulders and throat.",
        "کانال عضلات مخطط (آماده برای بازگشایی هیجانی)", "Striated Muscle Pathway (High readiness for affect processing)",
        "🫀"
    ),
    SMOOTH_GUT_STOMACH(
        1,
        "معده، شکم و دلشوره (عضلات صاف)", "پیچش معده، تهوع خفیف، سرد شدن دست‌وپا، یا حس خالی شدن زیر دل.",
        "Stomach & Solar Plexus (Smooth Muscle)", "Butterflies, gut knot, cold hands, or visceral sinking in the stomach.",
        "کانال عضلات صاف و خودمختار (نیازمند تنظیم واگ قبل از فشار)", "Smooth Muscle Autonomic Pathway (Requires vagal grounding)",
        "🦋"
    ),
    COGNITIVE_PERCEPTUAL(
        2,
        "سرگیجه، مه مغزی و پراکندگی ذهن", "حس گیجی، پریدن افکار از این شاخه به آن شاخه، سنگینی پلک‌ها یا جدا شدن از واقعیت.",
        "Prefrontal Brain Fog & Dissociation", "Racing scattered thoughts, heavy eyelids, dizziness, or spacing out.",
        "کانال اختلال شناختی-ادراکی (نیازمند لنگر انداختن حسی-فیزیکی)", "Cognitive-Perceptual Pathway (Requires somatic anchoring)",
        "🌀"
    )
}

// Step 4: Unconscious Core Fear & Secondary Gain (Triangle of Conflict - Bottom Hidden Vertex)
enum class CoreFearArchetype(
    val id: Int,
    val titleFa: String, val descFa: String,
    val titleEn: String, val descEn: String,
    val hiddenImpulseFa: String, val hiddenImpulseEn: String,
    val secondaryGainFa: String, val secondaryGainEn: String,
    val icon: String
) {
    SHATTERED_GENIUS_ILLUSION(
        0,
        "ترس از کشفِ «معمولی بودن»", "اگر ۱۰۰٪ تلاشم را بکنم و کارم عالی نشود، دیگر بهانه‌ای ندارم و معلوم می‌شود استعدادی ندارم.",
        "Shattering the 'Potential Genius' Illusion", "If I give 100% effort and still fall short, I lose my excuse and prove I'm mediocre.",
        "میل شدید به تمایز و خشم از قضاوت شدن", "Fierce drive for excellence & rage at potential humiliation",
        "سود ثانویه: تا زمانی که کار را به تعویق بیندازی، ایگوی تو در یک پناهگاه امن می‌ماند و به خودش می‌گوید: «من فوق‌العاده‌ام، فقط هنوز وقت نگذاشته‌ام!» تعلل تو در واقع محافظِ عزت‌نفس شکننده‌ات است.",
        "Secondary Gain: By delaying execution, your ego protects the comforting fantasy: 'I could build a masterpiece if I really tried.' Procrastination is acting as a bodyguard for your self-worth.",
        "🎭"
    ),
    EXPOSURE_AND_JUDGMENT(
        1,
        "ترس از دیده شدنِ نقص‌ها و قضاوت دیگران", "وقتی کارم تمام و منتشر شود، دیگران (یا رقبایم) می‌توانند عیب‌هایم را ببینند و مسخره یا نقدم کنند.",
        "Fear of Public Scrutiny & Imperfection", "Once I finish and show my work, others can see my flaws, criticize, or reject me.",
        "میل به اصالت و خشم سرکوب‌شده نسبت به منتقدان گذشته", "Desire for authentic expression & repressed anger at past critics",
        "سود ثانویه: نامرئی ماندن مساوی است با امنیت! ناخودآگاهت ترجیح می‌دهد در سایه بماند و پیشرفت نکند، اما در عوض هیچ تیری از سمت قضاوت دیگران به سمتت شلیک نشود.",
        "Secondary Gain: Invisibility equals safety. Your unconscious prefers keeping you stuck in the shadows rather than risking the sting of external judgment.",
        "🛡️"
    ),
    SUPEREGO_REBELLION(
        2,
        "لجبازی ناخودآگاه با «اجبار و بایدها»", "حس می‌کنم اگر طبق برنامه کار کنم، آزادی‌ام را از دست داده‌ام و دارم به یک رئیس سخت‌گیر درونی باج می‌دهم.",
        "Unconscious Rebellion Against Inner Authority", "Following a strict schedule feels like suffocating my freedom and obeying a harsh boss.",
        "تکانه خودمختاری و خشم انتقال‌یافته به والد کنترل‌گر درونی", "Drive for autonomy & transference rage against a controlling Superego",
        "سود ثانویه: تو هدفِ خودت را به چشم یک «والد زورگو» (Superego) می‌بینی! بنابراین با تعلل کردن و انجام ندادن کار، ناخودآگاهت حسِ شیرینِ «استقلال، سرکشی و آزادی» را تجربه می‌کند.",
        "Secondary Gain: Your unconscious has projected a 'controlling parent' onto your own goal! By procrastinating, your ego secretly enjoys a triumph of rebellion and autonomy.",
        "⛓️"
    ),
    BURDEN_OF_SUCCESS(
        3,
        "ترس از سنگینیِ مسئولیت بعد از موفقیت", "اگر واقعاً در این کار موفق شوم، توقع همه از من بالا می‌رود و دیگر حق استراحت یا اشتباه ندارم.",
        "Fear of High Expectations After Success", "If I actually succeed, the pressure will double and I'll never be allowed to rest.",
        "نیاز به امنیت عاطفی و سوگِ از دست رفتنِ کودکیِ بی‌مسئولیت", "Need for unconditional safety & grief over losing carefree rest",
        "سود ثانویه: کوچک ماندن یعنی کسی از تو انتظارِ بارِ سنگین ندارد. ناخودآگاهت ترمز را می‌کشد تا تو را از فرسودگیِ ناشی از «موفقیت بدون مرز» نجات دهد.",
        "Secondary Gain: Staying small protects you from crushing expectations. Your unconscious pulls the handbrake to save you from the exhaustion of unbounded responsibility.",
        "🏔️"
    )
}

enum class NervousSystemState(
    val titleFa: String, val subtitleFa: String,
    val titleEn: String, val subtitleEn: String,
    val bgColor: Color, val accentColor: Color
) {
    SYMPATHETIC(
        "سمپاتیک (مضطرب / پرتنش)", "آمیگدال برانگیخته است. برای دور زدن مقاومت، فقط «نسخه بقای ۲ دقیقه‌ای» را انجام بده.",
        "Sympathetic (Anxious / Overwhelmed)", "Amygdala arousal is high. Execute only the 2-Minute Survival Tier to bypass resistance.",
        Color(0xFF132226), Color(0xFF4FC3F7)
    ),
    VENTRAL_VAGAL(
        "ونترو واگال (متعادل و متمرکز)", "قشر پیش‌پیشانی آنلاین و امن است. زمان طلایی برای اجرای «نسخه استاندارد».",
        "Ventral Vagal (Grounded & Focused)", "Prefrontal cortex is online and regulated. Optimal window for your Standard Tier.",
        Color(0xFF0E131F), Color(0xFF9575CD)
    ),
    DORSAL_VAGAL(
        "دورسال واگال (خسته، کرخت و بی‌انگیزه)", "سیستم عصبی در حالت ذخیره انرژی است. امروز فقط مسیر سیناپسی را با نسخه ۲ دقیقه‌ای زنده نگه دار.",
        "Dorsal Vagal (Exhausted / Numb)", "System is conserving energy. Today is strictly about keeping the synapse alive (<2 mins).",
        Color(0xFF1A1622), Color(0xFF81C784)
    ),
    HIGH_DRIVE(
        "شارژ دوپامینی بالا (حالت غوطه‌وری / Flow)", "آمادگی عصبی در اوج است. امروز به سراغ «نسخه سقف» می‌رویم.",
        "High Dopaminergic Drive (Flow State)", "Peak autonomic readiness. Deploying your Ceiling Tier for maximum neuroplasticity.",
        Color(0xFF160D29), Color(0xFFFFB300)
    )
}

data class PsychoanalyticProfile(
    val goalArchetypeId: Int = 0,
    val defenseId: Int = 0,
    val somaticId: Int = 0,
    val coreFearId: Int = 0,
    val customGoalNote: String = "",
    val aiDeepSynthesisFa: String = "",
    val aiDeepSynthesisEn: String = "",
    val contractSigned: Boolean = false,
    val resilienceScore: Int = 100,
    val synapticDensity: Int = 0,
    val vulnerabilityHour: Int = 16
) {
    val goal: GoalArchetype get() = GoalArchetype.entries.getOrElse(goalArchetypeId) { GoalArchetype.CREATIVE_MASTERY }
    val defense: DefenseMechanism get() = DefenseMechanism.entries.getOrElse(defenseId) { DefenseMechanism.PERFECTIONIST_PARALYSIS }
    val somatic: SomaticChannel get() = SomaticChannel.entries.getOrElse(somaticId) { SomaticChannel.STRIATED_CHEST_THROAT }
    val fear: CoreFearArchetype get() = CoreFearArchetype.entries.getOrElse(coreFearId) { CoreFearArchetype.SHATTERED_GENIUS_ILLUSION }

    fun antidoteFa(): String =
        "وقتی در بدنم «${somatic.titleFa}» را حس کردم و ایگو خواست با «${defense.titleFa}» فرار کند، بلافاصله تایمر ۱۲۰ ثانیه‌ای را می‌زنم و «${goal.survivalFa}» را به ناقص‌ترین شکل ممکن انجام می‌دهم."

    fun antidoteEn(): String =
        "When I notice '${somatic.titleEn}' and my ego reaches for '${defense.titleEn}', I will immediately trigger the 120s timer and execute '${goal.survivalEn}' as imperfectly as possible."
}

// ============================================================================
// 2. HYBRID CLINICAL AI ENGINE (Built-in Matrix + Universal Cloud Gateway)
// ============================================================================

object HybridPsychoanalyticEngine {
    // Universal OpenAI-compatible endpoint (Supports OpenRouter.ai, AvalAI.ir, Groq, or custom proxy)
    var customApiEndpoint = "https://openrouter.ai/api/v1/chat/completions"
    var customApiKey = "" // Optional: Works 100% offline via clinical matrix if empty
    var customModelName = "google/gemini-2.0-flash-001"

    suspend fun synthesizeProfile(
        g: GoalArchetype,
        d: DefenseMechanism,
        s: SomaticChannel,
        f: CoreFearArchetype
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        // 1. Deterministic Harvard ISTDP & Kegan Clinical Matrix (Instant, Zero-Failure)
        val matrixFa = buildString {
            append("🔬 تشخیص بالینی (مدل مالان و کیگن):\n")
            append("در ناخودآگاه تو، میل به «${g.titleFa}» باعث بیدار شدن «${f.hiddenImpulseFa}» می‌شود. ")
            append("از آنجا که تجربه این تکانه برای ایگو با خطرِ «${f.titleFa}» گره خورده است، اضطراب از طریق «${s.istpdPathwayFa}» در بدنت تخلیه می‌شود و ایگو بلافاصله مکانیسم دفاعی «${d.clinicalNameFa}» را فعال می‌کند.\n\n")
            append("💡 ${f.secondaryGainFa}")
        }

        val matrixEn = buildString {
            append("🔬 Clinical Formulation (Malan ISTDP & Kegan Model):\n")
            append("In your unconscious, pursuing '${g.titleEn}' activates '${f.hiddenImpulseEn}'. ")
            append("Because your ego associates this impulse with '${f.titleEn}', anxiety discharges via your '${s.istpdPathwayEn}', forcing your ego to deploy '${d.clinicalNameEn}'.\n\n")
            append("💡 ${f.secondaryGainEn}")
        }

        // 2. Optional Cloud AI Enhancement (OpenRouter / AvalAI / Groq / OpenAI)
        if (customApiKey.isNotBlank()) {
            try {
                val url = URL(customApiEndpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $customApiKey")
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                }

                val prompt = """
                    You are a Harvard Psychoanalyst (ISTDP & Immunity to Change).
                    User selections:
                    - Goal: ${g.titleEn}
                    - Defense: ${d.clinicalNameEn}
                    - Somatic Anxiety: ${s.titleEn}
                    - Core Fear: ${f.titleEn}
                    Write a 3-sentence piercing psychoanalytic formulation in Persian (under key "fa") and English (under key "en") as valid JSON: {"fa":"...", "en":"..."}
                """.trimIndent()

                val payload = JSONObject().apply {
                    put("model", customModelName)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val responseText = conn.inputStream.bufferedReader().readText()
                val content = JSONObject(responseText)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val clean = content.substringAfter("{").substringBeforeLast("}")
                val parsed = JSONObject("{$clean}")
                return@withContext Pair(
                    parsed.optString("fa", matrixFa),
                    parsed.optString("en", matrixEn)
                )
            } catch (_: Exception) {
                // Gracefully fall back to the built-in Harvard Clinical Matrix
            }
        }

        delay(900) // Brief clinical calculation feel
        Pair(matrixFa, matrixEn)
    }

    fun generateRpeReward(lang: AppLang, state: NervousSystemState, synapses: Int): String {
        val roll = Random.nextFloat()
        return if (lang == AppLang.FA) {
            when {
                roll < 0.35f -> "⚡ جهش دوپامینی (پاداش غیرمنتظره RPE): تو دقیقاً در لحظه‌ای که مدار پیش‌فرض مغز (DMN) می‌خواست فرار کند، مقاومت ایگو را شکستی! ضخامت غلاف میلین این مسیر عصبی افزایش یافت."
                state == NervousSystemState.SYMPATHETIC || state == NervousSystemState.DORSAL_VAGAL ->
                    "🧠 پیروزی بر اصطکاک لیمبیک: اجرای نسخه بقای ۲ دقیقه‌ای در وضعیت خستگی/اضطراب، عقده‌های قاعده‌ای مغزت را بسیار عمیق‌تر از ۲ ساعت کار در روزهای عادی سیم‌کشی کرد."
                else -> "🌱 تثبیت هویت جدید: سیناپس شماره ${synapses + 1} با موفقیت در قشر پیش‌پیشانی ثبت شد. تو در حال عبور از «هویت منتظر» به «هویت عمل‌کننده» هستی."
            }
        } else {
            when {
                roll < 0.35f -> "⚡ Dopamine RPE Spike (Unexpected Insight): You overrode your Default Mode Network (DMN) at peak resistance. Myelin sheathing on this neural pathway just thickened."
                state == NervousSystemState.SYMPATHETIC || state == NervousSystemState.DORSAL_VAGAL ->
                    "🧠 Limbic Friction Victory: Executing a 2-minute micro-habit while dysregulated rewires your Basal Ganglia far more powerfully than 2 hours on an easy day."
                else -> "🌱 Identity Consolidation: Synaptic repetition #${synapses + 1} encoded in the prefrontal cortex. You are shifting from wishing to executing."
            }
        }
    }
}

// ============================================================================
// 3. PERSISTENCE & JITAI WORKMANAGER NOTIFICATIONS
// ============================================================================

class NeuroStorage(context: Context) {
    private val prefs = context.getSharedPreferences("neuro_shadow_v2_prefs", Context.MODE_PRIVATE)

    fun saveLang(lang: AppLang) = prefs.edit().putString("lang", lang.name).apply()
    fun loadLang(): AppLang =
        if (prefs.getString("lang", "FA") == "EN") AppLang.EN else AppLang.FA

    fun saveProfile(p: PsychoanalyticProfile) {
        prefs.edit().apply {
            putBoolean("hasProfile", true)
            putInt("goalArchetypeId", p.goalArchetypeId)
            putInt("defenseId", p.defenseId)
            putInt("somaticId", p.somaticId)
            putInt("coreFearId", p.coreFearId)
            putString("aiDeepSynthesisFa", p.aiDeepSynthesisFa)
            putString("aiDeepSynthesisEn", p.aiDeepSynthesisEn)
            putBoolean("contractSigned", p.contractSigned)
            putInt("resilienceScore", p.resilienceScore)
            putInt("synapticDensity", p.synapticDensity)
            putInt("vulnerabilityHour", p.vulnerabilityHour)
            apply()
        }
    }

    fun loadProfile(): PsychoanalyticProfile? {
        if (!prefs.getBoolean("hasProfile", false)) return null
        return PsychoanalyticProfile(
            goalArchetypeId = prefs.getInt("goalArchetypeId", 0),
            defenseId = prefs.getInt("defenseId", 0),
            somaticId = prefs.getInt("somaticId", 0),
            coreFearId = prefs.getInt("coreFearId", 0),
            aiDeepSynthesisFa = prefs.getString("aiDeepSynthesisFa", "") ?: "",
            aiDeepSynthesisEn = prefs.getString("aiDeepSynthesisEn", "") ?: "",
            contractSigned = prefs.getBoolean("contractSigned", false),
            resilienceScore = prefs.getInt("resilienceScore", 100),
            synapticDensity = prefs.getInt("synapticDensity", 0),
            vulnerabilityHour = prefs.getInt("vulnerabilityHour", 16)
        )
    }
}

object NeuroNotificationHelper {
    const val CHANNEL_ID = "neuro_antidote_jitai_v2"
    const val NOTIFICATION_ID = 4042
    const val ACTION_QUICK_SYNAPSE = "com.harvard.neuroshadow.ACTION_QUICK_SYNAPSE"
    const val EXTRA_LAUNCH_SOS = "EXTRA_LAUNCH_SOS"

    fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NeuroShadow Antidote Interventions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers bilingual psychoanalytic If-Then antidotes."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAntidoteIntervention(context: Context, profile: PsychoanalyticProfile, lang: AppLang) {
        ensureChannelExists(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val sosIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAUNCH_SOS, true)
        }
        val sosPending = PendingIntent.getActivity(
            context, 201, sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val quickIntent = Intent(context, AntidoteActionReceiver::class.java).apply {
            action = ACTION_QUICK_SYNAPSE
        }
        val quickPending = PendingIntent.getBroadcast(
            context, 202, quickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isFa = lang == AppLang.FA
        val title = if (isFa) "🛑 ردیابی مکانیسم دفاعی: ${profile.defense.titleFa}"
        else "🛑 Ego Defense Intercept: ${profile.defense.titleEn}"

        val body = if (isFa) profile.antidoteFa() else profile.antidoteEn()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(sosPending)
            .addAction(
                android.R.drawable.ic_media_play,
                if (isFa) "⏱️ تایمر ۱۲۰ ثانیه" else "⏱️ 120s SOS Timer",
                sosPending
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                if (isFa) "⚡ انجام نسخه بقا (+۱)" else "⚡ Did 2-Min Survival (+1)",
                quickPending
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}

class AntidoteNotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val storage = NeuroStorage(applicationContext)
        val profile = storage.loadProfile() ?: return Result.success()
        NeuroNotificationHelper.showAntidoteIntervention(applicationContext, profile, storage.loadLang())
        return Result.success()
    }
}

class AntidoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == NeuroNotificationHelper.ACTION_QUICK_SYNAPSE) {
            val storage = NeuroStorage(context)
            val profile = storage.loadProfile() ?: return
            val isFa = storage.loadLang() == AppLang.FA
            storage.saveProfile(
                profile.copy(
                    synapticDensity = profile.synapticDensity + 1,
                    resilienceScore = profile.resilienceScore + 5
                )
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reward = NotificationCompat.Builder(context, NeuroNotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(if (isFa) "⚡ ۱+ سیناپس و ۵+ تاب‌آوری ثبت شد!" else "⚡ +1 Synapse & +5 Resilience Encoded!")
                .setContentText(
                    if (isFa) "مقاومت لیمبیک را مستقیم از روی قفل صفحه شکستی!"
                    else "You bypassed limbic friction directly from the lock screen."
                )
                .setAutoCancel(true)
                .build()
            manager.notify(NeuroNotificationHelper.NOTIFICATION_ID, reward)
        }
    }
}

object InterventionScheduler {
    fun scheduleDaily(context: Context, hour24: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delayMs = target.timeInMillis - now.timeInMillis
        val req = PeriodicWorkRequestBuilder<AntidoteNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "neuro_daily_jitai_v2",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun triggerFiveSecondTest(context: Context) {
        val req = OneTimeWorkRequestBuilder<AntidoteNotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "neuro_test_jitai_v2",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
}

// ============================================================================
// 4. MAIN ACTIVITY & BILINGUAL LUXURY SHELL
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroNotificationHelper.ensureChannelExists(this)
        setContent {
            NeuroShadowProRoot()
        }
    }
}

@Composable
fun NeuroShadowProRoot() {
    val context = LocalContext.current
    val storage = remember { NeuroStorage(context) }

    var lang by remember { mutableStateOf(storage.loadLang()) }
    var profile by remember { mutableStateOf(storage.loadProfile()) }
    var activeTab by remember {
        mutableStateOf(if (profile == null) BottomTab.SCANNER else BottomTab.TRIANGLE)
    }
    var nervousState by remember { mutableStateOf(NervousSystemState.VENTRAL_VAGAL) }
    var showAiDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
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

    val isFa = lang == AppLang.FA
    val layoutDir = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    val bgAnimated by animateColorAsState(
        targetValue = when (activeTab) {
            BottomTab.NEURO_LOOP -> nervousState.bgColor
            BottomTab.SOMATIC_90S -> Color(0xFF0A191E)
            else -> Color(0xFF0B0F19)
        },
        animationSpec = tween(700),
        label = "bg"
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF9575CD),
                secondary = Color(0xFF4DD0E1),
                background = Color(0xFF0B0F19),
                surface = Color(0xFF151C2C)
            )
        ) {
            Scaffold(
                containerColor = bgAnimated,
                topBar = {
                    TopGlassBar(
                        lang = lang,
                        profile = profile,
                        onToggleLang = {
                            val next = if (lang == AppLang.FA) AppLang.EN else AppLang.FA
                            lang = next
                            storage.saveLang(next)
                        },
                        onOpenAiConfig = { showAiDialog = true }
                    )
                },
                bottomBar = {
                    BottomGlassNav(
                        lang = lang,
                        activeTab = activeTab,
                        hasProfile = profile != null,
                        onSelectTab = { activeTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (activeTab) {
                        BottomTab.SCANNER -> {
                            ProjectiveScannerWizard(
                                lang = lang,
                                existingProfile = profile,
                                onProfileCreated = { newProfile ->
                                    profile = newProfile
                                    storage.saveProfile(newProfile)
                                    InterventionScheduler.scheduleDaily(context, newProfile.vulnerabilityHour)
                                    activeTab = BottomTab.TRIANGLE
                                }
                            )
                        }

                        BottomTab.TRIANGLE -> {
                            val p = profile
                            if (p == null) {
                                EmptyProfilePrompt(lang) { activeTab = BottomTab.SCANNER }
                            } else {
                                PsycheMapTriangleScreen(
                                    lang = lang,
                                    profile = p,
                                    onUpdateProfile = { updated ->
                                        profile = updated
                                        storage.saveProfile(updated)
                                    },
                                    onGoToDailyLoop = { activeTab = BottomTab.NEURO_LOOP }
                                )
                            }
                        }

                        BottomTab.NEURO_LOOP -> {
                            val p = profile
                            if (p == null) {
                                EmptyProfilePrompt(lang) { activeTab = BottomTab.SCANNER }
                            } else {
                                DailyNeuroLoopV2Screen(
                                    lang = lang,
                                    profile = p,
                                    nervousState = nervousState,
                                    onStateChange = { nervousState = it },
                                    onUpdateProfile = { updated ->
                                        profile = updated
                                        storage.saveProfile(updated)
                                    },
                                    onGoToSomatic90s = { activeTab = BottomTab.SOMATIC_90S }
                                )
                            }
                        }

                        BottomTab.SOMATIC_90S -> {
                            SomaticNinetySecResetScreen(
                                lang = lang,
                                profile = profile,
                                onCompletedReset = {
                                    profile?.let { cur ->
                                        val updated = cur.copy(resilienceScore = cur.resilienceScore + 10)
                                        profile = updated
                                        storage.saveProfile(updated)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showAiDialog) {
                AiCloudGatewayDialog(
                    lang = lang,
                    onDismiss = { showAiDialog = false }
                )
            }
        }
    }
}

@Composable
fun TopGlassBar(
    lang: AppLang,
    profile: PsychoanalyticProfile?,
    onToggleLang: () -> Unit,
    onOpenAiConfig: () -> Unit
) {
    val isFa = lang == AppLang.FA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827).copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFF26A69A)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Ψ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Column {
                Text(
                    text = if (isFa) "نوروشَدو پرو" else "NeuroShadow Pro",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (profile != null) {
                        if (isFa) "🛡️ تاب‌آوری: ${profile.resilienceScore}  |  ⚡ سیناپس: ${profile.synapticDensity}"
                        else "🛡️ Resilience: ${profile.resilienceScore}  |  ⚡ Synapses: ${profile.synapticDensity}"
                    } else {
                        if (isFa) "آزمایشگاه روانکاوی و علوم اعصاب هاروارد" else "Harvard Psychoanalytic & Neural Lab"
                    },
                    color = Color(0xFF80CBC4),
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // AI Gateway Config Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F2937))
                    .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(10.dp))
                    .clickable { onOpenAiConfig() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("🤖 AI", color = Color(0xFFE5E7EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Live Bilingual FA | EN Switcher
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF5E35B1), Color(0xFF00897B))))
                    .clickable { onToggleLang() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isFa) "EN | فارسی" else "FA | English",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomGlassNav(
    lang: AppLang,
    activeTab: BottomTab,
    hasProfile: Boolean,
    onSelectTab: (BottomTab) -> Unit
) {
    val isFa = lang == AppLang.FA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF312E81) else Color.Transparent)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(tab.icon, fontSize = 18.sp)
                Text(
                    text = if (isFa) tab.titleFa else tab.titleEn,
                    color = if (selected) Color.White else Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ============================================================================
// 5. TAB 1: TAP-ONLY PROJECTIVE UNCONSCIOUS SCANNER (Bypasses Ego Typing)
// ============================================================================

@Composable
fun ProjectiveScannerWizard(
    lang: AppLang,
    existingProfile: PsychoanalyticProfile?,
    onProfileCreated: (PsychoanalyticProfile) -> Unit
) {
    val isFa = lang == AppLang.FA
    var step by remember { mutableIntStateOf(0) } // 0..3
    var selectedGoal by remember { mutableIntStateOf(existingProfile?.goalArchetypeId ?: 0) }
    var selectedDefense by remember { mutableIntStateOf(existingProfile?.defenseId ?: 0) }
    var selectedSomatic by remember { mutableIntStateOf(existingProfile?.somaticId ?: 0) }
    var selectedFear by remember { mutableIntStateOf(existingProfile?.coreFearId ?: 0) }
    var isSynthesizing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step Progress Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= step) Color(0xFF4DD0E1) else Color(0xFF1F2937))
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF172033)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.dp, Color(0xFF374151), RoundedCornerShape(14.dp))
        ) {
            Text(
                text = if (isFa)
                    "🧠 چرا در این اسکنر تایپ نمی‌کنید؟ در روانکاوی، تایپ کردن قشر منطقی مغز را فعال کرده و به «ایگو» فرصت سانسور و بهانه‌تراشی می‌دهد. با انتخاب سریع از بین کهن‌الگوهای زیر، مقاومت ناخودآگاهتان را دور می‌زنیم."
                else
                    "🧠 Why Zero Typing? Typing activates left-prefrontal rationalization and ego censorship. Selecting rapidly from projective archetypes bypasses your conscious defenses.",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        when (step) {
            0 -> {
                Text(
                    text = if (isFa) "گام ۱ از ۴: کدام قلمرو، هدف اصلی این فصل زندگی توست؟"
                    else "Step 1 of 4: Which domain represents your primary target?",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                GoalArchetype.entries.forEach { item ->
                    ArchetypeChoiceCard(
                        icon = item.icon,
                        title = if (isFa) item.titleFa else item.titleEn,
                        subtitle = if (isFa) item.descFa else item.descEn,
                        selected = selectedGoal == item.id,
                        accent = Color(0xFF9575CD),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedGoal = item.id
                        }
                    )
                }
            }

            1 -> {
                Text(
                    text = if (isFa) "گام ۲ از ۴ (صحنه جرم): وقتی ساعتِ شروع کار می‌رسد، ایگوی تو با کدام ترفند فرار می‌کند؟"
                    else "Step 2 of 4 (The Crime Scene): When it's time to start, how does your ego sabotage you?",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                DefenseMechanism.entries.forEach { item ->
                    ArchetypeChoiceCard(
                        icon = item.icon,
                        title = if (isFa) item.titleFa else item.titleEn,
                        subtitle = if (isFa) "${item.detailFa}\n🔬 مکانیسم: ${item.clinicalNameFa}"
                        else "${item.detailEn}\n🔬 Defense: ${item.clinicalNameEn}",
                        selected = selectedDefense == item.id,
                        accent = Color(0xFFFFB300),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedDefense = item.id
                        }
                    )
                }
            }

            2 -> {
                Text(
                    text = if (isFa) "گام ۳ از ۴ (نقشه بدنی ISTDP): اگر راه فرار را ببندیم و مجبورت کنیم الان شروع کنی، تنش و اضطراب به کجای بدنت می‌زند؟"
                    else "Step 3 of 4 (Somatic Anxiety Map): If we block your escape right now, where does tension hit your body?",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                SomaticChannel.entries.forEach { item ->
                    ArchetypeChoiceCard(
                        icon = item.icon,
                        title = if (isFa) item.titleFa else item.titleEn,
                        subtitle = if (isFa) "${item.descFa}\n⚡ ${item.istpdPathwayFa}"
                        else "${item.descEn}\n⚡ ${item.istpdPathwayEn}",
                        selected = selectedSomatic == item.id,
                        accent = Color(0xFF4DD0E1),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedSomatic = item.id
                        }
                    )
                }
            }

            3 -> {
                Text(
                    text = if (isFa) "گام ۴ از ۴ (کشف سود ثانویه): کدام حقیقتِ پنهان در ناخودآگاهت با این تعلل محافظت می‌شود؟"
                    else "Step 4 of 4 (Unconscious Secondary Gain): Which hidden truth is your procrastination secretly protecting you from?",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                CoreFearArchetype.entries.forEach { item ->
                    ArchetypeChoiceCard(
                        icon = item.icon,
                        title = if (isFa) item.titleFa else item.titleEn,
                        subtitle = if (isFa) item.descFa else item.descEn,
                        selected = selectedFear == item.id,
                        accent = Color(0xFFEC407A),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedFear = item.id
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isFa) "مرحله قبل" else "Back", color = Color.White)
                }
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        isSynthesizing = true
                        scope.launch {
                            val g = GoalArchetype.entries[selectedGoal]
                            val d = DefenseMechanism.entries[selectedDefense]
                            val s = SomaticChannel.entries[selectedSomatic]
                            val f = CoreFearArchetype.entries[selectedFear]
                            val (synFa, synEn) = HybridPsychoanalyticEngine.synthesizeProfile(g, d, s, f)
                            isSynthesizing = false
                            onProfileCreated(
                                PsychoanalyticProfile(
                                    goalArchetypeId = selectedGoal,
                                    defenseId = selectedDefense,
                                    somaticId = selectedSomatic,
                                    coreFearId = selectedFear,
                                    aiDeepSynthesisFa = synFa,
                                    aiDeepSynthesisEn = synEn,
                                    contractSigned = false,
                                    resilienceScore = existingProfile?.resilienceScore ?: 100,
                                    synapticDensity = existingProfile?.synapticDensity ?: 0
                                )
                            )
                        }
                    }
                },
                enabled = !isSynthesizing,
                modifier = Modifier.weight(2f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                if (isSynthesizing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFa) "در حال ترسیم مثلث تعارض..." else "Synthesizing Psyche Map...")
                } else {
                    Text(
                        text = if (step < 3) {
                            if (isFa) "تایید و گام بعدی ←" else "Next Step ->"
                        } else {
                            if (isFa) "🔮 ترسیم نقشه روان و مثلث تعارض" else "🔮 Generate Triangle of Conflict"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ArchetypeChoiceCard(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.18f) else Color(0xFF151C2C)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color(0xFF2D3748),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(alpha = 0.3f) else Color(0xFF1F2937)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Color(0xFFD1D5DB), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ============================================================================
// 6. TAB 2: INTERACTIVE MALAN'S TRIANGLE OF CONFLICT CANVAS & HAPTIC CONTRACT
// ============================================================================

@Composable
fun PsycheMapTriangleScreen(
    lang: AppLang,
    profile: PsychoanalyticProfile,
    onUpdateProfile: (PsychoanalyticProfile) -> Unit,
    onGoToDailyLoop: () -> Unit
) {
    val isFa = lang == AppLang.FA
    var activeVertex by remember { mutableIntStateOf(0) } // 0 = Defense, 1 = Anxiety, 2 = Hidden Feeling
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(if (profile.contractSigned) 1f else 0f) }
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseRadius"
    )

    LaunchedEffect(isHolding) {
        if (isHolding && !profile.contractSigned) {
            for (i in 1..30) {
                if (!isHolding) break
                delay(100)
                holdProgress = i / 30f
                if (i % 5 == 0) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            if (holdProgress >= 1f) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onUpdateProfile(profile.copy(contractSigned = true))
            }
        } else if (!profile.contractSigned) {
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
            text = if (isFa) "🔺 نقشه زنده «مثلث تعارض» (Malan's Triangle of Conflict)"
            else "🔺 Live Malan's Triangle of Conflict Radar",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
        )

        // Custom Drawn Neon Psychoanalytic Inverted Triangle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val pDefense = Offset(w * 0.2f, h * 0.22f)
                        val pAnxiety = Offset(w * 0.8f, h * 0.22f)
                        val pFeeling = Offset(w * 0.5f, h * 0.82f)

                        // Glowing Triangle Path
                        val path = Path().apply {
                            moveTo(pDefense.x, pDefense.y)
                            lineTo(pAnxiety.x, pAnxiety.y)
                            lineTo(pFeeling.x, pFeeling.y)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFFFB300), Color(0xFF4DD0E1), Color(0xFFEC407A))
                            ),
                            style = Stroke(width = 6f)
                        )

                        // Vertex 0: Defense (Top Left)
                        drawCircle(Color(0xFFFFB300).copy(alpha = 0.25f), radius = if (activeVertex == 0) pulseRadius else 20f, center = pDefense)
                        drawCircle(Color(0xFFFFB300), radius = 12f, center = pDefense)

                        // Vertex 1: Anxiety (Top Right)
                        drawCircle(Color(0xFF4DD0E1).copy(alpha = 0.25f), radius = if (activeVertex == 1) pulseRadius else 20f, center = pAnxiety)
                        drawCircle(Color(0xFF4DD0E1), radius = 12f, center = pAnxiety)

                        // Vertex 2: Hidden Impulse/Fear (Bottom)
                        drawCircle(Color(0xFFEC407A).copy(alpha = 0.25f), radius = if (activeVertex == 2) pulseRadius else 20f, center = pFeeling)
                        drawCircle(Color(0xFFEC407A), radius = 12f, center = pFeeling)
                    }

                    // Overlay Interactive Labels for the 3 Vertices
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = activeVertex == 0,
                            onClick = { activeVertex = 0 },
                            label = { Text(if (isFa) "🛡️ دفاع ایگو (D)" else "🛡️ Defense (D)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = activeVertex == 1,
                            onClick = { activeVertex = 1 },
                            label = { Text(if (isFa) "⚡ اضطراب بدنی (A)" else "⚡ Anxiety (A)", fontSize = 11.sp) }
                        )
                    }

                    FilterChip(
                        selected = activeVertex == 2,
                        onClick = { activeVertex = 2 },
                        label = { Text(if (isFa) "🔥 تکانه و ترس ناخودآگاه (F)" else "🔥 Hidden Impulse/Fear (F)", fontSize = 11.sp) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), modifier = Modifier.padding(vertical = 8.dp))

                // Active Vertex Breakdown
                val vertexDetail = when (activeVertex) {
                    0 -> if (isFa) "🛡️ رأس دفاع (${profile.defense.clinicalNameFa}):\nوقتی اضطراب بالا می‌رود، ایگوی تو از «${profile.defense.titleFa}» استفاده می‌کند تا مجبور نشود با ترس زیرین روبرو شود."
                    else "🛡️ Defense Vertex (${profile.defense.clinicalNameEn}):\nYour ego deploys '${profile.defense.titleEn}' to keep unconscious anxiety suppressed."

                    1 -> if (isFa) "⚡ رأس اضطراب بدنی (${profile.somatic.titleFa}):\n${profile.somatic.descFa} (${profile.somatic.istpdPathwayFa})"
                    else "⚡ Somatic Anxiety Vertex (${profile.somatic.titleEn}):\n${profile.somatic.descEn} (${profile.somatic.istpdPathwayEn})"

                    else -> if (isFa) "🔥 رأس ناخودآگاه (${profile.fear.titleFa}):\nتکانه پنهان: ${profile.fear.hiddenImpulseFa}"
                    else "🔥 Hidden Core Vertex (${profile.fear.titleEn}):\nHidden Impulse: ${profile.fear.hiddenImpulseEn}"
                }

                Text(
                    text = vertexDetail,
                    color = Color(0xFFE5E7EB),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Deep Psychoanalytic Formulation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF231934)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Color(0xFF9575CD), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa) profile.aiDeepSynthesisFa else profile.aiDeepSynthesisEn,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
                HorizontalDivider(color = Color(0xFF4A3B63))
                Text(
                    text = if (isFa) "⚡ پادزهر شرطی (IF-THEN) برای عقده‌های قاعده‌ای:" else "⚡ Basal Ganglia IF-THEN Antidote:",
                    color = Color(0xFF80CBC4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (isFa) profile.antidoteFa() else profile.antidoteEn(),
                    color = Color(0xFFE0F2F1),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // 3-Second Haptic Neuro-Contract or Proceed Button
        if (!profile.contractSigned) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF7E57C2), RoundedCornerShape(16.dp))
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
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7E57C2), Color(0xFF00897B))))
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = if (isHolding) {
                        if (isFa) "نگه دارید... ثبت تعهد عصبی (${(holdProgress * 100).toInt()}%)"
                        else "Encoding Somatosensory Contract... (${(holdProgress * 100).toInt()}%)"
                    } else {
                        if (isFa) "نگه داشتن ۳ ثانیه‌ای: پذیرش سود ثانویه و امضای قرارداد عصبی"
                        else "Hold for 3 Seconds: Sign Somatosensory Neuro-Contract"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else {
            Button(
                onClick = onGoToDailyLoop,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text(
                    text = if (isFa) "✅ قرارداد عصبی امضا شد — ورود به حلقه اجرایی روزانه"
                    else "✅ Neuro-Contract Signed — Enter Daily Neuro-Loop",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================
// 7. TAB 3: DAILY NEURO-LOOP & SMART JITAI INTERVENTION
// ============================================================================

@Composable
fun DailyNeuroLoopV2Screen(
    lang: AppLang,
    profile: PsychoanalyticProfile,
    nervousState: NervousSystemState,
    onStateChange: (NervousSystemState) -> Unit,
    onUpdateProfile: (PsychoanalyticProfile) -> Unit,
    onGoToSomatic90s: () -> Unit
) {
    val isFa = lang == AppLang.FA
    val context = LocalContext.current
    var rewardBanner by remember { mutableStateOf<String?>(null) }
    var showRecoverySheet by remember { mutableStateOf(false) }

    val activeTask = when (nervousState) {
        NervousSystemState.HIGH_DRIVE -> if (isFa) profile.goal.ceilingFa else profile.goal.ceilingEn
        NervousSystemState.VENTRAL_VAGAL -> if (isFa) profile.goal.standardFa else profile.goal.standardEn
        NervousSystemState.SYMPATHETIC, NervousSystemState.DORSAL_VAGAL ->
            if (isFa) profile.goal.survivalFa else profile.goal.survivalEn
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Polyvagal State Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x26FFFFFF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isFa) "۱. سنسور سیستم عصبی (امروز بدنت در کدام مدار است؟)"
                    else "1. Polyvagal Autonomic State (Where is your nervous system today?)",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                NervousSystemState.entries.forEach { st ->
                    val sel = st == nervousState
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) st.accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (sel) 1.5.dp else 0.5.dp,
                                color = if (sel) st.accentColor else Color(0x44FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onStateChange(st) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(st.accentColor))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isFa) st.titleFa else st.titleEn,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Text(
                    text = if (isFa) nervousState.subtitleFa else nervousState.subtitleEn,
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.sp
                )
            }
        }

        // Calibrated Elastic Mission Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.5.dp, nervousState.accentColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isFa) "🎯 ماموریت کالیبره‌شده متناسب با سیستم عصبی تو:"
                    else "🎯 Today's Biologically Calibrated Protocol:",
                    color = nervousState.accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                Text(
                    text = activeTask,
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val updated = profile.copy(synapticDensity = profile.synapticDensity + 1)
                            onUpdateProfile(updated)
                            rewardBanner = HybridPsychoanalyticEngine.generateRpeReward(lang, nervousState, profile.synapticDensity)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        Text(if (isFa) "انجام دادم (+سیناپس)" else "Executed (+Synapse)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onGoToSomatic90s,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFCA28))
                    ) {
                        Text(if (isFa) "🚨 مقاومت دارم (تخلیه ۹۰ ثانیه)" else "🚨 Resistance! (90s Reset)", fontSize = 12.sp)
                    }
                }
            }
        }

        rewardBanner?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF143328)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(14.dp))
            ) {
                Text(msg, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
            }
        }

        // Smart JITAI WorkManager Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B29)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Color(0xFF5C6BC0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa) "🔔 نوتیفیکیشن مداخله‌گر پادزهر (WorkManager JITAI)"
                    else "🔔 Smart Antidote Interceptor (WorkManager JITAI)",
                    color = Color(0xFF9FA8DA), fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "ساعت پرخطر تعلل: ${String.format("%02d:00", profile.vulnerabilityHour)}"
                        else "Vulnerability Window: ${String.format("%02d:00", profile.vulnerabilityHour)}",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val h = if (profile.vulnerabilityHour > 0) profile.vulnerabilityHour - 1 else 23
                            onUpdateProfile(profile.copy(vulnerabilityHour = h))
                            InterventionScheduler.scheduleDaily(context, h)
                        }) { Text("-1h", color = Color.White) }
                        OutlinedButton(onClick = {
                            val h = (profile.vulnerabilityHour + 1) % 24
                            onUpdateProfile(profile.copy(vulnerabilityHour = h))
                            InterventionScheduler.scheduleDaily(context, h)
                        }) { Text("+1h", color = Color.White) }
                    }
                }
                Button(
                    onClick = {
                        InterventionScheduler.triggerFiveSecondTest(context)
                        Toast.makeText(
                            context,
                            if (isFa) "نوتیفیکیشن ۵ ثانیه دیگر می‌آید! اپ را ببندید."
                            else "Notification arriving in 5 seconds! Minimize app.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                ) {
                    Text(
                        text = if (isFa) "⚡ تست نوتیفیکیشن هوشمند پادزهر (۵ ثانیه دیگر)"
                        else "⚡ Test Smart Antidote Notification (5s Delay)",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zero-Shame Recovery Trigger
        TextButton(
            onClick = { showRecoverySheet = !showRecoverySheet },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (isFa) "🧪 چند روز انجام ندادی؟ کلیک کن و بدون حس شرم ۱۵+ امتیاز تاب‌آوری بگیر"
                else "🧪 Missed days? Tap for Zero-Shame Recovery (+15 Resilience)",
                color = Color(0xFF80CBC4), fontSize = 12.sp
            )
        }

        if (showRecoverySheet) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa) "کدام عامل باعث توقف شد؟ (تبدیل خطا به دیتای آزمایشگاهی):"
                        else "Log the resistance variable (Convert lapse into lab data):",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    val reasons = if (isFa) listOf(
                        "نسخه هدف برای انرژی آن روزم بزرگ بود",
                        "مکانیسم دفاعی و کمال‌گرایی فعال شد",
                        "عامل بیرونی غیرمنتظره پیش آمد"
                    ) else listOf(
                        "Task tier exceeded my nervous system capacity",
                        "Ego defense & perfectionism took over",
                        "External unpredictable disruption"
                    )
                    reasons.forEach { r ->
                        OutlinedButton(
                            onClick = {
                                onUpdateProfile(profile.copy(resilienceScore = profile.resilienceScore + 15))
                                showRecoverySheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(r, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. TAB 4: 90-SECOND SOMATIC & PHYSIOLOGICAL SIGH RESET ORB
// ============================================================================

@Composable
fun SomaticNinetySecResetScreen(
    lang: AppLang,
    profile: PsychoanalyticProfile?,
    onCompletedReset: () -> Unit
) {
    val isFa = lang == AppLang.FA
    var secondsLeft by remember { mutableIntStateOf(90) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        if (secondsLeft == 0 && isRunning) {
            isRunning = false
            onCompletedReset()
        }
    }

    // 10-second respiratory cycle: 4s Double Inhale, 6s Long Vagal Exhale
    val cycleSecond = (90 - secondsLeft) % 10
    val isInhale = cycleSecond < 4

    val orbScale by animateFloatAsState(
        targetValue = if (!isRunning) 0.65f else if (isInhale) 1.0f else 0.45f,
        animationSpec = tween(durationMillis = if (isInhale) 3800 else 5800, easing = LinearOutSlowInEasing),
        label = "orbScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isFa) "🫁 اتاق تخلیه عصبی ۹۰ ثانیه‌ای (فرمول جیل بولت تیلور و هوبرمن)"
            else "🫁 90-Second Somatic & Vagal Reset Chamber",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center
        )

        Text(
            text = if (isFa)
                "طول عمر موج شیمیایی اضطراب و مقاومت در خون فقط ۹۰ ثانیه است. همراه با گوی نوری زیر تنفس فیزیولوژیک انجام بده و حس بدنی‌ات را بدون قضاوت تماشا کن:"
            else
                "A chemical wave of limbic resistance lasts only 90 seconds in the bloodstream. Breathe with the neural orb below to activate your vagus nerve:",
            color = Color(0xFFB0BEC5), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
        )

        // Animated Breathing & Somatic Orb Canvas
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF26A69A).copy(alpha = 0.55f), Color.Transparent)
                    ),
                    radius = maxR * orbScale
                )
                drawCircle(
                    color = if (isInhale) Color(0xFF4DD0E1) else Color(0xFF7E57C2),
                    radius = maxR * orbScale * 0.72f,
                    style = Stroke(width = 6f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${secondsLeft}s",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                )
                Text(
                    text = if (!isRunning) {
                        if (isFa) "آماده شروع" else "Ready"
                    } else if (isInhale) {
                        if (isFa) "دم عمیق دو مرحله‌ای (بینی)" else "Double Inhale (Nose)"
                    } else {
                        if (isFa) "بازدم طولانی «آه» (دهان)" else "Long Sigh Exhale (Mouth)"
                    },
                    color = Color(0xFFE0F2F1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        profile?.let { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isFa) "🎯 لنگر بدنی تو: توجه‌ات را روی «${p.somatic.titleFa}» بگذار و اجازه بده بدون فرار به «${p.defense.titleFa}»، موج انرژی از بدنت عبور کند."
                    else "🎯 Your Somatic Anchor: Focus gently on '${p.somatic.titleEn}' and watch the sensation pass without escaping into '${p.defense.titleEn}'.",
                    color = Color(0xFF80DEEA),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (secondsLeft == 0) secondsLeft = 90
                    isRunning = !isRunning
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isRunning) {
                        if (isFa) "توقف موقت" else "Pause"
                    } else {
                        if (isFa) "شروع تخلیه ۹۰ ثانیه‌ای" else "Start 90s Reset"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    isRunning = false
                    secondsLeft = 90
                },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isFa) "ریست" else "Reset", color = Color.White)
            }
        }
    }
}

// ============================================================================
// 9. UNIVERSAL AI CLOUD GATEWAY DIALOG & EMPTY STATE
// ============================================================================

@Composable
fun AiCloudGatewayDialog(lang: AppLang, onDismiss: () -> Unit) {
    val isFa = lang == AppLang.FA
    var endpoint by remember { mutableStateOf(HybridPsychoanalyticEngine.customApiEndpoint) }
    var apiKey by remember { mutableStateOf(HybridPsychoanalyticEngine.customApiKey) }
    var modelName by remember { mutableStateOf(HybridPsychoanalyticEngine.customModelName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151C2C),
        title = {
            Text(
                text = if (isFa) "🤖 تنظیمات درگاه هوش مصنوعی ابری" else "🤖 Universal Cloud AI Gateway",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa)
                        "اپلیکیشن به صورت پیش‌فرض دارای «موتور روانکاوی بالینی داخلی» است و بدون کلید هم ۱۰۰٪ کار می‌کند. برای اتصال به کلود/جمینای/گروک (از طریق OpenRouter یا AvalAI بدون تحریم)، مشخصات را وارد کنید:"
                    else
                        "The app includes a built-in Harvard Clinical Matrix that works 100% offline. To optionally connect OpenRouter, AvalAI, Groq, Claude, or Gemini, configure below:",
                    color = Color(0xFFB0BEC5), fontSize = 12.sp, lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("API Endpoint URL") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model (e.g., google/gemini-2.0-flash-001)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                HybridPsychoanalyticEngine.customApiEndpoint = endpoint
                HybridPsychoanalyticEngine.customModelName = modelName
                HybridPsychoanalyticEngine.customApiKey = apiKey
                onDismiss()
            }) {
                Text(if (isFa) "ذخیره" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "بستن" else "Close", color = Color.White)
            }
        }
    )
}

@Composable
fun EmptyProfilePrompt(lang: AppLang, onStartScan: () -> Unit) {
    val isFa = lang == AppLang.FA
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👁️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isFa) "هنوز اسکن ناخودآگاه را انجام نداده‌ای" else "No Unconscious Profile Mapped Yet",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onStartScan) {
            Text(if (isFa) "شروع اسکن ۴ مرحله‌ای (بدون تایپ)" else "Start 4-Step Tap-Only Scan")
        }
    }
}
