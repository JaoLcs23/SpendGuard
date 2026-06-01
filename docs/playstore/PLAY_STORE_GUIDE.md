# Guia de Publicação — Google Play Store

## 1. Checklist pré-publicação

### Código e build
- [ ] `versionCode` incrementado no `build.gradle.kts`
- [ ] `versionName` atualizado (ex: "1.0.0")
- [ ] `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`
- [ ] ProGuard habilitado (`isMinifyEnabled = true`, `isShrinkResources = true`)
- [ ] Nenhuma senha ou chave hardcoded no código
- [ ] `BuildConfig.IS_DEBUG = false` no build de release
- [ ] APK/AAB assinado com keystore de produção
- [ ] `Log.d` e `Log.v` removidos pelo ProGuard (`assumenosideeffects`)

### Funcional
- [ ] Fluxo de onboarding completo e sem erros
- [ ] Login/cadastro com e-mail e Google funcionando
- [ ] Guardião analisa e bloqueia compras corretamente
- [ ] Cooling-off agenda notificação e abre corretamente
- [ ] Widget funciona em pelo menos 3 launchers diferentes
- [ ] Exportação de planilha com biometria
- [ ] Billing: compra Pro mensal e anual
- [ ] Billing: restore de compra após reinstalar
- [ ] Deep links funcionando (OAuth redirect)
- [ ] Permissão de notificações solicitada corretamente

### Privacidade
- [ ] Política de privacidade publicada em URL acessível
- [ ] Nenhum dado de usuário enviado sem consentimento
- [ ] `FLAG_SECURE` removido (ou mantido conscientemente)
- [ ] Formulário de segurança de dados preenchido no Console

---

## 2. Gerar AAB de release

No Android Studio:
```
Build → Generate Signed Bundle / APK → Android App Bundle
→ Keystore: spendguard-release.jks
→ Build Variant: release
```

Ou via linha de comando:
```bash
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```

---

## 3. Configuração no Google Play Console

### 3.1 Criar o app
1. Acesse [play.google.com/console](https://play.google.com/console)
2. **Criar app** → Preencha:
   - Nome: `SpendGuard — Guardião Financeiro`
   - Idioma padrão: Português (Brasil)
   - App ou jogo: App
   - Gratuito ou pago: Gratuito (compras no app)
   - Declarações: marque todas que se aplicam

### 3.2 Configuração do app (menu lateral)

#### Presença na Play Store

**Detalhes do app:**
```
Nome curto (30 chars):
SpendGuard

Nome completo (50 chars):
SpendGuard — Guardião Financeiro IA

Descrição curta (80 chars):
Pare compras por impulso com IA. Analise, reflita e economize mais todo dia.

Descrição longa (4000 chars):
[veja seção 4 abaixo]
```

**Ícone do app:** 512×512 px, PNG, sem transparência, sem cantos arredondados (o Play arredonda automaticamente)

**Gráfico de recursos (Feature graphic):** 1024×500 px, JPG ou PNG

**Capturas de tela obrigatórias:**
- Telefone: mínimo 2, máximo 8 (recomendado: 5–7)
- Tablet 7": opcional mas recomendado
- Formato: mínimo 320px no lado menor, máximo 3840px

#### Classificação do conteúdo
1. Preencha o questionário de classificação
2. Categoria sugerida: **Finanças**
3. Classificação esperada: **Livre** (sem conteúdo adulto)

#### Público-alvo
- Faixa etária: 18+
- Apelos a crianças: Não

#### Segurança de dados
Declare o que o app coleta:

| Dado | Coletado | Compartilhado | Obrigatório |
|---|---|---|---|
| E-mail | Sim | Não | Sim |
| Nome | Sim | Não | Não |
| Histórico de compras | Sim | Não | Sim |
| Dados financeiros (valores) | Sim | Não | Sim |
| Identificadores de dispositivo | Não | Não | — |

Finalidade principal: **Funcionalidade do app**
Criptografia em trânsito: **Sim**
Exclusão de dados: **Sim** (via configurações do app)

---

## 4. Textos da Play Store

### Descrição longa

```
🛡️ SPENDGUARD — SEU GUARDIÃO FINANCEIRO COM INTELIGÊNCIA ARTIFICIAL

Você já comprou algo por impulso e se arrependeu depois? O SpendGuard intercepta esse momento antes que aconteça.

━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 GUARDIÃO ANTI-IMPULSO COM IA
━━━━━━━━━━━━━━━━━━━━━━━━━
Descreva o que quer comprar e por que. O Guardião analisa sua justificativa com Gemini AI, detecta padrões emocionais e dá um veredito claro: compra consciente ou impulso disfarçado.

Antes de cada análise, o app pergunta como você está. Uma compra feita estressado ou animado demais recebe uma abordagem diferente — porque 40% das compras por impulso acontecem em estado emocional alterado.

━━━━━━━━━━━━━━━━━━━━━━━━━
🔥 STREAK E METAS
━━━━━━━━━━━━━━━━━━━━━━━━━
Construa uma sequência de dias sem compras por impulso. Defina uma meta mensal de economia e acompanhe o progresso em tempo real no dashboard.

━━━━━━━━━━━━━━━━━━━━━━━━━
📱 WIDGET INTELIGENTE
━━━━━━━━━━━━━━━━━━━━━━━━━
Veja seus gastos, economia e bloqueios da semana direto na tela inicial. Com um toque em qualquer categoria, o Guardião abre pronto para analisar.

━━━━━━━━━━━━━━━━━━━━━━━━━
🔔 DETECÇÃO AUTOMÁTICA (PRO)
━━━━━━━━━━━━━━━━━━━━━━━━━
O SpendGuard monitora notificações de Shopee, Shein, Mercado Livre, Amazon e outros. Quando detecta uma compra em andamento, abre o Guardião automaticamente antes que você finalize.

━━━━━━━━━━━━━━━━━━━━━━━━━
📶 FUNCIONA SEM INTERNET
━━━━━━━━━━━━━━━━━━━━━━━━━
A IA offline aprende com cada análise feita online. Com o tempo, fica cada vez mais precisa para o seu perfil — mesmo sem conexão.

━━━━━━━━━━━━━━━━━━━━━━━━━
📚 BIBLIOTECA FINANCEIRA GRATUITA
━━━━━━━━━━━━━━━━━━━━━━━━━
Livros, artigos e vídeos selecionados com critério. Sem limite de acesso — gratuito para todos os usuários.

━━━━━━━━━━━━━━━━━━━━━━━━━
PLANO GRATUITO INCLUI:
━━━━━━━━━━━━━━━━━━━━━━━━━
✓ 5 análises por semana no Guardião
✓ Streak, meta e intenções financeiras
✓ Widget 4×2 na tela inicial
✓ Biblioteca financeira completa
✓ Histórico de análises

PLANO PRO INCLUI TUDO + :
✓ Análises ilimitadas
✓ Detecção automática de compras
✓ Análise offline com IA adaptativa
✓ Insight semanal personalizado
✓ Exportar planilha financeira
✓ Importar extrato bancário
✓ Modo estrito anti-impulso

━━━━━━━━━━━━━━━━━━━━━━━━━
PRIVACIDADE
━━━━━━━━━━━━━━━━━━━━━━━━━
Seus dados financeiros ficam no seu dispositivo e no seu perfil seguro. Nunca vendemos dados. Nunca exibimos anúncios.

Comece grátis. Seus gastos impulsivos não.
```

### Notas de atualização (What's New) — v1.0.0
```
🎉 Lançamento do SpendGuard!

• Guardião anti-impulso com Gemini AI
• Check-in emocional antes de cada análise  
• IA offline que aprende com seu histórico
• Widget 4×2 com gastos e economia da semana
• Streak de dias sem impulso
• Meta mensal de economia
• Biblioteca financeira gratuita e completa
```

---

## 5. Configurar produtos no app (Billing)

No Google Play Console → **Monetização → Produtos no app**:

### Assinaturas

**Plano Mensal:**
- ID do produto: `spendguard_pro_monthly`
- Nome: SpendGuard Pro — Mensal
- Preço sugerido: R$ 14,90/mês
- Período de faturamento: Mensal
- Período de teste gratuito: 7 dias

**Plano Anual:**
- ID do produto: `spendguard_pro_yearly`
- Nome: SpendGuard Pro — Anual
- Preço sugerido: R$ 99,90/ano (~R$ 8,32/mês — 44% de desconto)
- Período de faturamento: Anual
- Período de teste gratuito: 7 dias

---

## 6. Lançamento faseado

Recomendado para a primeira publicação:

1. **Teste interno** (até 100 usuários) — valide o fluxo completo de billing
2. **Teste fechado / alfa** (usuários convidados) — colete feedback real
3. **Teste aberto / beta** — disponível para qualquer pessoa
4. **Produção — 10%** — monitore crashes e avaliações
5. **Produção — 50%** — se métricas OK, expanda
6. **Produção — 100%**

---

## 7. Métricas para monitorar após lançamento

- **Crash-free rate:** manter > 99,5%
- **ANR rate:** manter < 0,47% (limiar do Google)
- **Avaliação média:** objetivo > 4,3 ⭐
- **Retenção D1/D7/D30:** referência mercado ~40% / 20% / 10%
- **Conversão gratuito → Pro:** objetivo > 3%
