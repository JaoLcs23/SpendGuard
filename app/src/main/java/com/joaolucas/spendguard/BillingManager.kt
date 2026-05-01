package com.joaolucas.spendguard

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(
    private val context: Context,
    private val proManager: ProManager
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "spendguard_pro_monthly"
        const val PRODUCT_YEARLY  = "spendguard_pro_yearly"
    }

    sealed class BillingState {
        object Idle : BillingState()
        object Loading : BillingState()
        data class Success(val plan: String) : BillingState()
        data class Error(val message: String) : BillingState()
        object UserCancelled : BillingState()
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    restorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun disconnect() {
        billingClient.endConnection()
    }

    suspend fun launchBillingFlow(activity: Activity, productId: String) {
        _billingState.value = BillingState.Loading

        if (!billingClient.isReady) {
            _billingState.value = BillingState.Error("Serviço de pagamento não disponível")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val productDetailsResult = suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { result, details ->
                cont.resume(Pair(result, details))
            }
        }

        val (queryResult, productDetailsList) = productDetailsResult
        if (queryResult.responseCode != BillingClient.BillingResponseCode.OK
            || productDetailsList.isEmpty()
        ) {
            _billingState.value = BillingState.Error(
                "Produto não encontrado. Verifique o Google Play Console."
            )
            return
        }

        val productDetails = productDetailsList.first()

        val offerToken = productDetails
            .subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: run {
                _billingState.value = BillingState.Error("Nenhuma oferta disponível")
                return
            }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingState.value = BillingState.UserCancelled
            }
            else -> {
                _billingState.value = BillingState.Error(
                    "Erro no pagamento (${result.responseCode}): ${result.debugMessage}"
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    activateProLocally(purchase)
                } else {
                    _billingState.value = BillingState.Error("Falha ao confirmar compra")
                }
            }
        } else {
            activateProLocally(purchase)
        }
    }

    private fun activateProLocally(purchase: Purchase) {
        val plan = when {
            purchase.products.contains(PRODUCT_YEARLY)  -> "yearly"
            purchase.products.contains(PRODUCT_MONTHLY) -> "monthly"
            else -> "monthly"
        }
        proManager.activatePro(plan)
        _billingState.value = BillingState.Success(plan)
    }

    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val activePurchase = purchases.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (activePurchase != null) {
                activateProLocally(activePurchase)
            } else {
                if (proManager.isPro.value) proManager.deactivatePro()
            }
        }
    }
}