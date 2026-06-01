# SpendGuard

**Seu guardião financeiro pessoal — com Inteligência Artificial**

SpendGuard é um aplicativo Android que intercepta compras por impulso antes que o dinheiro saia da sua conta. Usando IA (Google Gemini), analisa cada justificativa de compra, detecta padrões emocionais e aplica períodos de reflexão baseados em valor. Com o tempo, aprende os padrões individuais de cada usuário para melhorar a análise mesmo sem internet.

---

## Funcionalidades

### Guardião Anti-Impulso
- Análise de compras via Gemini AI
- Check-in emocional antes de cada análise
- Período de reflexão de 24h a 1 semana conforme o valor
- Lembrete da intenção financeira do usuário
- Análise offline com IA adaptativa que aprende com o histórico

### IA Adaptativa Offline
- Aprende com cada decisão do Gemini quando online
- Análise personalizada baseada no perfil real do usuário
- Modelo salvo localmente — dados nunca saem do aparelho
- Ativa automaticamente após 10+ análises online

### Dashboard
- Gráfico de gastos dos últimos 6 meses
- Streak de dias sem impulso
- Meta mensal de economia com barra de progresso
- Insight semanal gerado pela IA (Pro)
- Alerta noturno entre 22h e 2h

### Histórico
- Filtros por status e período personalizado
- Cards expansíveis com veredito completo
- Exportação de planilha com autenticação biométrica (Pro)

### Widget 4×2
- Gastos, economia e bloqueios da semana
- Acesso direto ao Guardião por categoria

### Biblioteca Financeira
- Conteúdo curado gratuito para todos os usuários
- Livros, artigos e cursos de qualidade verificada

### Conquistas
- Sistema de badges por marcos de disciplina financeira

---

## Arquitetura

```
app/
├── GeminiService.kt          # Integração com Gemini AI + retry/backoff
├── OfflineAnalyzer.kt         # Análise offline com regras estáticas
├── AdaptiveModelTrainer.kt    # Aprende com decisões do Gemini
├── AdaptiveOfflineModel.kt    # Análise offline personalizada
├── UserRepository.kt          # Autenticação e dados do usuário (Supabase)
├── ProManager.kt              # Gerenciamento do plano Pro
├── StreakManager.kt           # Streak diário anti-impulso
├── GoalManager.kt             # Meta mensal de economia
├── IntentionsManager.kt       # Diário de intenções financeiras
├── WeeklyInsightManager.kt    # Cache do insight semanal
├── BillingManager.kt          # Google Play Billing
├── ShoppingNotificationListener.kt  # Detecção de compras em notificações
├── CoolingOffWorker.kt        # WorkManager para período de reflexão
├── SpendGuardWidget.kt        # Widget Android 4×2
└── ExportHelper.kt            # Export CSV com BiometricPrompt
```

### Stack tecnológica

| Camada | Tecnologia |
|---|---|
| UI | Jetpack Compose + Material 3 |
| IA online | Google Gemini (generativeai SDK) |
| IA offline | AdaptiveOfflineModel (on-device) |
| Backend | Supabase (Auth + PostgreSQL) |
| Banco local | Room (SQLite) |
| Billing | Google Play Billing 7.x |
| Segurança | EncryptedSharedPreferences + BiometricPrompt |
| Workers | WorkManager |
| Widget | AppWidgetProvider + RemoteViews |

---

## Planos

| Funcionalidade | Grátis | Pro |
|---|---|---|
| Análises no Guardião | 5/semana | Ilimitadas |
| Check-in emocional | ✓ | ✓ |
| Intenções financeiras | ✓ | ✓ |
| Streak anti-impulso | ✓ | ✓ |
| Meta mensal | ✓ | ✓ |
| Widget | ✓ | ✓ |
| Biblioteca financeira | Completa | Completa |
| Histórico | ✓ | ✓ |
| Exportar planilha | — | ✓ |
| Importar extrato | — | ✓ |
| Detecção automática | — | ✓ |
| Modo estrito | — | ✓ |
| Análise offline (IA adaptativa) | — | ✓ |
| Insight semanal | — | ✓ |
| Programa de indicação | — | 7 dias grátis |

---

## Setup de desenvolvimento

### Pré-requisitos
- Android Studio Hedgehog+
- JDK 17
- Conta Supabase
- Chave API Gemini (Google AI Studio)
- Conta Google Play Console (para billing)

### Configuração

1. Clone o repositório
2. Crie `local.properties` na raiz com:
```properties
GEMINI_API_KEY=sua_chave
SUPABASE_URL=https://seu-projeto.supabase.co
SUPABASE_ANON_KEY=sua_anon_key
KEYSTORE_PATH=../spendguard-release.jks
KEYSTORE_PASSWORD=sua_senha
KEY_ALIAS=spendguard-key
KEY_PASSWORD=sua_senha
```
3. Configure o Supabase conforme `docs/SUPABASE_SETUP.md`
4. Sync Gradle e execute no emulador ou dispositivo

---

## Testes automatizados

Veja `spendguard_agents/README.md` para o sistema de testes com CrewAI + Appium + Claude.

---

## Licença

Proprietário — todos os direitos reservados © 2026 João Lucas
