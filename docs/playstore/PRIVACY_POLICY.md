# Política de Privacidade — SpendGuard

**Última atualização:** maio de 2026

---

## 1. Introdução

O SpendGuard ("nós", "nosso", "app") é desenvolvido por João Lucas Rodrigues. Esta Política de Privacidade descreve como coletamos, usamos e protegemos suas informações ao utilizar o aplicativo SpendGuard para Android.

Ao usar o SpendGuard, você concorda com as práticas descritas nesta política.

---

## 2. Dados que coletamos

### 2.1 Dados fornecidos por você
- **E-mail e nome** — para criação e autenticação da conta
- **Dados de compras analisadas** — nome do item, valor, justificativa e resultado da análise
- **Intenção financeira** — texto opcional que você escreve sobre seus objetivos
- **Estado emocional** — seleção opcional antes de cada análise (não é armazenado no servidor)

### 2.2 Dados gerados automaticamente
- **Histórico de análises** — armazenado localmente no dispositivo e sincronizado com nossa infraestrutura segura
- **Configurações do app** — preferências, plano ativo, conquistas
- **Modelo de IA offline** — padrões aprendidos das suas análises, armazenados **exclusivamente no dispositivo**

### 2.3 Dados que NÃO coletamos
- Dados bancários, senhas ou informações de cartão
- Localização
- Contatos ou lista de chamadas
- Fotos ou arquivos pessoais
- Dados de outros aplicativos (exceto texto de notificações de compra, se você habilitar a detecção automática)

---

## 3. Como usamos seus dados

| Dado | Finalidade |
|---|---|
| E-mail / nome | Autenticação e identificação da conta |
| Histórico de compras | Exibir histórico, gerar insights e sincronizar entre dispositivos |
| Dados de análises | Enviar ao Gemini AI (Google) para análise — veja seção 4 |
| Modelo de IA offline | Análises locais sem internet — nunca sai do dispositivo |
| Notificações de compra | Detectar compras em andamento (apenas se você ativar) |

---

## 4. Compartilhamento com terceiros

### Google Gemini AI
Quando você realiza uma análise **com internet**, enviamos ao Google Gemini:
- Nome do item, valor e justificativa
- Estado emocional selecionado
- Contexto do seu perfil financeiro (faixa de renda e objetivo, se configurados)

Não enviamos seu e-mail, nome ou qualquer identificador pessoal ao Gemini. Consulte a [Política de Privacidade do Google](https://policies.google.com/privacy) para detalhes sobre o tratamento de dados pelo Gemini.

### Supabase
Utilizamos o Supabase para autenticação e armazenamento seguro do histórico de análises. Os dados são armazenados em servidores com criptografia em repouso e em trânsito. Consulte a [Política de Privacidade do Supabase](https://supabase.com/privacy).

### Google Play Billing
Pagamentos pelo plano Pro são processados exclusivamente pelo Google Play. Não temos acesso aos seus dados de pagamento.

### Nenhum outro compartilhamento
Não vendemos, alugamos ou compartilhamos seus dados com terceiros para fins publicitários.

---

## 5. Armazenamento e segurança

- Dados sensíveis no dispositivo (status Pro, conquistas, streak) são armazenados com **EncryptedSharedPreferences (AES-256-GCM)**
- A exportação de planilha requer **autenticação biométrica**
- A comunicação com o Supabase usa **TLS 1.3**
- O modelo de IA offline **nunca é enviado a nenhum servidor**

---

## 6. Seus direitos

Você pode, a qualquer momento:
- **Acessar** seus dados através do histórico no app
- **Exportar** seus dados via a função de exportação de planilha
- **Excluir** sua conta e todos os dados associados (Ajustes → Dados → Excluir conta)
- **Desativar** a detecção automática de notificações nas configurações do Android

---

## 7. Retenção de dados

- Dados da conta: mantidos enquanto a conta estiver ativa
- Após exclusão da conta: removidos em até 30 dias dos nossos servidores
- Dados locais no dispositivo: removidos ao desinstalar o app

---

## 8. Crianças

O SpendGuard não é destinado a menores de 18 anos. Não coletamos intencionalmente dados de crianças. Se você acredita que uma criança nos forneceu dados, entre em contato para que possamos removê-los.

---

## 9. Alterações nesta política

Avisaremos sobre mudanças significativas por meio de uma notificação no app. A data de "última atualização" no topo deste documento reflete a versão mais recente.

---

## 10. Contato

Para dúvidas, solicitações ou exercício dos seus direitos:

**E-mail:** privacidade@spendguard.app  
**Desenvolvedor:** João Lucas Rodrigues

---

*Esta política está disponível em português (Brasil) e é o idioma oficial para todos os fins legais.*
