package com.joaolucas.spendguard

import java.text.SimpleDateFormat
import java.util.Locale

object CsvParser {

    data class ParsedTransaction(
        val itemName: String,
        val price: Double,
        val timestamp: Long,
        val rawLine: String = ""
    )

    data class ParseResult(
        val transactions: List<ParsedTransaction>,
        val format: String,
        val totalLines: Int,
        val skippedLines: Int,
        val errors: List<String>
    )

    private const val MAX_FILE_SIZE_CHARS = 5_000_000
    private const val MAX_LINES           = 50_000

    fun parse(csvContent: String): ParseResult {
        if (csvContent.length > MAX_FILE_SIZE_CHARS)
            return ParseResult(emptyList(), "Erro", 0, 0, listOf("Arquivo muito grande (máx. 5 MB)"))

        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParseResult(emptyList(), "Vazio", 0, 0, listOf("Arquivo vazio"))
        if (lines.size > MAX_LINES)
            return ParseResult(emptyList(), "Erro", lines.size, 0, listOf("Arquivo com muitas linhas (máx. $MAX_LINES)"))

        val sampleLines = lines.take(15)
        val sep = if (sampleLines.sumOf { it.count { c -> c == ';' } } > sampleLines.sumOf { it.count { c -> c == ',' } }) ';' else ','

        var headerIdx = -1
        var iDate = -1
        var iDesc = -1
        var iVal = -1

        for ((idx, line) in lines.withIndex()) {
            val cols = splitCsv(line, sep).map { it.lowercase() }

            val tDate = cols.indexOfFirst { it.matches(Regex(".*(data|date|dia|lançamento).*")) }
            val tVal = cols.indexOfFirst { it.matches(Regex(".*(valor|amount|saída|débito|compra).*")) }

            if (tDate != -1 && tVal != -1) {
                headerIdx = idx
                iDate = tDate
                iVal = tVal
                iDesc = cols.indexOfFirst { it.matches(Regex(".*(descri|hist|detalhe|estabelecimento|nome|título|title).*")) }
                if (iDesc == -1) iDesc = cols.indexOfFirst { it != cols[tDate] && it != cols[tVal] && it.isNotBlank() }
                if (iDesc == -1) iDesc = (iDate + 1) % cols.size
                break
            }
        }

        val dataLines = if (headerIdx != -1) lines.drop(headerIdx + 1) else lines

        if (headerIdx == -1) {
            iDate = 0
            iDesc = 1
            iVal = 2
        }

        val errors = mutableListOf<String>()
        val tempResults = mutableListOf<ParsedTransaction>()
        var skipped = 0

        for ((i, line) in dataLines.withIndex()) {
            try {
                val cols = splitCsv(line, sep)
                if (cols.size <= maxOf(iDate, iVal)) { skipped++; continue }

                val dateStr = cols.getOrNull(iDate)?.trim() ?: ""
                val descStr = (cols.getOrNull(iDesc)?.trim()?.ifBlank { "Transação" } ?: "Transação")
                    .take(200)
                val valStr = cols.getOrNull(iVal)?.trim() ?: ""

                val descLower = descStr.lowercase()
                val isLixoBancario = descLower.contains("saldo") ||
                        descLower.contains("histórico disponível") ||
                        descLower.contains("historico disponivel") ||
                        descLower.contains("saldos e investimentos") ||
                        descLower.contains("lançamentos futuros") ||
                        descLower.contains("lancamentos futuros") ||
                        descLower.contains("saldo anterior")

                if (isLixoBancario) {
                    skipped++
                    continue
                }

                val amountInfo = parseAmountRobust(valStr, line)
                if (amountInfo == null) { skipped++; continue }

                val ts = parseDateRobust(dateStr)
                if (ts == null) {
                    errors.add("Linha ${i + headerIdx + 2}: Data inválida '$dateStr'")
                    skipped++
                    continue
                }

                tempResults.add(ParsedTransaction(descStr, amountInfo, ts, line))
            } catch (e: Exception) {
                errors.add("Linha ${i + headerIdx + 2}: Erro ao ler")
                skipped++
            }
        }

        val hasNegatives = tempResults.any { it.price < 0 }
        val finalTransactions = mutableListOf<ParsedTransaction>()

        for (t in tempResults) {
            if (hasNegatives) {
                if (t.price < 0) {
                    finalTransactions.add(t.copy(price = Math.abs(t.price)))
                } else {
                    skipped++
                }
            } else {
                val lowerLine = t.rawLine.lowercase()
                val isIncome = lowerLine.contains("pagamento") || lowerLine.contains("estorno") || lowerLine.contains("recebido")
                if (!isIncome) {
                    finalTransactions.add(t)
                } else {
                    skipped++
                }
            }
        }

        return ParseResult(finalTransactions, "Leitor Inteligente", lines.size, skipped, errors)
    }

    private fun splitCsv(line: String, sep: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"'  -> inQuotes = !inQuotes
                ch == sep && !inQuotes -> { result.add(current.toString()); current.clear() }
                else       -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseAmountRobust(raw: String, fullLine: String): Double? {
        var s = raw.trim()
            .replace("R\\$\\s*".toRegex(), "")
            .replace("\\s".toRegex(), "")

        if (s.isEmpty() || s == "-") return null

        val isNegative = s.startsWith("-") || fullLine.lowercase().contains("débito") || fullLine.lowercase().contains("debito")
        s = s.replace("-", "")

        val hasComma = s.contains(',')
        val hasDot   = s.contains('.')

        s = when {
            hasComma && hasDot -> {
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) s.replace(".", "").replace(",", ".")
                else s.replace(",", "")
            }
            hasComma && !hasDot -> s.replace(",", ".")
            else -> s
        }

        val amount = s.toDoubleOrNull() ?: return null
        return if (isNegative) -amount else amount
    }

    private fun parseDateRobust(dateStr: String): Long? {
        val cleanDate = dateStr.take(10)
        val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd/MM/yy", "MM/dd/yyyy", "d/M/yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale("pt", "BR"))
                sdf.isLenient = false
                val date = sdf.parse(cleanDate) ?: continue
                return date.time
            } catch (_: Exception) {}
        }
        return null
    }
}