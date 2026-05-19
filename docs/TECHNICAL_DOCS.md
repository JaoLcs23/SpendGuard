# Documentação Técnica — SpendGuard

## 1. Visão Geral da Arquitetura

O SpendGuard é um app Android nativo construído com Jetpack Compose, seguindo uma arquitetura de camadas simples sem ViewModel explícito — o estado é gerenciado diretamente nos Composables com `remember`/`rememberSaveable` e `StateFlow` nos managers.

```
┌─────────────────────────────────────────────┐
│                    UI Layer                  │
│   Compose Screens + MainActivity Navigation  │
├─────────────────────────────────────────────┤
│                 Domain Layer                 │
│   Managers (Pro, Streak, Goal, Intentions)   │
│   GeminiService / OfflineAnalyzer / Adaptive │
├─────────────────────────────────────────────┤
│                  Data Layer                  │
│   UserRepository (Supabase)                  │
│   PurchaseHistory DAO (Room/SQLite)          │
│   EncryptedSharedPreferences                 │
└─────────────────────────────────────────────┘
```

---

## 2. Módulos Principais

### 2.1 GeminiService

Responsável por todas as chamadas à API do Gemini AI.

**Métodos públicos:**
- `analyzeImpulse(item, price, justification, profile, emotionalState)` — análise principal
- `generateWeeklyInsight(purchases, profile)` — insight semanal (Pro)
- `extractPurchaseInfo(notificationText)` — extração de dados de notificações
- `extractPixInfo(notificationText)` — extração de dados de PIX

**Comportamento:**
- Timeout de 20 segundos por chamada
- Retry com backoff exponencial (até 2 tentativas, delay 1s e 2s)
- Sanitização de inputs antes de enviar ao modelo
- Parsing robusto de JSON com regex `\\{.*\\}`

**Prompt de análise inclui:**
- Perfil financeiro do usuário (renda, objetivos)
- Contexto de horas de trabalho equivalentes ao preço
- Estado emocional atual (se informado)
- Intenção financeira salva

---

### 2.2 Sistema de IA Offline

#### OfflineAnalyzer (estático)
Análise baseada em listas de palavras-chave fixas. Fallback quando não há dados do `AdaptiveModel`.

**Regras de decisão (em ordem de prioridade):**
1. Score de necessidade ≥ 2 → aprovada
2. Score de necessidade ≥ 1 + sem impulso + sem emocionais → aprovada
3. Preço ≤ R$30 + necessidade ≥ 1 → aprovada
4. Preço ≤ R$30 + sem impulso + sem emocionais → aprovada
5. Score de impulso ≥ 1 → bloqueada
6. Palavras emocionais detectadas → bloqueada
7. Preço alto + sem necessidade → bloqueada
8. Justificativa fraca (< 10 chars ou < 3 palavras) e preço > R$30 → bloqueada
9. Default → bloqueada

#### AdaptiveModelTrainer
Aprende com cada decisão do Gemini quando o usuário está online.

**Processo de aprendizado:**
1. Extrai palavras com 4+ caracteres do item + justificativa
2. Para cada palavra, atualiza ou cria uma `LearnedRule` com peso (0,3 inicial, máx 1,0)
3. Atualiza taxas de bloqueio por categoria (média móvel com fator 0,2)
4. Atualiza preços médios de compras bloqueadas/aprovadas (média móvel 0,1)
5. Mantém máximo de 200 regras (prioriza as mais recorrentes)

#### AdaptiveOfflineModel
Usa as regras aprendidas para análise personalizada.

**Ativa após:** 10+ análises online
**Fallback:** `OfflineAnalyzer` se dados insuficientes
**Confiança:** alta (≥3 regras), média (1-2 regras), baixa (0 regras)

---

### 2.3 ProManager

Controla o acesso a funcionalidades pagas.

**Armazenamento:** EncryptedSharedPreferences com AES-256-GCM

**Métodos:**
- `canUseGuardian()` — 5 análises/semana no plano gratuito
- `canUseCalculator()` — 5 usos/semana no plano gratuito
- `canSaveToLibrary()` — sempre true (biblioteca gratuita)
- `canExport()` — apenas Pro
- `canUseNotifications()` — apenas Pro
- `activatePro(plan)` / `deactivatePro()`
- `checkTrialExpiry()` — verifica e expira trial automaticamente

**Contadores semanais:** baseados em `Calendar.WEEK_OF_YEAR` — resetam automaticamente na virada da semana.

---

### 2.4 ShoppingNotificationListener

Monitora notificações de apps de e-commerce e bancos.

**Apps monitorados:**
- Shopee, Shein, Mercado Livre, Amazon, AliExpress, Magalu
- Nubank, Inter, C6, Itaú, Bradesco, Santander, Caixa

**Deduplicação:**
- Por `sbn.key` (evita reprocessar a mesma notificação)
- Por hash de conteúdo com TTL de 30 segundos (evita duplicatas do Gmail)

**Extração de valor:**
- Chama `GeminiService.extractPurchaseInfo()` para extrair item e preço do texto da notificação
- Fallback: usa o texto bruto como nome do item

---

### 2.5 BillingManager

Integração com Google Play Billing 7.x.

**Produtos:**
- `spendguard_pro_monthly` — plano mensal
- `spendguard_pro_yearly` — plano anual

**Fluxo de compra:**
1. `launchBillingFlow()` → abre o checkout do Google Play
2. `onPurchasesUpdated()` → captura o resultado
3. `activateProLocally()` → ativa no `ProManager` + sincroniza com Supabase via `UserRepository.activatePro()`
4. `acknowledgePurchase()` → confirma a compra para o Google Play

**Verificação recomendada em produção:** Edge Function no Supabase que valida o `purchaseToken` na API do Google Play antes de ativar no banco.

---

### 2.6 Segurança

| Dado | Armazenamento | Criptografia |
|---|---|---|
| Status Pro | EncryptedSharedPreferences | AES-256-GCM |
| Conquistas | EncryptedSharedPreferences | AES-256-GCM |
| Perfil financeiro | EncryptedSharedPreferences | AES-256-GCM |
| Streak | EncryptedSharedPreferences | AES-256-GCM |
| Histórico de compras | Room (SQLite) | Não criptografado |
| Token de sessão | Supabase Auth (gerenciado pelo SDK) | TLS |
| Export CSV | BiometricPrompt antes de gerar | — |

---

## 3. Banco de Dados Local (Room)

### Entidade: PurchaseEntity

| Campo | Tipo | Descrição |
|---|---|---|
| id | Int (PK, autoincrement) | ID local |
| userId | String | ID do usuário no Supabase |
| itemName | String | Nome do item analisado |
| price | Double | Valor em reais |
| justification | String | Justificativa fornecida |
| wasBlocked | Boolean | Resultado da análise |
| aiMessage | String | Veredicto completo da IA |
| coolingOffTime | Int | Tempo de reflexão em horas |
| category | String | Categoria inferida pela IA |
| timestamp | Long | Unix timestamp da análise |
| isImported | Boolean | Se veio de importação CSV |

---

## 4. Integração Supabase

### Tabelas

**users**
```sql
id          uuid    PRIMARY KEY (auth.uid())
email       text    NOT NULL
name        text
plan_type   text    DEFAULT 'free'  -- 'free' | 'monthly' | 'yearly'
plan_expiry timestamp
created_at  timestamp DEFAULT now()
```

**referrals**
```sql
id          uuid    PRIMARY KEY DEFAULT gen_random_uuid()
referrer_id uuid    REFERENCES users(id)
referred_id uuid    REFERENCES users(id)
code        text    NOT NULL UNIQUE
reward_given boolean DEFAULT false  -- controlado por trigger, nunca pelo cliente
created_at  timestamp DEFAULT now()
```

### Row Level Security (RLS) obrigatória

```sql
-- Usuário só acessa os próprios dados
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_own" ON users
  USING (auth.uid() = id);

-- reward_given nunca pode ser true pelo cliente
CREATE POLICY "referrals_no_reward" ON referrals
  WITH CHECK (reward_given = false);
```

---

## 5. Widget

O widget usa `RemoteViews` e segue as restrições do Android 12+:
- Background: `layer-list` com `<shape>` interno (shape direto no root causa rejeição)
- `android:clipToOutline="true"` no root
- Nenhuma `<View>` pura — usar `<TextView>` como divisor
- Tamanho fixo 4×2 via `resizeMode="none"`
- Dados via `getAllPurchasesList()` (suspend, não Flow)

---

## 6. Fluxo de Cooling-Off

1. Gemini retorna `coolingOffTime` em **horas** (24, 48, 72 ou 168)
2. `SimulatorScreen` agenda um `CoolingOffWorker` via WorkManager com `setInitialDelay(hours, TimeUnit.HOURS)`
3. Após o delay, o worker dispara uma notificação com botão "Analisar novamente"
4. O botão envia um Intent para `MainActivity` com `auto_item_name` nos extras
5. `MainActivity` captura em `onNewIntent` e navega direto para o Guardião pré-preenchido
