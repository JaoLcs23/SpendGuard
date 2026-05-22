package com.joaolucas.spendguard

class DebugNotificationHelper(private val context: android.content.Context) {

    private val ignoreKeywords = listOf(
        "nota fiscal", "nf-e", "danfe", "chave de acesso", "xml da nota",
        "nota fiscal eletrônica", "segunda via", "nota fiscal disponível",
        "nfe disponível", "acesse sua nota",
        "oferta especial", "aproveite", "não perca", "exclusivo para você",
        "cupom de desconto", "promoção relâmpago", "% de desconto", "% off",
        "últimas unidades", "ver oferta", "ver promoção", "confira as ofertas",
        "você tem um cupom", "resgate seu cupom", "use o cupom",
        "melhor preço", "preço baixou",
        "seu pedido foi entregue", "entregue com sucesso", "retirar na agência",
        "saiu para entrega", "objeto entregue", "em rota de entrega",
        "previsão de entrega", "objeto postado",
        "avalie sua compra", "como foi sua experiência", "deixe sua avaliação",
        "avalie o produto", "sua opinião",
        "acesso à sua conta", "redefinir senha", "código de verificação",
        "bem-vindo", "cadastro realizado"
    )

    private val pixSentKeywords = listOf(
        "pix enviado", "pix realizado", "pix efetuado",
        "transferência pix", "transferencia pix",
        "você enviou", "voce enviou",
        "pagamento via pix", "pagamento pix realizado",
        "pix concluído", "pix concluido",
        "transferência realizada", "transferencia realizada",
        "você transferiu", "voce transferiu",
        "pix aprovado", "envio de pix"
    )

    /**
     * Dispara uma notificação de teste passando pelo pipeline completo:
     * 1. Verifica ignoreKeywords
     * 2. Detecta se é PIX ou compra
     * 3. Chama Gemini para extrair itemName + price
     * 4. Verifica deduplicação de 12h no Room
     * 5. Exibe (ou silencia) a notificação do SpendGuard
     *
     * @param rawText   Texto bruto da notificação simulada
     * @param isPurchaseScenario  Se true, trata como notificação de compra; se false, como candidata a ser ignorada
     */
    suspend fun fireTestNotification(rawText: String, isPurchaseScenario: Boolean) {
        val fullText = rawText.lowercase()

        if (ignoreKeywords.any { fullText.contains(it) }) {
            android.util.Log.d("DebugNotif", "BLOQUEADO por ignoreKeywords: $rawText")
            return
        }

        val isPixSent = pixSentKeywords.any { fullText.contains(it) }

        val geminiService = GeminiService(BuildConfig.GEMINI_API_KEY)

        if (isPixSent) {
            val pixInfo = geminiService.extractPixInfo(rawText)
            showDebugPixNotification(
                bank      = "Teste (Debug)",
                recipient = pixInfo?.recipient ?: "Destinatário de Teste",
                amount    = pixInfo?.amount ?: 0.0,
                rawText   = rawText
            )
        } else {
            val purchaseInfo = geminiService.extractPurchaseInfo(rawText)
            val itemName = purchaseInfo?.itemName ?: "Item de Teste"
            val price    = purchaseInfo?.price ?: 0.0

            if (itemName != "Item de Teste") {
                val since = System.currentTimeMillis() - 48L * 60 * 60 * 1000
                val db = SpendGuardDatabase.getDatabase(context)
                val existing = db.purchaseDao().findSimilarRecentPurchase(itemName, since)
                if (existing != null) {
                    android.util.Log.d("DebugNotif", "BLOQUEADO por dedup 12h: '$itemName' já está no histórico")
                    return
                }
            }

            showDebugShoppingNotification(
                store    = "Mercado Livre (Teste)",
                itemName = itemName,
                price    = price
            )
        }
    }

    private fun showDebugShoppingNotification(store: String, itemName: String, price: Double) {
        val nm        = context.getSystemService(android.app.NotificationManager::class.java) ?: return
        val notifId   = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val priceText = if (price > 0) "R$ ${"%.2f".format(price)}" else "Valor não identificado"

        createChannel(nm, ShoppingNotificationListener.CHANNEL_ID, "Reflexão de Compras")

        val analyzeIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_item_name", itemName)
            putExtra("auto_item_price", price)
            putExtra("auto_store", store)
            putExtra("open_guardian", true)
        }
        val analyzePi = android.app.PendingIntent.getActivity(
            context, notifId, analyzeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val ignorePi = android.app.PendingIntent.getBroadcast(
            context, notifId + 1,
            android.content.Intent(ShoppingNotificationListener.ACTION_IGNORE)
                .setPackage(context.packageName)
                .putExtra(ShoppingNotificationListener.EXTRA_NOTIF_ID, notifId),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(notifId, androidx.core.app.NotificationCompat.Builder(context, ShoppingNotificationListener.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🛡[DEBUG] Compra — $store")
            .setContentText("$itemName · $priceText")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("Você acabou de comprar:\n$itemName\n$priceText\n\nEssa compra foi planejada ou foi por impulso? Analise agora com o Guardião.")
                .setSummaryText("$store [DEBUG]"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(analyzePi)
            .addAction(android.R.drawable.ic_menu_search, "Analisar", analyzePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignorar", ignorePi)
            .build()
        )
    }

    private fun showDebugPixNotification(bank: String, recipient: String, amount: Double, rawText: String) {
        val nm         = context.getSystemService(android.app.NotificationManager::class.java) ?: return
        val notifId    = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val amountText = if (amount > 0) "R$ ${"%.2f".format(amount)}" else "Valor não identificado"

        createChannel(nm, ShoppingNotificationListener.CHANNEL_ID_PIX, "Reflexão de PIX")

        val analyzeIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_item_name", "PIX para $recipient")
            putExtra("auto_item_price", amount)
            putExtra("auto_store", bank)
            putExtra("open_guardian", true)
            putExtra("is_pix", true)
            putExtra("pix_raw_text", rawText)
        }
        val analyzePi = android.app.PendingIntent.getActivity(
            context, notifId, analyzeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val ignorePi = android.app.PendingIntent.getBroadcast(
            context, notifId + 1,
            android.content.Intent(ShoppingNotificationListener.ACTION_IGNORE)
                .setPackage(context.packageName)
                .putExtra(ShoppingNotificationListener.EXTRA_NOTIF_ID, notifId),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(notifId, androidx.core.app.NotificationCompat.Builder(context, ShoppingNotificationListener.CHANNEL_ID_PIX)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("[DEBUG] PIX — $bank")
            .setContentText("Para: $recipient · $amountText")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("Você enviou um PIX:\nPara: $recipient\nValor: $amountText\n\nEsse pagamento foi necessário ou por impulso?")
                .setSummaryText("$bank [DEBUG]"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(analyzePi)
            .addAction(android.R.drawable.ic_menu_search, "Refletir", analyzePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignorar", ignorePi)
            .build()
        )
    }

    private fun createChannel(nm: android.app.NotificationManager, id: String, name: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(id, name, android.app.NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}
