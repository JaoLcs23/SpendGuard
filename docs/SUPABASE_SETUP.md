# Configuração do Supabase — SpendGuard

## 1. Criar projeto

1. Acesse [supabase.com](https://supabase.com) e crie uma conta
2. Novo projeto → escolha região mais próxima (ex: South America - São Paulo)
3. Anote a **URL** e a **anon key** para o `local.properties`

---

## 2. Criar tabelas

Execute no **SQL Editor** do Supabase:

```sql
-- Tabela de usuários
CREATE TABLE users (
  id          uuid        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email       text        NOT NULL,
  name        text        DEFAULT '',
  plan_type   text        DEFAULT 'free' CHECK (plan_type IN ('free', 'monthly', 'yearly')),
  plan_expiry timestamptz,
  created_at  timestamptz DEFAULT now()
);

-- Tabela de referrals
CREATE TABLE referrals (
  id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  referrer_id  uuid        REFERENCES users(id) ON DELETE CASCADE,
  referred_id  uuid        REFERENCES users(id) ON DELETE CASCADE,
  code         text        NOT NULL UNIQUE,
  reward_given boolean     DEFAULT false,
  created_at   timestamptz DEFAULT now(),
  UNIQUE(referred_id)
);

-- Índices
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_referrals_code ON referrals(code);
CREATE INDEX idx_referrals_referrer ON referrals(referrer_id);
```

---

## 3. Row Level Security (RLS)

```sql
-- Habilitar RLS em todas as tabelas
ALTER TABLE users    ENABLE ROW LEVEL SECURITY;
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;

-- users: cada um vê e edita apenas os próprios dados
CREATE POLICY "users_select_own" ON users FOR SELECT USING (auth.uid() = id);
CREATE POLICY "users_update_own" ON users FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "users_insert_own" ON users FOR INSERT WITH CHECK (auth.uid() = id);

-- referrals: usuário vê os próprios referrals
CREATE POLICY "referrals_select" ON referrals FOR SELECT
  USING (auth.uid() = referrer_id OR auth.uid() = referred_id);

-- referrals: cliente nunca pode inserir com reward_given = true
CREATE POLICY "referrals_insert" ON referrals FOR INSERT
  WITH CHECK (reward_given = false);

-- reward_given só pode ser atualizado por service_role (backend)
CREATE POLICY "referrals_no_client_reward" ON referrals FOR UPDATE
  USING (auth.uid() = referrer_id)
  WITH CHECK (reward_given = false);
```

---

## 4. Trigger para criar perfil automaticamente

```sql
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.users (id, email, name)
  VALUES (
    NEW.id,
    NEW.email,
    COALESCE(NEW.raw_user_meta_data->>'name', '')
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();
```

---

## 5. Autenticação Google (OAuth)

1. Supabase Dashboard → **Authentication → Providers → Google**
2. Ative o provider Google
3. Acesse [Google Cloud Console](https://console.cloud.google.com)
4. Crie credenciais OAuth 2.0 para Android:
   - Tipo: Android
   - Package name: `com.joaolucas.spendguard`
   - SHA-1: obtenha com `./gradlew signingReport`
5. Copie o **Client ID** para o Supabase
6. Adicione o redirect URI no Google Cloud: `https://seu-projeto.supabase.co/auth/v1/callback`

---

## 6. Variáveis de ambiente

No `local.properties`:
```properties
SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 7. Edge Function para validar billing (recomendado)

Crie em `supabase/functions/validate-purchase/index.ts`:

```typescript
import { serve } from "https://deno.land/std@0.177.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const { purchaseToken, productId, userId } = await req.json()

  // Valida com Google Play Developer API
  const googleResponse = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.joaolucas.spendguard/purchases/subscriptions/${productId}/tokens/${purchaseToken}`,
    { headers: { Authorization: `Bearer ${Deno.env.get("GOOGLE_ACCESS_TOKEN")}` } }
  )

  if (!googleResponse.ok) {
    return new Response(JSON.stringify({ error: "Token inválido" }), { status: 400 })
  }

  const purchase = await googleResponse.json()

  if (purchase.paymentState === 1) { // pago
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    )

    const planType = productId.includes("yearly") ? "yearly" : "monthly"
    const expiry   = new Date(parseInt(purchase.expiryTimeMillis)).toISOString()

    await supabase.from("users").update({
      plan_type: planType,
      plan_expiry: expiry
    }).eq("id", userId)

    return new Response(JSON.stringify({ success: true, planType }), { status: 200 })
  }

  return new Response(JSON.stringify({ error: "Pagamento não confirmado" }), { status: 402 })
})
```
