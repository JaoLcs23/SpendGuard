package com.joaolucas.spendguard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickEntryActivity : Activity() {

    companion object {
        const val EXTRA_CATEGORY    = "auto_category"
        const val EXTRA_DESTINATION = "widget_destination"

        private val GOLD      = Color.parseColor("#FFD700")
        private val GOLD_DIM  = Color.parseColor("#33FFD700")
        private val WHITE_DIM = Color.parseColor("#1AFFD700")
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var selectedCategory: SpendingCategory = SpendingCategory.ALIMENTACAO
    private lateinit var chipFood:      LinearLayout
    private lateinit var chipLeisure:   LinearLayout
    private lateinit var chipClothing:  LinearLayout
    private lateinit var chipTech:      LinearLayout
    private lateinit var inputItemName: EditText
    private lateinit var inputValue:    EditText
    private lateinit var btnSave:       Button
    private lateinit var btnAnalyze:    Button
    private lateinit var btnClose:      ImageView
    private lateinit var textError:     TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(android.view.Gravity.BOTTOM)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.6f)

        setContentView(R.layout.activity_quick_entry)

        chipFood      = findViewById(R.id.chip_food)
        chipLeisure   = findViewById(R.id.chip_leisure)
        chipClothing  = findViewById(R.id.chip_clothing)
        chipTech      = findViewById(R.id.chip_tech)
        inputItemName = findViewById(R.id.input_item_name)
        inputValue    = findViewById(R.id.input_value)
        btnSave       = findViewById(R.id.btn_save)
        btnAnalyze    = findViewById(R.id.btn_analyze)
        btnClose      = findViewById(R.id.btn_close)
        textError     = findViewById(R.id.text_error)

        val categoryName = intent.getStringExtra(EXTRA_CATEGORY)
        selectedCategory = SpendingCategory.fromString(categoryName) ?: SpendingCategory.ALIMENTACAO
        updateCategorySelection()

        chipFood.setOnClickListener     { selectCategory(SpendingCategory.ALIMENTACAO) }
        chipLeisure.setOnClickListener  { selectCategory(SpendingCategory.LAZER) }
        chipClothing.setOnClickListener { selectCategory(SpendingCategory.VESTUARIO) }
        chipTech.setOnClickListener     { selectCategory(SpendingCategory.TECNOLOGIA) }

        btnClose.setOnClickListener { finish() }

        findViewById<View>(R.id.quick_entry_root).setOnClickListener {  }

        btnSave.setOnClickListener { handleSave(openGuardian = false) }

        btnAnalyze.setOnClickListener { handleSave(openGuardian = true) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun selectCategory(category: SpendingCategory) {
        selectedCategory = category
        updateCategorySelection()
    }

    private fun updateCategorySelection() {
        val chips = listOf(
            chipFood     to SpendingCategory.ALIMENTACAO,
            chipLeisure  to SpendingCategory.LAZER,
            chipClothing to SpendingCategory.VESTUARIO,
            chipTech     to SpendingCategory.TECNOLOGIA
        )

        chips.forEach { (chip, category) ->
            val isSelected = category == selectedCategory
            chip.alpha = if (isSelected) 1.0f else 0.5f

            val imageView = chip.getChildAt(0) as? ImageView
            imageView?.colorFilter = PorterDuffColorFilter(
                if (isSelected) GOLD else Color.WHITE,
                PorterDuff.Mode.SRC_IN
            )

            val textView = chip.getChildAt(1) as? TextView
            textView?.setTextColor(if (isSelected) GOLD else Color.parseColor("#AAFFFFFF"))
        }
    }

    private fun handleSave(openGuardian: Boolean) {
        textError.visibility = View.GONE

        val name  = inputItemName.text.toString().trim()
        val value = inputValue.text.toString().replace(",", ".").toDoubleOrNull()

        when {
            name.isEmpty() -> {
                showError("Digite o nome do item")
                return
            }
            value == null || value <= 0.0 -> {
                showError("Digite um valor válido")
                return
            }
            value > 1_000_000.0 -> {
                showError("Valor muito alto")
                return
            }
        }

        btnSave.isEnabled    = false
        btnAnalyze.isEnabled = false
        btnSave.text         = "Salvando…"

        scope.launch {
            try {
                val database   = SpendGuardDatabase.getDatabase(applicationContext)
                val userId     = try {
                    SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
                } catch (_: Exception) { "" }

                val purchase = PurchaseEntity(
                    userId        = userId,
                    itemName      = name,
                    price         = value!!,
                    justification = "Registrado via widget",
                    wasBlocked    = false,
                    aiMessage     = "Gasto registrado diretamente pelo widget.",
                    coolingOffTime = 0,
                    category      = selectedCategory.name,
                    isImported    = false
                )

                withContext(Dispatchers.IO) {
                    database.purchaseDao().insert(purchase)
                    UserRepository().syncPurchase(purchase)
                }

                SpendGuardWidget.requestUpdate(applicationContext)

                if (openGuardian) {
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_destination",  "GUARDIAN")
                        putExtra("auto_item_name",       name)
                        putExtra("auto_item_price",      value)
                        putExtra("auto_category",        selectedCategory.name)
                    }
                    startActivity(intent)
                }

                finish()

            } catch (e: Exception) {
                btnSave.isEnabled    = true
                btnAnalyze.isEnabled = true
                btnSave.text         = "Salvar compra"
                showError("Erro ao salvar. Tente novamente.")
            }
        }
    }

    private fun showError(msg: String) {
        textError.text       = msg
        textError.visibility = View.VISIBLE
    }
}
