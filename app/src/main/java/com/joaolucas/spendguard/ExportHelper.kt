package com.joaolucas.spendguard

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun generateProfessionalCSV(context: Context, purchases: List<PurchaseEntity>): Uri? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

            val csvContent = buildString {
                append("\uFEFF")

                append("Data e Hora;Categoria;Item;Valor (R$);Status;Justificativa;Tempo de Geladeira (min);IA Guardião\n")

                purchases.forEach { purchase ->
                    val dateStr = dateFormat.format(Date(purchase.timestamp))

                    val categoryName = ProfileSpendingCategory.values()
                        .firstOrNull { it.name == purchase.category }?.label ?: purchase.category

                    val safeItem = purchase.itemName.replace(";", ",").replace("\n", " ")
                    val safeJustification = purchase.justification.replace(";", ",").replace("\n", " ")
                    val safeAiMessage = purchase.aiMessage.replace(";", ",").replace("\n", " ")

                    val priceStr = String.format(Locale("pt", "BR"), "%.2f", purchase.price)

                    val statusStr = if (purchase.wasBlocked) "Bloqueada" else "Liberada"

                    append("$dateStr;$categoryName;$safeItem;$priceStr;$statusStr;$safeJustification;${purchase.coolingOffTime};$safeAiMessage\n")
                }
            }

            val file = File(context.cacheDir, "Relatorio_SpendGuard.csv")
            FileOutputStream(file).use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Falha ao gerar CSV", e)
            null
        }
    }
}