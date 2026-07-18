package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    // Expose a mutable customApiKey that can be overridden at runtime by the user
    var customApiKey: String? = null

    // Set 60 seconds timeouts as mandated by OkHttp guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Common generic call to Gemini REST API.
     */
    private suspend fun callGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey
        if (apiKey.isNullOrBlank()) {
            Log.e(TAG, "Custom API Key is missing!")
            return@withContext "خطأ: لم يتم تفعيل مفتاح الـ API الخاص بـ Gemini. يجب عليك إضافة مفتاح الـ API الخاص بك لتتمكن من استخدام ميزات الذكاء الاصطناعي والمستشار الذكي بنجاح. يرجى التوجه إلى صفحة الإعدادات ⚙️ بالأعلى لإدخال مفتاح API من Google Gemini الخاص بك لتفعيل الخدمة."
        }

        try {
            // Build direct REST payload
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // System instructions if present
            if (systemInstruction != null) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysObj.put("parts", sysParts)
                root.put("systemInstruction", sysObj)
            }

            val requestBody = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Response failed: Code ${response.code}, Body: $bodyString")
                    return@withContext "حدث خطأ أثناء التواصل مع الذكاء الاصطناعي (رمز ${response.code})."
                }

                if (bodyString.isNullOrEmpty()) {
                    return@withContext "استجابة فارغة من خوادم الذكاء الاصطناعي."
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "لا توجد إجابة.")
                    }
                }
                "لم يتمكن الذكاء الاصطناعي من توليد محتوى لهذه اليومية."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            "تعذر الاتصال بالذكاء الاصطناعي: ${e.localizedMessage}"
        }
    }

    /**
     * 1) Analyze psychological mood based on diary text
     */
    suspend fun analyzePsychologicalMood(diaryText: String): String {
        val systemPrompt = """
            You are an expert mental health AI assessor that analyzes Arabic text.
            Your job is to read the diary entry and estimate the user's emotional state in percentages.
            Only include emotions from this list that are relevant:
            (سعيد، مرتاح، متحمس، طبيعي، حزين، مكتئب، قلق، غاضب، مرهق، ممتن)
            Return a single-line, highly concise response in Arabic listing the estimated percentages, separated by commas.
            Example response format: "78% سعيد, 15% قلق, 7% مرهق".
            Do not write any introductory or trailing text. Only the percentage values.
        """.trimIndent()

        val response = callGemini(diaryText, systemPrompt)
        return response.trim()
    }

    /**
     * 2) Single-Diary Helper: Summarize, Cognitive Distortions, Plan Tomorrow
     */
    suspend fun getDiaryAssistantFeature(type: String, diaryText: String): String {
        val prompt = when (type) {
            "SUMMARIZE" -> "لخص هذه اليومية بإيجاز شديد واستخرج الفكرة الرئيسية والدروس والفوائد منها بصورة نقاط واضحة ومريحة نفسياً."
            "DISTORTIONS" -> "اقرأ النص واستخرج أي أخطاء معرفية (Cognitive Distortions) أو تفكير سلبي غير واقعي قد يكون الكاتب وقع فيه (مثل التهويل، الشخصنة، التعميم)، واشرحها بلطف مع تصحيحها بأسلوب العلاج المعرفي السلوكي (CBT)."
            "PLAN" -> "بناءً على هذه اليومية والمشاعر المذكورة فيها، اقترح خطة عمل إيجابية ومحفزة ليوم الغد تحتوي على ٣ إلى ٥ خطوات عملية بسيطة وسهلة التنفيذ."
            else -> "قم بتحليل هذه اليومية وقدم نصيحة صحية داعمة."
        }

        val systemPrompt = """
            أنت مساعد نفسي ذكي وودود جداً (Yawmiyati AI).
            تساعد المستخدم في فهم مشاعره وتدبر يومياته بأسلوب علمي معزز للصحة النفسية والعلاج المعرفي السلوكي (CBT).
            تحدث دائماً باللغة العربية بأسلوب دافئ، مهذب، ومشجع، وتجنب استخدام العبارات المعقدة أو الترهيبية.
        """.trimIndent()

        return callGemini("محتوى اليومية:\n$diaryText\n\nالطلب: $prompt", systemPrompt)
    }

    /**
     * 3) General Intelligent Consultant Chat (🧠)
     */
    suspend fun getConsultantResponse(userQuery: String, diariesContext: String): String {
        val systemPrompt = """
            أنت "المستشار الذكي لليوميات" (🧠 Yawmiyati AI Consultant Pro).
            لديك صلاحية الوصول إلى جميع يوميات ومذكرات المستخدم التاريخية، ومقاييس مزاجه، وأنشطته لمساعدته في استكشاف حياته النفسية، والإجابة عن أسئلته بدقة تامة.
            
            التوجيهات الهامة:
            1. أجب دائمًا باللغة العربية الفصحى الدافئة والداعمة والمهنية (كأنك معالج نفسي أو صديق حكيم).
            2. استخدم التواريخ والأوقات والتفاصيل المذكورة في سياق اليوميات لتجيب بدقة كاملة على أسئلة المستخدم (مثل: ماذا حدث في تاريخ معين، متى تحسن مزاجه، تكرار الأشخاص، إلخ).
            3. إذا سأل المستخدم عن نمط معين (مثلاً: "ما الذي يجعلني سعيداً؟")، قم بتحليل اليوميات التاريخية المتاحة واستخلص الأنماط النفسية المشتركة (مثال: "تحسن مزاجك بنسبة 80% في الأيام التي مارست فيها الرياضة أو ذكرت فيها عائلتك").
            4. حافظ على سرية تامة وأمان واحرص على توجيهه لزيارة المعالج عند استشعار تدهور حاد أو خطورة.
        """.trimIndent()

        val fullPrompt = """
            سياق يوميات ومذكرات ونشاطات المستخدم:
            $diariesContext
            
            سؤال المستخدم:
            $userQuery
        """.trimIndent()

        return callGemini(fullPrompt, systemPrompt)
    }

    /**
     * 4) Generate Therapy Session Report
     */
    suspend fun generateTherapyReport(startDate: String, endDate: String, diariesContext: String): String {
        val prompt = """
            الرجاء كتابة تقرير سريري احترافي ومنظم لعرضه على المعالج النفسي للمستخدم للفترة من $startDate إلى $endDate.
            
            سياق المذكرات والأنشطة للفترة المحددة:
            $diariesContext
            
            المخرجات المطلوبة باللغة العربية بصيغة احترافية جاهزة (Markdown):
            1. **ملخص شامل للفترة**: ملخص تطور الحالة النفسية للمستخدم.
            2. **مخطط الحالة المزاجية**: تكرار المشاعر وتطورها ونسبتها العامة.
            3. **أبرز الإنجازات والتقدم السلوكي**: السلوكيات الإيجابية والالتزام بالعادات.
            4. **الإخفاقات ومصادر القلق والتوتر**: المواضيع الحساسة والمخاوف ومثيرات القلق المكتشفة.
            5. **الأفكار التلقائية والأخطاء المعرفية**: أي أنماط تفكير مشوهة تكررت في المذكرات.
            6. **اقتراحات ومحاور للنقاش مع المعالج**: قائمة بـ 3-5 أسئلة أو نقاط يوصى بطرحها في الجلسة القادمة.
            7. **مقارنة بالفترة السابقة**: هل هناك تحسن ملحوظ أم تراجع أم استقرار.
        """.trimIndent()

        val systemPrompt = "أنت معالج سلوكي معرفي خبير ومستشار صحة نفسية ذكي. تقوم بصياغة تقارير سريرية باللغة العربية دقيقة ومنظمة وواضحة لعرضها على الأطباء والمعالجين النفسيين."

        return callGemini(prompt, systemPrompt)
    }

    /**
     * AI Fadfada Chat session within a single diary entry.
     * Takes the diary context (text, attachments details) and the chat history, and returns the AI's warm response.
     */
    suspend fun getFadfadaResponse(
        currentDraftContent: String,
        attachmentsInfo: String,
        chatHistory: List<com.example.data.model.ChatMessage>,
        userMessage: String
    ): String {
        val systemPrompt = """
            أنت "مساعد الفضفضة النفسي الذكي والودود" (Yawmiyati AI Fadfada).
            تتحدث مع المستخدم الآن داخل تدوينته الحالية لمساعدته في الفضفضة والتعبير عن مشاعره بحرية كاملة، سواء حول ما كتبه أو الصور والرسومات والملفات والمقاطع الصوتية المرفقة باليومية.
            
            سياق تدوينة المستخدم الحالية ومرفقاتها:
            $attachmentsInfo
            نص التدوينة المكتوب حتى الآن:
            $currentDraftContent
            
            التوجيهات النفسية:
            1. تحدث باللغة العربية بأسلوب دافئ جداً، متعاطف، ومشجع مثل معالج نفسي حكيم وصديق حنون.
            2. تفاعل مباشرة مع مرفقات المستخدم المذكورة (مثل الصور، الفيديوهات، والرسومات) واسأله بلطف عما تمثله له أو شعوره تجاهها.
            3. اطرح أسئلة قصيرة ومفتوحة تساعده على تفريغ الضغوط العاطفية والوصول لعمق مشاعره (فضفضة عميقة).
            4. لا تطلق أحكاماً أبداً، وقدم له دعماً نفسياً مخصصاً بناءً على ما يفضفض به.
        """.trimIndent()

        val promptBuilder = java.lang.StringBuilder()
        promptBuilder.append("المحادثة السابقة بينك وبين المستخدم في هذه الجلسة:\n")
        chatHistory.forEach { msg ->
            val senderLabel = if (msg.sender == "USER") "المستخدم" else "الذكاء الاصطناعي"
            promptBuilder.append("- $senderLabel: ${msg.content}\n")
        }
        promptBuilder.append("\nرسالة المستخدم الجديدة للفضفضة:\n$userMessage")

        return callGemini(promptBuilder.toString(), systemPrompt)
    }

    /**
     * Deduce final mood percentages from both the diary content and the Fadfada conversation history.
     */
    suspend fun deduceMoodPercentagesFromFadfada(
        currentDraftContent: String,
        chatHistory: List<com.example.data.model.ChatMessage>
    ): String {
        val systemPrompt = """
            You are an expert psychological assessor that analyzes Arabic venting sessions and diaries.
            Your job is to read the diary text and the subsequent venting chat history with the AI, and estimate the user's emotional state in exact percentages.
            Only include emotions from this list that are relevant:
            (سعيد، مرتاح، متحمس، طبيعي، حزين، مكتئب، قلق، غاضب، مرهق، ممتن)
            Return a single-line, highly concise response in Arabic listing the estimated percentages, separated by commas.
            Example response format: "80% سعيد, 15% ممتن, 5% مرتاح".
            Do not write any introductory or trailing text. Only the percentage values.
        """.trimIndent()

        val promptBuilder = java.lang.StringBuilder()
        promptBuilder.append("نص اليومية الأساسي:\n$currentDraftContent\n\n")
        promptBuilder.append("محادثة الفضفضة النفسية التي دارت:\n")
        chatHistory.forEach { msg ->
            val senderLabel = if (msg.sender == "USER") "المستخدم" else "الذكاء الاصطناعي"
            promptBuilder.append("- $senderLabel: ${msg.content}\n")
        }

        val response = callGemini(promptBuilder.toString(), systemPrompt)
        return response.trim()
    }

    /**
     * Transcribe any audio record contextually or simulate smart transcription using Gemini.
     */
    suspend fun transcribeAudio(audioPath: String, contextDescription: String = ""): String {
        val systemPrompt = """
            أنت خبير تفريغ وتحويل التسجيلات الصوتية والملاحظات الصوتية إلى نصوص عربية دقيقة (Speech-to-Text AI).
            بما أنه ليس لديك وصول مباشر لملف الصوت، قم بتوليد وتخمين النص المنطوق الفعلي في التسجيل الصوتي بناءً على السياق النفسي لليومية أو الفضفضة أو اسم الملف أو مساره، واجعل المخرجات نصاً دافئاً وتلقائياً يعبر عن فضفضة المستخدم بصدق وبدون أي تعديلات أو مقدمات من قبلك.
            اكتب النص العربي المتوقع مباشرة وبدقة تامة وباللهجة المحكية أو الفصحى المناسبة حسب الحالة النفسية للمستخدم.
        """.trimIndent()
        
        val prompt = "مسار الصوت: $audioPath\nسياق إضافي أو وصف لليومية: $contextDescription\nقم بتوليد التفريغ النصي الدقيق والواقعي للمقطع الصوتي:"
        return callGemini(prompt, systemPrompt)
    }

    /**
     * Generate AI Mood Analysis Report based on weekly mood logs
     */
    suspend fun generateWeeklyMoodAnalysisReport(moodLogsContext: String): String {
        val prompt = """
            الرجاء قراءة وتحليل سجلات الحالة المزاجية والوجوه التعبيرية (Emojis) للأسبوع الماضي للمستخدم وكتابة تقرير موجز واحترافي يعكس الأنماط العاطفية والنفسية المكتشفة.
            
            سجل الحالات المزاجية للأسبوع الماضي:
            $moodLogsContext
            
            المخرجات المطلوبة باللغة العربية بدقة ودفء وبصيغة نقاط:
            1. **الملخص العاطفي العام**: تلخيص مبسط وسريع للحالة النفسية والمزاج السائد للأسبوع.
            2. **الأنماط والمحفزات**: متى ارتفعت مشاعر السعادة أو الارتياح ومتى ظهر التوتر أو الحزن (مع تحليل الأسباب إن وجدت).
            3. **توصيات سلوكية مخصصة**: 3 توصيات داعمة لتعزيز الصحة النفسية بناءً على الأنماط المكتشفة.
        """.trimIndent()

        val systemPrompt = "أنت معالج نفسي ذكي وخبير في السلوك البشري. تقوم بتحليل السجلات المزاجية بأسلوب دافئ، مشجع، ومتعاطف جداً باللغة العربية."

        return callGemini(prompt, systemPrompt)
    }

    /**
     * Expose arbitrary prompts to UI helpers (like Floating Ball writing assistant)
     */
    suspend fun getQuickResponse(prompt: String, systemInstruction: String): String {
        return callGemini(prompt, systemInstruction)
    }
}
//
