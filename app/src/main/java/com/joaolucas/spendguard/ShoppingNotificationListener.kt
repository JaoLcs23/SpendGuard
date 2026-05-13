package com.joaolucas.spendguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShoppingNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notifCounter = java.util.concurrent.atomic.AtomicInteger(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    )

    private val processedKeys: MutableSet<String> = object : LinkedHashSet<String>(512) {
        private val maxSize = 500
        override fun add(element: String): Boolean {
            if (size >= maxSize) remove(iterator().next())
            return super.add(element)
        }
    }

    private val recentContentHashes = object : LinkedHashMap<Int, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Int, Long>) = size > 100
    }
    private val CONTENT_DEDUP_MS = 30_000L

    private val shoppingApps = mapOf(
        "com.google.android.gm"             to "Gmail",
        "com.microsoft.office.outlook"      to "Outlook",
        "com.shopee.br"                     to "Shopee",
        "com.shein.intl"                    to "Shein",
        "com.mercadolibre"                  to "Mercado Livre",
        "com.mercadopago.wallet"            to "Mercado Pago",
        "com.luizalabs.mlapp"              to "Magalu",
        "com.amazon.mShop.android.shopping" to "Amazon",
        "br.com.americanas.app"             to "Americanas",
        "com.b2w.submarino"                 to "Submarino",
        "com.kabum.app"                     to "KaBuM",
        "com.aliexpress.revolutionary"      to "AliExpress",
        "br.com.ifood.client"              to "iFood",
        "com.rappi.intl"                    to "Rappi",
        "com.ubercab.eats"                  to "Uber Eats",
        "com.netshoes.app"                  to "Netshoes",
        "com.dafiti.android"                to "Dafiti",
        "com.centauro.app"                  to "Centauro"
    )

    private val bankApps = mapOf(
        "com.nu.production"                         to "Nubank",
        "br.com.intermedium"                        to "Inter",
        "com.c6bank.app"                            to "C6 Bank",
        "com.itau"                                  to "Itaú",
        "br.com.bradesco"                           to "Bradesco",
        "com.santander.mobile"                      to "Santander",
        "br.com.bb.android"                         to "Banco do Brasil",
        "br.com.caixa.tem"                          to "Caixa Tem",
        "com.caixa.internet.banking"                to "Caixa",
        "br.com.btgpactual.btgpactualdigital"       to "BTG Pactual",
        "br.com.original.bank"                      to "Banco Original",
        "br.com.neon"                               to "Neon",
        "br.com.bancointer"                         to "Inter",
        "com.picpay"                                to "PicPay",
        "br.com.recargapay.app"                     to "RecargaPay",
        "com.pagbank"                               to "PagBank",
        "br.com.sicoob.sicoobnet.mobile"            to "Sicoob",
        "br.com.sicredi.app"                        to "Sicredi",
        "br.gov.caixa.caixatem"                     to "Caixa Tem"
    )

    private val purchaseKeywords = listOf(
        "pedido confirmado", "compra confirmada", "pedido realizado",
        "pagamento confirmado", "pedido aprovado", "compra efetuada",
        "seu pedido", "obrigado pela compra", "compra realizada",
        "pedido recebido", "você comprou", "sua compra",
        "pagamento aprovado", "fatura aprovada", "pedido #",
        "order confirmed", "purchase confirmed", "thank you for your purchase",
        "your order", "payment confirmed"
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

    private val pixReceivedKeywords = listOf(
        "pix recebido", "você recebeu", "voce recebeu",
        "recebeu um pix", "depósito recebido", "deposito recebido",
        "transferência recebida", "transferencia recebida",
        "entrada de", "crédito de", "credito de"
    )

    companion object {
        const val CHANNEL_ID      = "shopping_reflection"
        const val CHANNEL_ID_PIX  = "pix_reflection"
        const val ACTION_IGNORE   = "com.joaolucas.spendguard.ACTION_IGNORE"
        private const val MAX_NOTIFICATION_TEXT_LENGTH = 500
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { processedKeys.add(it.key) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        processedKeys.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (processedKeys.contains(sbn.key)) return
        processedKeys.add(sbn.key)

        val packageName = sbn.packageName
        val proManager  = ProManager(applicationContext)
        if (!proManager.canUseNotifications()) return

        val extras  = sbn.notification.extras
        val title   = extras.getCharSequence("android.title")?.toString() ?: ""
        val text    = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = "$title $text $bigText".lowercase()

        val rawText = "$title. $text. $bigText"
            .trim()
            .take(MAX_NOTIFICATION_TEXT_LENGTH)
            .replace(Regex("[\\x00-\\x1F\\x7F]"), " ")

        val contentHash = ("$title|$text").hashCode()
        val now = System.currentTimeMillis()
        synchronized(recentContentHashes) {
            val lastSeen = recentContentHashes[contentHash]
            if (lastSeen != null && now - lastSeen < CONTENT_DEDUP_MS) return
            recentContentHashes[contentHash] = now
        }

        val storeName = shoppingApps[packageName]
        if (storeName != null) {
            val isPurchase = purchaseKeywords.any { fullText.contains(it) }
            if (!isPurchase) return
            handleShoppingNotification(rawText, storeName)
            return
        }

        val bankName = bankApps[packageName]
        if (bankName != null) {
            val isReceived = pixReceivedKeywords.any { fullText.contains(it) }
            if (isReceived) return

            val isPixSent = pixSentKeywords.any { fullText.contains(it) }
            if (!isPixSent) return

            handlePixNotification(rawText, bankName)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        processedKeys.remove(sbn.key)
    }

    private fun handleShoppingNotification(rawText: String, storeName: String) {
        val geminiService = GeminiService(BuildConfig.GEMINI_API_KEY)
        serviceScope.launch {
            try {
                val purchaseInfo = geminiService.extractPurchaseInfo(rawText)
                showShoppingNotification(
                    store    = storeName,
                    itemName = purchaseInfo?.itemName ?: "Compra detectada",
                    price    = purchaseInfo?.price ?: 0.0
                )
            } catch (_: Exception) { }
        }
    }

    private fun handlePixNotification(rawText: String, bankName: String) {
        val geminiService = GeminiService(BuildConfig.GEMINI_API_KEY)
        serviceScope.launch {
            try {
                val pixInfo = geminiService.extractPixInfo(rawText)
                showPixNotification(
                    bank      = bankName,
                    recipient = pixInfo?.recipient ?: "Destinatário não identificado",
                    amount    = pixInfo?.amount ?: 0.0,
                    rawText   = rawText
                )
            } catch (_: Exception) { }
        }
    }

    private fun showShoppingNotification(store: String, itemName: String, price: Double) {
        val nm        = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifId   = notifCounter.incrementAndGet()
        val priceText = if (price > 0) "R$ ${"%.2f".format(price)}" else "Valor não identificado"

        createShoppingChannel(nm)

        val analyzeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_item_name",  itemName)
            putExtra("auto_item_price", price)
            putExtra("auto_store",      store)
            putExtra("open_guardian",   true)
        }
        val analyzePi = PendingIntent.getActivity(
            this, notifId, analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ignorePi = PendingIntent.getBroadcast(
            this, notifId + 1,
            Intent(ACTION_IGNORE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(notifId, NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🛡Compra detectada — $store")
            .setContentText("$itemName · $priceText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Você acabou de comprar:\n$itemName\n$priceText\n\nEssa compra foi planejada ou foi por impulso? Analise agora com o Guardião.")
                .setSummaryText(store))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(analyzePi)
            .addAction(android.R.drawable.ic_menu_search, "Analisar", analyzePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignorar", ignorePi)
            .build()
        )
    }

    private fun showPixNotification(bank: String, recipient: String, amount: Double, rawText: String) {
        val nm         = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifId    = notifCounter.incrementAndGet()
        val amountText = if (amount > 0) "R$ ${"%.2f".format(amount)}" else "Valor não identificado"

        createPixChannel(nm)

        val analyzeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_item_name",  "PIX para $recipient")
            putExtra("auto_item_price", amount)
            putExtra("auto_store",      bank)
            putExtra("open_guardian",   true)
            putExtra("is_pix",          true)
            putExtra("pix_raw_text",    rawText)
        }
        val analyzePi = PendingIntent.getActivity(
            this, notifId, analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ignorePi = PendingIntent.getBroadcast(
            this, notifId + 1,
            Intent(ACTION_IGNORE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(notifId, NotificationCompat.Builder(this, CHANNEL_ID_PIX)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PIX enviado — $bank")
            .setContentText("Para: $recipient · $amountText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Você enviou um PIX:\nPara: $recipient\nValor: $amountText\n\nEsse pagamento foi necessário ou foi por impulso? Reflita com o Guardião.")
                .setSummaryText(bank))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(analyzePi)
            .addAction(android.R.drawable.ic_menu_search, "Refletir", analyzePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignorar", ignorePi)
            .build()
        )
    }

    private fun createShoppingChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reflexão de Compras", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "Alertas do SpendGuard para compras detectadas"
                        enableVibration(true)
                    }
            )
        }
    }

    private fun createPixChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_PIX, "Reflexão de PIX", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "Alertas do SpendGuard para PIX enviados"
                        enableVibration(true)
                    }
            )
        }
    }
}