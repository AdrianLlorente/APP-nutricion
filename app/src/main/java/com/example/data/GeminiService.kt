package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class FoodExtractionResult(
    val foodName: String,
    val calories: Double,
    val carbs: Double,
    val proteins: Double,
    val fats: Double,
    val sugar: Double,
    val micronutrients: String,
    val mealType: String // Breakfast, Lunch, Dinner, Snack
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun extractNutrition(userText: String): FoodExtractionResult? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is placeholder")
            return null
        }

        val prompt = """
            Analiza el siguiente texto de comida del usuario: "$userText".
            Calcula el desglose nutricional estimado basándote en cantidades promedio estándar de los alimentos descritos.
            Devuelve un JSON estructurado que coincida exactamente con este formato:
            {
              "foodName": "Nombre descriptivo del plato o alimento (ej: Salmón con arroz)",
              "calories": 450.0,
              "carbs": 40.0,
              "proteins": 35.0,
              "fats": 15.0,
              "sugar": 2.0,
              "micronutrients": "Principales micronutrientes (ej: Omega-3, Vitamina D, Potasio)",
              "mealType": "Lunch"
            }
            Reglas críticas:
            1. 'mealType' SOLO puede ser uno de estos valores: "Breakfast", "Lunch", "Dinner", "Snack". Elige el más adecuado según el alimento o descripción.
            2. El idioma de 'foodName' y 'micronutrients' debe ser Español.
            3. Devuelve únicamente el objeto JSON, estructurado tal como se solicita.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseFormatText(mimeType = "application/json")),
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "Eres un experto nutricionista clínico con especialidad en desglose de alimentos.")))
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Gemini Raw Response: $jsonText")
                return moshi.adapter(FoodExtractionResult::class.java).fromJson(jsonText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini extractNutrition call", e)
        }
        return null
    }

    suspend fun generateCoachAnalysis(logsSummaryText: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "La clave de API de Gemini no está configurada o es incorrecta. Por favor añádela en la sección de Secrets."
        }

        val prompt = """
            Analiza los siguientes registros de alimentación de la última semana:
            $logsSummaryText
            
            Genera un análisis nutricional constructivo en Español de máximo 3 párrafos, amigable, motivador y profesional.
            Evalúa si el usuario o grupo está cumpliendo con una ingesta saludable de calorías y equilibrio de macros.
            Proporciona 3 recomendaciones claras y accionables (en viñetas) para mejorar sus hábitos.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = "Eres un Coach Nutricional de confianza, amigable, científico pero accesible, que ayuda a parejas y familias a mejorar sus hábitos nutricionales juntos.")))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No se pudo generar el análisis. Inténtalo de nuevo."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating coach analysis", e)
            "Error al contactar con el coach virtual: ${e.localizedMessage}"
        }
    }
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}
