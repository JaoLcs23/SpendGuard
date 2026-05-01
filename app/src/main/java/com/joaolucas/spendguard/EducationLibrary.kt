package com.joaolucas.spendguard

import io.github.jan.supabase.postgrest.Postgrest

class EducationRepository(private val postgrest: Postgrest) {
    suspend fun saveToLibrary(resource: EducationalResourceRemote): Boolean {
        val alreadySaved = postgrest.from("educational_resources")
            .select {
                filter {
                    eq("user_id", resource.userId)
                    eq("title", resource.title)
                }
            }
            .decodeList<EducationalResourceRemote>()
            .isNotEmpty()

        if (alreadySaved) return false

        postgrest.from("educational_resources").insert(resource)
        return true
    }

    suspend fun updateReadStatus(resourceId: String, isRead: Boolean) {
        postgrest.from("educational_resources")
            .update({ set("is_read", isRead) }) {
                filter {
                    eq("id", resourceId)
                }
            }
    }

    suspend fun getUserLibrary(userId: String): List<EducationalResourceRemote> {
        return postgrest.from("educational_resources")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<EducationalResourceRemote>()
    }

    suspend fun deleteFromLibrary(resourceId: String) {
        postgrest.from("educational_resources")
            .delete {
                filter {
                    eq("id", resourceId)
                }
            }
    }
}

enum class ResourceType { LIVRO, VIDEO, SITE, ARTIGO, CURSO }

data class EducationalResource(
    val title: String,
    val author: String,
    val type: ResourceType,
    val description: String,
    val link: String = ""
)

object EducationLibrary {

    val resources: List<EducationalResource> = listOf(

        EducationalResource(
            title       = "A Fazenda de FIIs",
            author      = "Jean Tosetto",
            type        = ResourceType.LIVRO,
            description = "Aprenda a plantar suas sementes no mercado imobiliário para colher aluguéis mensais."
        ),
        EducationalResource(
            title       = "Psicologia Financeira",
            author      = "Morgan Housel",
            type        = ResourceType.LIVRO,
            description = "Entenda como suas emoções ditam seus gastos — o complemento perfeito para o uso do Guardião."
        ),
        EducationalResource(
            title       = "O Homem Mais Rico da Babilônia",
            author      = "George S. Clason",
            type        = ResourceType.LIVRO,
            description = "A base da regra dos 10%: \"Uma parte de tudo que você ganha pertence a você\"."
        ),
        EducationalResource(
            title       = "Pai Rico, Pai Pobre",
            author      = "Robert Kiyosaki",
            type        = ResourceType.LIVRO,
            description = "A distinção clássica entre ativos que põem dinheiro no bolso e passivos que tiram."
        ),
        EducationalResource(
            title       = "Dinheiro Sem Medo",
            author      = "Eduardo Amuri",
            type        = ResourceType.LIVRO,
            description = "Com uma abordagem humana, foca na relação emocional que temos com o dinheiro. Propõe uma organização que não aprisiona, mas libera. Ideal para quem busca entender a educação financeira como uma ferramenta de bem-estar."
        ),
        EducationalResource(
            title       = "A Geometria da Riqueza",
            author      = "Brian Portnoy",
            type        = ResourceType.LIVRO,
            description = "Utiliza a psicologia para definir a riqueza como \"contentamento financiado\". Explica como alinhar propósito de vida, estratégia de investimento e táticas de mercado, tratando a gerência financeira como uma arquitetura pessoal."
        ),
        EducationalResource(
            title       = "Aposte em Você",
            author      = "Annie Duke",
            type        = ResourceType.LIVRO,
            description = "Este livro é uma aula sobre responsabilidade e tomada de decisão sob incerteza. É uma leitura essencial para entender que investir e gerir projetos é, essencialmente, lidar com probabilidades."
        ),
        EducationalResource(
            title       = "O Investidor Comportamental",
            author      = "Daniel Crosby",
            type        = ResourceType.LIVRO,
            description = "Explora as influências sociológicas, neurológicas e psicológicas que nos levam a cometer erros financeiros. Disseca como o ego e a emoção sabotam a gerência do patrimônio."
        ),
        EducationalResource(
            title       = "Build: Um Guia Ortodoxo para Criar Coisas que Valem a Pena",
            author      = "Tony Fadell",
            type        = ResourceType.LIVRO,
            description = "Fala como gerir a si mesmo, sua equipe e seu produto desde o protótipo até o lançamento. É um livro sobre o \"fazer\" com excelência e propósito."
        ),

        EducationalResource(
            title       = "Tesouro Direto Para Iniciantes",
            author      = "Me Poupe!",
            type        = ResourceType.VIDEO,
            description = "O guia definitivo para começar a investir com pouco dinheiro e segurança total.",
            link        = "https://youtu.be/y2sBkIX72-g?si=AN8K2HAonVVcB6Z9"
        ),
        EducationalResource(
            title       = "Luiz Barsi Ensina Como Usar a Renda Fixa",
            author      = "AGF",
            type        = ResourceType.VIDEO,
            description = "Descubra como construir uma carteira de ações que pagam dividendos mensais, com uma estratégia segura, prática e validada pelo maior investidor da Bolsa de Valores.",
            link        = "https://youtu.be/k7EBzeZbHdM?si=kTNPYAwfAGd-AaXA"
        ),

        EducationalResource(
            title       = "O Que São Fundos Imobiliários e Como Invesir em Fundos Imobiliários",
            author      = "Breno Perrucho - Jovens de Negócio",
            type        = ResourceType.VIDEO,
            description = "Saiba como funcionam os fundos imobiliários e o pagamento de aluguéis através de renda passiva.",
            link        = "https://youtu.be/vZ64S8dFpEM?si=c08mzmcUIRU4kTMl"
        ),

        EducationalResource(
            title       = "Como Analisar Ações de Maneira Simples e Rápida",
            author      = "Investidor Sardinha | Raul Sena",
            type        = ResourceType.VIDEO,
            description = "Aula sobre análise fundamentalista, avaliação de indicadores e outros pontos essenciais para quem quer investir em ações com mais segurança.",
            link        = "https://youtu.be/bkcMlHEtXsI?si=ndOeRXaEBNtTD5PG"
        ),
        EducationalResource(
            title       = "O Vídeo Que Eu Queria Ter Visto Quando Era Pobre",
            author      = "Investidor Sardinha | Raul Sena",
            type        = ResourceType.VIDEO,
            description = "Se o seu objetivo é ter liberdade financeira, construir patrimônio, viver de renda e parar de se sentir preso às contas do mês, esse conteúdo vai abrir sua visão.",
            link        = "https://youtu.be/Kj3zr_zzMrY?si=TmGLKYT723fUH1EI"
        ),
        EducationalResource(
            title       = "Classe Média Deve Fazer Compra Mensal Ou Semanal No Mercado?",
            author      = "Investidor Sardinha | Raul Sena",
            type        = ResourceType.VIDEO,
            description = "Comparação em detalhes das vantagens e desvantagens da compra mensal e da compra semanal de mercado.",
            link        = "https://youtu.be/XL3GUWzHKlM?si=bK4YILkq4aEGpgTH"
        ),
        EducationalResource(
            title       = "Dicas Financeiras Que Mudarão Sua Mente!",
            author      = "Investidor Sardinha | Raul Sena",
            type        = ResourceType.VIDEO,
            description = "Aula de como organizar as finanças pessoais, evitando erros comuns que levam ao endividamento e à falta de controle sobre seu dinheiro.",
            link        = "https://youtu.be/L77tVt9aqTA?si=zaL0BbToKpFBTlSD"
        ),
        EducationalResource(
            title       = "5 Conselhos Brutalmente Honestos Para Você Que Ainda é Jovem",
            author      = "Investidor Sardinha | Raul Sena",
            type        = ResourceType.VIDEO,
            description = "Você vai entender que nunca gastar mais do que ganha, definir limites claros para seu custo de vida e saber a diferença entre necessidade e status podem ser os grandes divisores de águas pra sua vida financeira.",
            link        = "https://youtu.be/5LhQ-xAXcNM?si=-ulNr5A8vRBQWIdx"
        ),

        EducationalResource(
            title       = "A Mágica Dos R$ 10 Mil Funciona?",
            author      = "Geração Dividendos",
            type        = ResourceType.VIDEO,
            description = "O conteúdo desmistifica a ideia de que o esforço inicial é em vão, demonstrando, através de simulações, como a consistência e a escolha do ativo influenciam o tempo necessário para a independência financeira.",
            link        = "https://youtu.be/vvgPEjkbbxc?si=lH3pjepwDQOlHR_e"
        ),

        EducationalResource(
            title       = "Qual Carro Você Deveria Comprar Com o Seu salário?",
            author      = "O Primo Rico",
            type        = ResourceType.VIDEO,
            description = "Análise real sobre a compra de um carro, é realmente necessário ou um sonho que pode ser adiado por um tempo?",
            link        = "https://youtu.be/4i3tFIJxNxY?si=updT0muxQdWthI9Y"
        ),
        EducationalResource(
            title       = "Como Mudar De Vida Em 30 Dias",
            author      = "O Primo Rico",
            type        = ResourceType.VIDEO,
            description = "Passos práticos e cientificamente comprovados para transformar a vida em 30 dias, focando em três pilares fundamentais: saúde física, saúde emocional e saúde financeira.",
            link        = "https://youtu.be/djDoxAgMWoo?si=qYbHdH6uFbWODh02"
        ),
        EducationalResource(
            title       = "7 Dicas Para Quem Ganha Pouco Economizar Dinheiro",
            author      = "O Primo Rico",
            type        = ResourceType.VIDEO,
            description = "Dicas que mostram que a economia é uma ferramenta poderosa de gestão, capaz de gerar resultados equivalentes a uma rentabilidade alta.",
            link        = "https://youtu.be/nX5nPjcVP_I?si=SDCk53hDx1yoplBO"
        ),
        EducationalResource(
            title       = "Comprar Imóvel Ou Morar De Aluguel: Qual Vale Mais a Pena?",
            author      = "O Primo Rico",
            type        = ResourceType.VIDEO,
            description = "Descubra por meio de análise técnica baseada em números e variáveis específicas o que compensa, comprar ou alugar um imóvel.",
            link        = "https://youtu.be/_Qc9r8j7YAI?si=gpYoSsowjpW63ROw"
        ),

        EducationalResource(
            title       = "Isso Vai Mudar Sua Vida Em 6 Meses!",
            author      = "Primo Pobre",
            type        = ResourceType.VIDEO,
            description = "Como a sua mentalidade, sua vida e suas finanças vão mudar quando você juntar seus primeiros R\$5 mil.",
            link        = "https://youtu.be/XcxuZqiMbQY?si=WGWp9qJIRrHPV-zp"
        ),

        EducationalResource(
            title       = "Consórcio Explicado Com Bananas",
            author      = "O Primo Primata",
            type        = ResourceType.VIDEO,
            description = "Descubra como funciona o sistema de consórcio explicado de forma simples e divertida.",
            link        = "https://youtu.be/_BPGsFST2J0?si=uXWSoogsKhUUsCJ7"
        ),
        EducationalResource(
            title       = "IOF Explicado Com Bananas",
            author      = "O Primo Primata",
            type        = ResourceType.VIDEO,
            description = "Explicação de que é e como funciona o IOF explicado de forma simples e divertida.",
            link        = "https://youtu.be/1_2GLxt4KMU?si=PW6FhDSk-JTgtpOy"
        ),
        EducationalResource(
            title       = "Comprar à Vista ou Parcelado Explicado Com Bananas",
            author      = "O Primo Primata",
            type        = ResourceType.VIDEO,
            description = "Entenda quando faz mais sentido pagar à vista ou parcelado.",
            link        = "https://youtu.be/QQ4hY6Iup_A?si=KXSSWbc-tjBRqto6"
        ),
        EducationalResource(
            title       = "Faça Isso Sempre Que For Pago",
            author      = "Mark Tilbury",
            type        = ResourceType.VIDEO,
            description = "Descubra como uma rotina eficaz toda vez que receber o seu salário pode fazer a diferença entre viver de salário em salário e começar a construir sua liberdade financeira.",
            link        = "https://youtu.be/sPm9pynCS0k?si=9s4Un9goZ7IqE4tC"
        ),

        EducationalResource(
            title       = "Status Invest",
            author      = "Status Invest",
            type        = ResourceType.SITE,
            description = "Ferramenta gratuita para consultar indicadores de FIIs e Ações.",
            link        = "https://statusinvest.com.br"
        ),
        EducationalResource(
            title       = "Calculadora do Cidadão",
            author      = "Banco Central",
            type        = ResourceType.SITE,
            description = "Ferramenta oficial para simular correção de valores e juros compostos.",
            link        = "https://www3.bcb.gov.br/CALCIDADAO/publico/exibirFormCorrecaoValores.do?method=exibirFormCorrecaoValores"
        ),


        EducationalResource(
            title       = "Gerenciando a Si Mesmo",
            author      = "Peter Drucker",
            type        = ResourceType.ARTIGO,
            description = "O guia definitivo sobre autogestão. Ensina a identificar seus pontos fortes e a assumir a responsabilidade sobre sua trajetória e decisões.",
            link        = "https://hbr.org/2005/01/managing-oneself"
        ),
        EducationalResource(
            title       = "A Tirania da Conveniência",
            author      = "Tim Wu",
            type        = ResourceType.ARTIGO,
            description = "Uma reflexão crítica sobre como a busca pela facilidade pode sabotar nossa autonomia e nos levar a escolhas financeiras impulsivas.",
            link        = "https://www.nytimes.com/2018/02/16/opinion/sunday/tyranny-convenience.html"
        ),
        EducationalResource(
            title       = "Os Vieses Comportamentais dos Indivíduos",
            author      = "CFA Institute",
            type        = ResourceType.ARTIGO,
            description = "Explora os erros sistemáticos que o cérebro humano comete ao lidar com dinheiro, ajudando a identificar e mitigar comportamentos autodestrutivos.",
            link        = "https://www.cfainstitute.org/insights/professional-learning/refresher-readings/2026/the-behavioral-biases-of-individuals"
        ),
        EducationalResource(
            title       = "Finanças Pessoais",
            author      = "EV.G (Gov.br)",
            type        = ResourceType.CURSO,
            description = "Curso gratuito com certificado sobre gestão de orçamento doméstico.",
            link        = "https://www.escolavirtual.gov.br/curso/66"
        ),
        EducationalResource(
            title       = "A Ciência do Bem-Estar",
            author      = "Yale University (via Coursera)",
            type        = ResourceType.CURSO,
            description = "Um estudo sobre os preconceitos cognitivos que nos fazem desejar coisas que não nos satisfazem, essencial para entender a psicologia por trás do consumo.",
            link        = "https://www.coursera.org/learn/the-science-of-well-being"
        ),

        EducationalResource(
            title       = "Mindshift: Rompendo Barreiras no Aprendizado",
            author      = "McMaster University (via Coursera)",
            type        = ResourceType.CURSO,
            description = "Focado em autogerência e responsabilidade, este curso ensina como treinar o cérebro para se adaptar a novas carreiras e desafios de gestão pessoal.",
            link        = "https://www.coursera.org/learn/mindshift"
        ),
    )

    fun getRandom(): EducationalResource = resources.random()
}