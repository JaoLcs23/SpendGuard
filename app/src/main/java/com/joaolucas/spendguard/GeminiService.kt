package com.joaolucas.spendguard

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class GeminiService(apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val API_TIMEOUT_MS   = 20_000L
    private val MAX_RETRIES      = 2
    private val MAX_INPUT_LENGTH = 500

    private fun sanitize(input: String): String =
        input
            .take(MAX_INPUT_LENGTH)
            .replace(Regex("[\\x00-\\x1F\\x7F`]"), " ")
            .trim()

    suspend fun calculateOpportunityCost(amount: Double): OpportunityAnalysis {
        val prompt = """
            Você é um analista financeiro virtual fornecendo projeções de mercado educacionais.
            O usuário deixou de gastar R$ ${"%.2f".format(amount)}.
            Gere uma simulação de investimentos dinâmica, realista e com tom estritamente profissional, variando os prazos e setores a cada vez.

            REGRA DE OURO: NUNCA use palavras como "hipotético", "fictício", "inventado" ou "exemplo". Use termos profissionais como "Projeção", "Cenário", "Média do setor" ou "Estimativa". Não cite tickers (códigos) específicos de ações/fundos para evitar filtros de segurança, cite apenas o setor.

            1. Fundos Imobiliários: Escolha um setor forte (ex: Shoppings, Lajes Corporativas, Logística, Papel). Assuma um valor de cota realista e um Dividend Yield médio de mercado (ex: 0,7% a 1,1% ao mês). Mostre o potencial de renda passiva mensal projetada.
            2. Renda Fixa: Escolha um prazo aleatório para a projeção (ex: 1, 2, 3 ou 5 anos). Compare o rendimento projetado do Tesouro Selic contra a Poupança neste período.
            3. Ações na B3: Escolha um setor resiliente (ex: Energia, Saneamento, Bancos, Seguros). Projete um retorno baseado em médias históricas de dividendos desse mercado (ex: 6% a 10% a.a.).
            4. Mensagem: Uma frase de impacto curta e profissional sobre inteligência financeira, o poder dos juros compostos ou visão de longo prazo.

            Não use markdown, asteriscos, negrito nem marcadores. Escreva em texto simples corrido.
            
            Retorne APENAS um JSON válido seguindo este esquema exato:
            {"fiis": "string", "savings": "string", "stocks": "string", "motivationalMessage": "string"}
        """.trimIndent()

        var lastEx: Exception? = null
        var genResponse: com.google.ai.client.generativeai.type.GenerateContentResponse? = null
        for (attempt in 0..MAX_RETRIES) {
            try {
                genResponse = withTimeout(API_TIMEOUT_MS) { generativeModel.generateContent(prompt) }
                break
            } catch (e: Exception) { lastEx = e; if (attempt < MAX_RETRIES) delay(1_000L * (attempt + 1)) }
        }
        val text = genResponse?.text ?: throw (lastEx ?: Exception("Resposta vazia da API"))

        val regex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text)
        val cleanJson = match?.value ?: throw Exception("JSON não encontrado na resposta")

        return jsonParser.decodeFromString<OpportunityAnalysis>(cleanJson)
    }

    suspend fun analyzeImpulse(
        item: String,
        price: Double,
        justification: String,
        userProfile: FinancialProfile = FinancialProfile(),
        emotionalState: EmotionalState? = null
    ): InterventionResult {
        val safeItem          = sanitize(item)
        val safeJustification = sanitize(justification)
        val profileContext    = userProfile.toPromptContext()

        val profileSection = if (profileContext.isNotEmpty())
            "\n    PERFIL DO USUÁRIO (use para personalizar a análise):\n    $profileContext\n"
        else ""

        val hourlyRate = if (userProfile.monthlyIncome > 0) userProfile.monthlyIncome / 160.0 else 0.0
        val workHours  = if (hourlyRate > 0) "%.1f".format(price / hourlyRate) else null
        val workSection = if (workHours != null)
            "\n    CONTEXTO DE TEMPO: Este item custa aproximadamente $workHours horas de trabalho do usuário. Mencione isso na análise de forma reflexiva.\n"
        else ""

        val emotionalSection = if (emotionalState != null)
            "\n    ESTADO EMOCIONAL ATUAL DO USUÁRIO: ${emotionalState.label}. Leve isso em conta — decisões em estado emocional negativo tendem a ser arrependidas.\n"
        else ""

        val categoryOptions = SpendingCategory.geminiOptions

        val prompt = """
    Você é o 'SpendGuard', um guardião financeiro rigoroso, porém empático.
    Sua missão é proteger o usuário de compras por impulso e validar decisões financeiras racionais.
$profileSection$workSection$emotionalSection
    DADOS DA TENTATIVA DE COMPRA:
    - Item: "$safeItem"
    - Valor: R$ ${"%.2f".format(price)}
    - Justificativa do usuário: "$safeJustification"

    CRITÉRIOS DE AVALIAÇÃO:
    1. Racional (allowed: true): A justificativa demonstra necessidade real (ex: saúde, ferramenta de trabalho essencial), planejamento financeiro prévio ou substituição urgente.
    2. Impulsiva (allowed: false): A justificativa é puramente emocional (ex: "eu mereço", "está na promoção", "só se vive uma vez"), vaga, ou trata-se de um luxo caro sem planejamento.
    ${if (profileContext.isNotEmpty()) "Leve em conta a renda e o objetivo financeiro do usuário ao calibrar a severidade da análise." else ""}

    REGRAS DE RETORNO (JSON):
    - allowed: booleano (true ou false).
    - message: Se bloqueado, dê uma "bronca" amigável e reflexiva, focando no longo prazo. Se permitido, parabenize a consciência financeira. (Máximo de 3 linhas).
    - coolingOffTime: Se allowed=false, defina um tempo de reflexão em HORAS proporcional ao valor. Referência:
      * 24 horas: compras até R$100
      * 48 horas: compras entre R$100–R$500
      * 72 horas: compras entre R$500–R$1000
      * 168 horas (1 semana): compras acima de R$1000
      Se allowed=true, retorne 0. O valor representa HORAS, não minutos.
    - category: Classifique o item em UMA das categorias abaixo. Retorne EXATAMENTE o nome em maiúsculas, sem alteração.
      Opções: $categoryOptions

    IMPORTANTE: Retorne APENAS um objeto JSON válido. NÃO use marcação markdown (como ```json), não adicione notas ou textos fora das chaves.

    Formato EXATO esperado:
    {
      "allowed": boolean,
      "message": "string",
      "coolingOffTime": number,
      "category": "string"
    }
""".trimIndent()

        val response = withTimeout(API_TIMEOUT_MS) {
            generativeModel.generateContent(prompt)
        }
        val text = response.text ?: throw Exception("Resposta vazia da API")

        val regex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text)
        val cleanJson = match?.value ?: throw Exception("JSON não encontrado na resposta")

        val result = jsonParser.decodeFromString<InterventionResult>(cleanJson)

        return result.copy(
            category = SpendingCategory.fromString(result.category).name
        )
    }

    suspend fun generateWeeklyInsight(
        purchases: List<PurchaseEntity>,
        userProfile: FinancialProfile = FinancialProfile()
    ): WeeklyInsight {
        val blocked  = purchases.filter { it.wasBlocked }
        val approved = purchases.filter { !it.wasBlocked }
        val saved    = blocked.sumOf { it.price }
        val spent    = approved.sumOf { it.price }
        val topCategory = blocked.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "N/A"
        val profileCtx = userProfile.toPromptContext()

        val prompt = """
Você é o SpendGuard, assistente financeiro pessoal.
Esta é a análise semanal do usuário.

DADOS DA SEMANA:
- Compras bloqueadas: ${blocked.size} (R$ ${"%.2f".format(saved)} protegidos)
- Compras aprovadas: ${approved.size} (R$ ${"%.2f".format(spent)} gastos)
- Categoria de maior risco: $topCategory
${if (profileCtx.isNotEmpty()) "- Perfil: $profileCtx" else ""}

Gere um insight semanal personalizado, honesto e encorajador em JSON:
{
  "summary": "2-3 frases sobre a semana do usuário, mencionando padrões reais",
  "fiis": "projeção FII com setor, yield e renda passiva potencial se o valor protegido fosse investido",
  "savings": "comparativo Tesouro Selic vs Poupança para o valor protegido em 1 ano",
  "stocks": "projeção de ações com setor resiliente e retorno médio histórico",
  "motivationalMessage": "uma frase de impacto curta sobre a conquista da semana"
}

Retorne APENAS JSON válido, sem markdown.
        """.trimIndent()

        val response = withTimeout(API_TIMEOUT_MS) { generativeModel.generateContent(prompt) }
        val text = response.text ?: throw Exception("Resposta vazia")
        val cleanJson = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(text)?.value
            ?: throw Exception("JSON não encontrado")
        return jsonParser.decodeFromString<WeeklyInsight>(cleanJson).copy(generatedAt = System.currentTimeMillis())
    }

    suspend fun extractPurchaseInfo(notificationText: String): PurchaseInfo? {
        val safeText = sanitize(notificationText)

        val prompt = """
            Extraia informações de compra deste texto de notificação: "$safeText"
            
            Retorne APENAS JSON seguindo este esquema:
            {"itemName": "nome do produto/serviço", "price": 0.0}
            
            Se não conseguir identificar claramente, retorne: {"itemName": "Compra detectada", "price": 0.0}
        """.trimIndent()

        return try {
            val response = withTimeout(API_TIMEOUT_MS) {
                generativeModel.generateContent(prompt)
            }
            val text = response.text ?: return null

            val regex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
            val cleanJson = regex.find(text)?.value ?: return null

            jsonParser.decodeFromString<PurchaseInfo>(cleanJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractPixInfo(notificationText: String): PixInfo? {
        val safeText = sanitize(notificationText)

        val prompt = """
            Extraia informações deste texto de notificação de PIX enviado: "$safeText"

            Retorne APENAS JSON seguindo este esquema:
            {"recipient": "nome ou chave do destinatário", "amount": 0.0, "description": "motivo se disponível"}

            Se não conseguir identificar o destinatário, use: "Destinatário não identificado"
            Se não conseguir identificar o valor, use: 0.0
            Se não houver descrição, use: ""
        """.trimIndent()

        return try {
            val response = withTimeout(API_TIMEOUT_MS) {
                generativeModel.generateContent(prompt)
            }
            val text = response.text ?: return null

            val regex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
            val cleanJson = regex.find(text)?.value ?: return null

            jsonParser.decodeFromString<PixInfo>(cleanJson)
        } catch (_: Exception) {
            null
        }
    }
}