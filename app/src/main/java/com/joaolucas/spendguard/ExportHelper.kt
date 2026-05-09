package com.joaolucas.spendguard

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun canUseBiometric(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun exportWithBiometric(
        activity: FragmentActivity,
        purchases: List<PurchaseEntity>,
        onSuccess: (Uri) -> Unit,
        onFailure: () -> Unit
    ) {
        if (!canUseBiometric(activity)) {
            val uri = generateProfessionalCSV(activity, purchases)
            if (uri != null) onSuccess(uri) else onFailure()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val uri = generateProfessionalCSV(activity, purchases)
                if (uri != null) onSuccess(uri) else onFailure()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onFailure() }
            override fun onAuthenticationFailed() { onFailure() }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar exportação")
            .setSubtitle("Autentique-se para exportar seus dados financeiros")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }

    fun generateProfessionalCSV(context: Context, purchases: List<PurchaseEntity>): Uri? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val csvContent = buildString {
                append("\uFEFF")
                append("Data e Hora;Categoria;Item;Valor (R\$);Status;Justificativa;Tempo de Geladeira (min);IA Guardião\n")
                purchases.forEach { purchase ->
                    val dateStr      = dateFormat.format(Date(purchase.timestamp))
                    val categoryName = SpendingCategory.values()
                        .firstOrNull { it.name == purchase.category }?.label ?: purchase.category
                    val safeItem          = purchase.itemName.replace(";", ",").replace("\n", " ").take(200)
                    val safeJustification = purchase.justification.replace(";", ",").replace("\n", " ").take(300)
                    val safeAiMessage     = purchase.aiMessage.replace(";", ",").replace("\n", " ").take(500)
                    val priceStr          = String.format(Locale("pt", "BR"), "%.2f", purchase.price)
                    val statusStr         = if (purchase.wasBlocked) "Bloqueada" else "Liberada"
                    append("$dateStr;$categoryName;$safeItem;$priceStr;$statusStr;$safeJustification;${purchase.coolingOffTime};$safeAiMessage\n")
                }
            }
            val file = File(context.cacheDir, "Relatorio_SpendGuard.csv")
            FileOutputStream(file).use { it.write(csvContent.toByteArray(Charsets.UTF_8)) }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            Log.e("ExportHelper", "Falha ao gerar CSV", e)
            null
        }
    }
}
