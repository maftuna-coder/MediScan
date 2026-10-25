
package com.example.mediscan
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.net.URLEncoder
import android.text.Html
private val Teal = Color(0xFF087F8C)
private val PaleTeal = Color(0xFFE8F6F5)
private val DarkTeal = Color(0xFF07545B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MediScanApp()
            }
        }
    }
}

data class HealthArticle(
    val title: String,
    val url: String,
    val summary: String
)

data class Assessment(
    val urgency: String,
    val explanation: String,
    val advice: String
)


data class HistoryEntry(
    val date: Long,
    val symptoms: String,
    val severity: String,
    val temperature: String,
    val urgency: String,
    val explanation: String,
    val advice: String
)


object IntelligentEngine {

    fun assess(
        symptoms: Set<String>,
        severity: String,
        temperature: Float?
    ): Assessment {

        // Educational safety rules take priority over ordinary rules.
        if ("Difficulty breathing" in symptoms ||
            "Chest pain" in symptoms ||
            "Confusion" in symptoms
        ) {
            return Assessment(
                "Emergency warning",
                "A reported warning sign may indicate a medical emergency.",
                "Seek emergency medical help now. Do not rely on this app to assess the severity."
            )
        }

        if (severity == "Severe" ||
            (temperature != null && temperature >= 40f)
        ) {
            return Assessment(
                "Prompt medical assessment",
                "Severe symptoms or a very high reported temperature need prompt evaluation.",
                "Contact a healthcare professional urgently. Seek emergency help if you feel seriously unwell."
            )
        }

        if (symptoms.isEmpty()) {
            return Assessment(
                "More information needed",
                "No symptoms were selected.",
                "Select at least one symptom to continue."
            )
        }

        return Assessment(
            "Preliminary assessment",
            "The selected symptoms do not establish a specific diagnosis.",
            "Monitor your symptoms and seek professional advice if they persist, worsen, or concern you."
        )
    }
}

object HealthApi {

    suspend fun search(query: String): List<HealthArticle> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val address =
                "https://wsearch.nlm.nih.gov/ws/query" +
                        "?db=healthTopics&term=$encoded"

            val connection = URL(address).openConnection()
                    as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty(
                "User-Agent", "MediScanStudentProject/1.0"
            )

            try {
                connection.getInputStream().use { stream ->
                    val parser = Xml.newPullParser()
                    parser.setFeature(
                        XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                        false
                    )
                    parser.setInput(stream, "UTF-8")

                    val articles = mutableListOf<HealthArticle>()
                    var currentUrl = ""
                    var title = ""
                    var summary = ""
                    var event = parser.eventType

                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name) {
                                    "document" -> {
                                        currentUrl =
                                            parser.getAttributeValue(
                                                null, "url"
                                            ) ?: ""
                                        title = ""
                                        summary = ""
                                    }

                                    "content" -> {
                                        val name =
                                            parser.getAttributeValue(
                                                null, "name"
                                            )
                                        val rawValue = parser.nextText().trim()
                                        val value = Html.fromHtml(
                                            rawValue,
                                            Html.FROM_HTML_MODE_LEGACY
                                        ).toString().trim()

                                        when (name) {
                                            "title" -> title = value
                                            "snippet" -> summary = value
                                        }
                                    }
                                }
                            }

                            XmlPullParser.END_TAG -> {
                                if (parser.name == "document" &&
                                    title.isNotBlank() &&
                                    currentUrl.startsWith(
                                        "https://medlineplus.gov/"
                                    )
                                ) {
                                    articles.add(
                                        HealthArticle(
                                            title = title,
                                            url = currentUrl,
                                            summary = summary
                                        )
                                    )
                                }
                            }
                        }
                        event = parser.next()
                    }

                    articles.take(5)
                }
            } finally {
                connection.disconnect()
            }
        }
}


object HistoryStore {
    private const val PREFS_NAME = "mediscan_history"
    private const val KEY_HISTORY = "assessment_records"

    fun getAll(context: android.content.Context): List<HistoryEntry> {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )

        val json = org.json.JSONArray(
            prefs.getString(KEY_HISTORY, "[]")
        )

        return (0 until json.length()).mapNotNull { index ->
            val item = json.optJSONObject(index) ?: return@mapNotNull null

            HistoryEntry(
                date = item.optLong("date"),
                symptoms = item.optString("symptoms"),
                severity = item.optString("severity"),
                temperature = item.optString("temperature"),
                urgency = item.optString("urgency"),
                explanation = item.optString("explanation"),
                advice = item.optString("advice")
            )
        }.reversed()
    }

    fun save(context: android.content.Context, entry: HistoryEntry) {
        val records = getAll(context).toMutableList()
        records.add(0, entry)

        val json = org.json.JSONArray()

        records.take(50).forEach { record ->
            val item = org.json.JSONObject()
            item.put("date", record.date)
            item.put("symptoms", record.symptoms)
            item.put("severity", record.severity)
            item.put("temperature", record.temperature)
            item.put("urgency", record.urgency)
            item.put("explanation", record.explanation)
            item.put("advice", record.advice)
            json.put(item)
        }

        context.getSharedPreferences(
            PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_HISTORY, json.toString())
            .apply()
    }

    fun clear(context: android.content.Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        ).edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}






@Composable
fun MediScanApp() {
    var screen by remember { mutableStateOf("home") }

    when (screen) {
        "home" -> HomeScreen(
            onStart = { screen = "assessment" },
            onHistory = { screen = "history" }
        )

        "assessment" -> AssessmentScreen(
            onBack = { screen = "home" }
        )

        "history" -> HistoryScreen(
            onBack = { screen = "home" }
        )
    }
}


@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            "✚  MediScan",
            color = DarkTeal,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "INTELLIGENT HEALTH ASSISTANT",
            color = Teal,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaleTeal, RoundedCornerShape(24.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Understand your symptoms.",
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )

            Text(
                "Explore an educational symptom assessment " +
                        "and reliable health information.",
                color = Color(0xFF49686B),
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Symptom Checker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                Text(
                    "Enter symptoms and receive an explainable, " +
                            "rule-based assessment."
                )
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal
                    )
                ) {
                    Text("Check Symptoms")
                }
            }
        }
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Assessment History")
        }
        Text(
            "Educational use only. MediScan does not diagnose " +
                    "diseases or replace a healthcare professional.",
            fontSize = 12.sp,
            color = Color.Gray,
            lineHeight = 19.sp
        )
    }
}


@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var records by remember {
        mutableStateOf(HistoryStore.getAll(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to home", color = Teal)
        }

        Text(
            "Assessment History",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTeal
        )

        Text(
            "Your saved assessments on this device.",
            color = Color.Gray
        )

        if (records.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    HistoryStore.clear(context)
                    records = emptyList()
                }
            ) {
                Text("Delete all history")
            }
        }

        if (records.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    "No saved assessments yet. Complete a symptom assessment to see it here.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            records.forEach { record ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            record.urgency,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )

                        Text("Symptoms: ${record.symptoms}")
                        Text("Severity: ${record.severity}")
                        Text("Temperature: ${record.temperature}")
                        Text(record.explanation)
                        Text(record.advice, color = Color.DarkGray)

                        Text(
                            java.text.SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(record.date)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssessmentScreen(onBack: () -> Unit) {
    val symptoms = listOf(
        "Fever",
        "Cough",
        "Headache",
        "Sore throat",
        "Fatigue",
        "Nausea",
        "Difficulty breathing",
        "Chest pain",
        "Confusion"
    )
    val context = LocalContext.current
    var selected by remember { mutableStateOf(setOf<String>()) }
    var severity by remember { mutableStateOf("Moderate") }
    var temperature by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf<Assessment?>(null) }

    var articles by remember { mutableStateOf<List<HealthArticle>>(emptyList()) }
    var researchArticles by remember {
        mutableStateOf<List<HealthArticle>>(emptyList())
    }
    var loading by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to home", color = Teal)
        }

        Text(
            "Symptom Assessment",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTeal
        )

        Text("Select all symptoms that apply.")

        symptoms.forEach { symptom ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = symptom in selected,
                    onCheckedChange = { checked ->
                        selected = if (checked) {
                            selected + symptom
                        } else {
                            selected - symptom
                        }
                        assessment = null
                        articles = emptyList()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Teal
                    )
                )
                Text(symptom)
            }
        }

        Text(
            "Temperature (°C, optional)",
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = temperature,
            onValueChange = {
                temperature = it
                assessment = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 37.5") },
            singleLine = true
        )

        Text(
            "Reported severity",
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Mild", "Moderate", "Severe").forEach { option ->
                FilterChip(
                    selected = severity == option,
                    onClick = {
                        severity = option
                        assessment = null
                    },
                    label = { Text(option) }
                )
            }
        }

        Button(
            onClick = {
                val temp = temperature.toFloatOrNull()
                val validTemp = temperature.isBlank() ||
                        (temp != null && temp in 25f..45f)

                if (!validTemp || selected.isEmpty()) {
                    assessment = Assessment(
                        "Check your entries",
                        if (!validTemp) {
                            "Enter a valid temperature between 25 and 45 °C."
                        } else {
                            "Select at least one symptom."
                        },
                        "Correct the entries and analyze again."
                    )
                    articles = emptyList()
                } else {
                    assessment = IntelligentEngine.assess(
                        selected,
                        severity,
                        temp
                    )


                    HistoryStore.save(
                        context,
                        HistoryEntry(
                            date = System.currentTimeMillis(),
                            symptoms = selected.joinToString(", "),
                            severity = severity,
                            temperature = temperature.ifBlank { "Not provided" },
                            urgency = assessment!!.urgency,
                            explanation = assessment!!.explanation,
                            advice = assessment!!.advice
                        )
                    )

                    articles = emptyList()
                    apiError = ""

                    // Search reference material only when appropriate.
                    val query = selected
                        .filter {
                            it !in setOf(
                                "Difficulty breathing",
                                "Chest pain",
                                "Confusion"
                            )
                        }
                        .take(3)
                        .joinToString(" ")

                    if (query != null) {
                        loading = true
                        scope.launch {

                            try {
                                articles = withContext(Dispatchers.IO) {
                                    HealthApi.search(query)
                                }

                                if (articles.isEmpty()) {
                                    apiError =
                                        "No matching MedlinePlus articles were returned."
                                }
                            }catch (e: Exception) {
                                apiError =
                                    "Health information is temporarily " +
                                            "unavailable. Check your internet connection."
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Teal
            )
        ) {
            Text("Analyze Symptoms")
        }

        assessment?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PaleTeal
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        result.urgency,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(result.explanation)
                    Text(
                        result.advice,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        if (assessment != null &&
            assessment?.urgency != "Check your entries"
        ) {
            Text(
                "Health Information — MedlinePlus",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )

            when {
                loading -> CircularProgressIndicator(color = Teal)
                apiError.isNotBlank() -> Text(apiError)
                articles.isEmpty() -> Text(
                    "No health articles available for this query."
                )
                else -> articles.forEach { article ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                article.title,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )
                            if (article.summary.isNotBlank()) {
                                Text(article.summary)
                            }
                            TextButton(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(article.url)
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Read on MedlinePlus")
                            }
                        }
                    }
                }
            }
        }

// Your existing code above...


    }
}
