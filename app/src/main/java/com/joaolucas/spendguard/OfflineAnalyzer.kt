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
        "sem condicao", "necessidade", "obrigação", "obrigacao"
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
        val combined = "$item $justification".lowercase()

        val impulseScore    = impulseKeywords.count { combined.contains(it) }
        val necessityScore  = necessityKeywords.count { combined.contains(it) }

        val highPrice     = price > 300.0
        val veryHighPrice = price > 1000.0

        val allowed = when {
            necessityScore >= 2                          -> true
            necessityScore >= 1 && impulseScore == 0     -> true
            impulseScore >= 2                            -> false
            impulseScore >= 1 && highPrice               -> false
            veryHighPrice && necessityScore == 0         -> false
            justification.trim().length < 10             -> false
            else                                         -> true
        }

        val coolingOff = if (!allowed) when {
            veryHighPrice -> 1440
            highPrice     -> 720
            else          -> 60
        } else 0

        val message = if (allowed) {
            "[Análise offline] Sua justificativa parece razoável. Mas se puder, conecte-se à internet para uma análise mais precisa."
        } else {
            "[Análise offline] A justificativa sugere uma compra por impulso. Aguarde ${coolingOff / 60}h antes de decidir. Reconnecte-se para uma análise completa."
        }

        return InterventionResult(
            allowed        = allowed,
            message        = message,
            coolingOffTime = coolingOff,
            category       = inferCategory(item, justification).name
        )
    }
}