# Changelog — SpendGuard

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

---

## [1.0.0] — 2026-05

### Lançamento inicial

#### Funcionalidades
- Guardião anti-impulso com Gemini AI
- Check-in emocional antes de cada análise (4 estados)
- Intenções financeiras — diário de propósito
- IA offline adaptativa (AdaptiveModelTrainer + AdaptiveOfflineModel)
- Streak diário de dias sem impulso
- Meta mensal de economia com barra de progresso
- Dashboard com gráfico de gastos dos últimos 6 meses
- Alerta noturno entre 22h e 2h
- Insight semanal gerado pela IA (Pro)
- Histórico com filtro por período personalizado e cards expansíveis
- Exportação de planilha com autenticação biométrica (Pro)
- Importação de extrato bancário em CSV (Pro)
- Widget 4×2 com gastos, economia e bloqueios da semana
- Biblioteca financeira gratuita e completa
- Sistema de conquistas
- Detecção automática de compras em notificações (Pro)
- Modo estrito anti-impulso (Pro)
- Análise offline com IA adaptativa (Pro)
- Programa de indicação com 7 dias de trial (Pro)
- Planos mensais e anuais via Google Play Billing

#### Segurança
- EncryptedSharedPreferences para dados sensíveis
- BiometricPrompt antes de exportar planilha
- RLS no Supabase — usuário acessa apenas os próprios dados
- `rewardGiven` controlado apenas pelo backend
- ProGuard com remoção de logs de debug no release
- Modelo de IA offline armazenado exclusivamente no dispositivo

#### Correções técnicas aplicadas durante desenvolvimento
- Widget: `<View>` substituída por `<TextView>` (restrição RemoteViews)
- Widget: background migrado para `layer-list` (Android 12+)
- Widget: query `suspend` substituindo hack `Flow + delay + cancel`
- Notificações duplicadas: deduplicação por hash de conteúdo + TTL 30s
- Cooling-off: unidade corrigida de minutos para horas
- HistoryScreen: `@Composable` restaurado após remoção acidental
- FilterChip: `filterChipBorder` com parâmetros obrigatórios `enabled` e `selected`
