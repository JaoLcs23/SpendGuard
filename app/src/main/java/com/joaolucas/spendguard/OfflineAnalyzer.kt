package com.joaolucas.spendguard

object OfflineAnalyzer {
    private val impulseKeywords = listOf(
        "quero", "desejo", "mereco", "mereço", "vontade", "promoção", "promocao",
        "desconto", "achei barato", "oferta", "liquidação", "liquidacao",
        "só hoje", "so hoje", "última chance", "ultima chance", "fomo",
        "todo mundo tem", "tô com vontade", "to com vontade", "me deu vontade",
        "tô precisando", "to precisando"
    )

    private val necessityKeywords = listOf(
        "quebrou", "estragou", "preciso trabalhar", "ferramenta", "saúde", "saude",
        "médico", "medico", "remédio", "remedio", "urgente", "necessário", "necessario",
        "substituir", "parou de funcionar", "essencial", "trabalho", "sem condição",
        "sem condicao", "necessidade", "obrigação", "obrigacao",
        "prova", "escola", "faculdade", "estudo", "estudar", "aula", "curso",
        "material", "material escolar", "lápis", "caneta", "caderno", "apostila",
        "alimentação", "alimentacao", "almoço", "almoco", "jantar", "café", "cafe",
        "transporte", "passagem", "combustível", "combustivel", "gasolina",
        "conta", "boleto", "aluguel", "luz", "água", "agua", "internet", "telefone",
        "higiene", "sabonete", "shampoo", "pasta de dente", "papel higiênico",
        "limpeza", "produto de limpeza", "acabou", "terminou", "faltou", "sem estoque",
        "reposição", "reposicao", "preciso", "precisa", "precisamos"
    )

    private val categoryKeywords: List<Pair<SpendingCategory, List<String>>> = listOf(
        SpendingCategory.SAUDE to listOf(
            "farmácia", "farmacia", "remédio", "remedio", "médico", "medico",
            "consulta", "exame", "hospital", "clínica", "clinica", "saúde", "saude",
            "dentista", "vacina", "cirurgia", "plano de saúde"
        ),
        SpendingCategory.ALIMENTACAO to listOf(
            "supermercado", "mercado", "açougue", "acougue", "padaria", "restaurante",
            "lanchonete", "pizza", "hamburguer", "hamburger", "ifood", "rappi",
            "delivery", "comida", "alimento", "hortifruti", "pão", "pao", "carne",
            "frango", "leite", "feira", "lanche"
        ),
        SpendingCategory.TRANSPORTE to listOf(
            "uber", "99", "combustível", "combustivel", "gasolina", "etanol",
            "passagem", "ônibus", "onibus", "metrô", "metro", "trem", "táxi", "taxi",
            "estacionamento", "pedágio", "pedagio", "manutenção do carro", "pneu",
            "troca de óleo", "shell box", "ipiranga", "abastecimento"
        ),
        SpendingCategory.TECNOLOGIA to listOf(
            "celular", "smartphone", "notebook", "computador", "tablet", "fone",
            "headphone", "earphone", "teclado", "mouse", "monitor", "hd", "ssd",
            "memória", "memoria", "processador", "placa de vídeo", "video", "câmera",
            "camera", "impressora", "carregador", "cabo", "adaptador", "iphone",
            "samsung", "apple", "xiaomi", "samsung", "game", "console", "playstation",
            "xbox", "nintendo", "eletrônico", "eletronico"
        ),
        SpendingCategory.VESTUARIO to listOf(
            "roupa", "roupas", "camisa", "camiseta", "calça", "calca", "shorts",
            "vestido", "blusa", "jaqueta", "casaco", "tênis", "tenis", "sapato",
            "sandália", "sandalia", "bota", "chinelo", "meia", "cueca", "sutiã",
            "sutia", "bermuda", "moletom", "cinto", "bolsa", "mochila",
            "acessório", "acessorio", "óculos", "oculos", "relógio", "relogio",
            "shein", "renner", "riachuelo", "c&a", "hering"
        ),
        SpendingCategory.LAZER to listOf(
            "cinema", "teatro", "show", "festival", "ingresso", "jogo", "game",
            "esporte", "academia", "futebol", "tênis de mesa", "natação", "natacao",
            "hobby", "bicicleta", "bike", "skate", "surf", "trilha", "viagem",
            "passeio", "parque", "diversão", "diversao", "brinquedo", "livro de lazer"
        ),
        SpendingCategory.CASA to listOf(
            "móvel", "movel", "sofá", "sofa", "cama", "colchão", "colchao",
            "geladeira", "fogão", "fogao", "micro-ondas", "microondas", "televisão",
            "televisao", "tv", "cortina", "tapete", "luminária", "luminaria",
            "lâmpada", "lampada", "ferramenta", "parafuso", "tinta", "pintura",
            "decoração", "decoracao", "armário", "armario", "prateleira",
            "aluguel", "condomínio", "condominio", "luz", "água", "agua", "internet"
        ),
        SpendingCategory.ASSINATURAS to listOf(
            "netflix", "spotify", "amazon prime", "disney", "hbo", "globoplay",
            "youtube premium", "apple music", "deezer", "crunchyroll", "academia",
            "plano", "mensalidade", "assinatura", "renovação", "renovacao",
            "antivírus", "antivirus", "office", "adobe", "cloud"
        ),
        SpendingCategory.EDUCACAO to listOf(
            "curso", "aula", "escola", "faculdade", "universidade", "livro",
            "material escolar", "caderno", "caneta", "mochila escolar",
            "apostila", "certificação", "certificacao", "udemy", "coursera",
            "alura", "treinamento", "workshop", "palestra", "seminário", "seminario"
        )
    )

    // AQUI: Tornamos a função PÚBLICA para o ImportScreen usar
    fun inferCategory(item: String, justification: String): SpendingCategory {
        val combined = "$item $justification".lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { combined.contains(it) }) return category
        }
        return SpendingCategory.OUTROS
    }

    fun analyze(item: String, price: Double, justification: String): InterventionResult {
        val combinedBase = "$item $justification".lowercase()

        val impulseScore    = impulseKeywords.count { combinedBase.contains(it) }
        val necessityScore  = necessityKeywords.count { combinedBase.contains(it) }

        val highPrice     = price > 300.0
        val veryHighPrice = price > 1000.0

        val justificationTrimmed = justification.trim()
        val combined = "$item $justificationTrimmed".lowercase()
        val lowPrice = price <= 30.0

        val hasEmotionalWords = listOf(
            "bonita", "bonito", "lindo", "linda", "achei bonito", "achei bonita",
            "gostei", "adorei", "quero muito", "tô afim", "to afim", "parece bom",
            "tava olhando", "vi na vitrine", "vi no insta", "vi no instagram",
            "todo mundo usa", "ficou na minha cabeça", "não resisti",
            "aproveitei a promoção", "tava barato demais"
        ).any { combined.contains(it) }

        val hasWeakJustification = justificationTrimmed.length < 10
                || justificationTrimmed.split(" ").size < 3

        val allowed = when {
            necessityScore >= 2                                              -> true
            necessityScore >= 1 && impulseScore == 0 && !hasEmotionalWords  -> true
            lowPrice && necessityScore >= 1                                  -> true
            lowPrice && impulseScore == 0 && !hasEmotionalWords             -> true
            impulseScore >= 1                                                -> false
            hasEmotionalWords                                                -> false
            veryHighPrice && necessityScore == 0                             -> false
            highPrice && necessityScore == 0                                 -> false
            hasWeakJustification && !lowPrice                                -> false
            else                                                             -> false
        }

        val coolingOff = if (!allowed) when {
            veryHighPrice -> 168
            highPrice     -> 48
            price > 100   -> 24
            else          -> 24
        } else 0

        val message = if (allowed) {
            "[Offline] Sua justificativa parece razoável. Conecte-se à internet para uma análise mais precisa pelo Guardião."
        } else {
            "[Offline] A justificativa sugere uma compra por impulso. Aguarde ${coolingOff}h antes de decidir. Conecte-se para uma análise completa."
        }

        return InterventionResult(
            allowed        = allowed,
            message        = message,
            coolingOffTime = coolingOff,
            category       = inferCategory(item, justification).name
        )
    }
}