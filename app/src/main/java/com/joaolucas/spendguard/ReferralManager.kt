package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Serializable
data class ReferralRow(
    val id: String = "",
    @SerialName("referrer_id")  val referrerId: String = "",
    @SerialName("referred_id")  val referredId: String = "",
    val code: String = "",
    @SerialName("used_at")      val usedAt: String? = null,
    @SerialName("reward_given") val rewardGiven: Boolean = false
)

class ReferralManager(
    private val context: Context,
    private val proManager: ProManager
) {
    private val client = SupabaseClient.client
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendguard_referral", Context.MODE_PRIVATE)

    companion object {
        const val DEEP_LINK_HOST   = "referral"
        const val REFERRAL_SCHEME  = "spendguard"
        const val PLAY_STORE_URL   = "https://play.google.com/store/apps/details?id=com.joaolucas.spendguard"
        private const val KEY_PENDING_CODE = "pending_referral_code"
        private const val KEY_REDEEMED    = "referral_redeemed"
        private const val KEY_MY_CODE     = "my_referral_code"
        private const val CODE_MAX_LENGTH = 8
        private val CODE_REGEX = Regex("^[A-F0-9]{8}$")
    }

    private val _referralState = MutableStateFlow<ReferralState>(ReferralState.Idle)
    val referralState: StateFlow<ReferralState> = _referralState

    fun getMyCode(): String? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        val code = userId.replace("-", "").take(CODE_MAX_LENGTH).uppercase()
        prefs.edit().putString(KEY_MY_CODE, code).apply()
        return code
    }

    fun getShareLink(): String? {
        val code = getMyCode() ?: return null
        return "$REFERRAL_SCHEME://$DEEP_LINK_HOST?code=$code"
    }

    fun getShareText(): String {
        val code = getMyCode() ?: "SEU_CÓDIGO"
        return "Controlo meus impulsos financeiros com o SpendGuard! 🛡️\n\n" +
                "Use meu código de convite e ganhe 30% off no primeiro mês Pro:\n" +
                "Código: $code\n\n" +
                "Baixe aqui: $PLAY_STORE_URL?referral=$code"
    }

    fun savePendingCode(code: String) {
        if (code.isBlank()) return
        val sanitized = code.trim().uppercase()
        if (!CODE_REGEX.matches(sanitized)) return
        prefs.edit().putString(KEY_PENDING_CODE, sanitized).apply()
    }

    fun getPendingCode(): String? = prefs.getString(KEY_PENDING_CODE, null)

    fun hasPendingCode(): Boolean = !getPendingCode().isNullOrBlank()

    fun hasBeenRedeemed(): Boolean = prefs.getBoolean(KEY_REDEEMED, false)

    suspend fun redeemPendingReferral() {
        val code = getPendingCode() ?: return
        if (hasBeenRedeemed()) return

        val sanitizedCode = code.trim().uppercase()
        if (!CODE_REGEX.matches(sanitizedCode)) {
            prefs.edit().remove(KEY_PENDING_CODE).apply()
            _referralState.value = ReferralState.Invalid
            return
        }

        val referredId = client.auth.currentUserOrNull()?.id ?: return

        val myCode = getMyCode()
        if (myCode != null && sanitizedCode == myCode) {
            _referralState.value = ReferralState.Invalid
            return
        }

        _referralState.value = ReferralState.Loading

        try {
            val referrer = client.postgrest["users"]
                .select(Columns.list("id", "email", "referral_code")) {
                    filter { eq("referral_code", sanitizedCode) }
                }
                .decodeSingleOrNull<UserProfile>()

            if (referrer == null || referrer.id == referredId) {
                prefs.edit().remove(KEY_PENDING_CODE).apply()
                _referralState.value = ReferralState.Invalid
                return
            }

            client.postgrest["referrals"].insert(
                ReferralRow(
                    referrerId  = referrer.id,
                    referredId  = referredId,
                    code        = sanitizedCode,
                    rewardGiven = false
                )
            )

            proManager.activateTrialPro(days = 7)

            prefs.edit()
                .putBoolean(KEY_REDEEMED, true)
                .remove(KEY_PENDING_CODE)
                .apply()

            _referralState.value = ReferralState.Success(referrerName = referrer.email ?: "")

        } catch (e: Exception) {
            _referralState.value = ReferralState.Error(e.message ?: "Erro ao resgatar convite")
        }
    }

    suspend fun getReferralCount(): Int {
        return try {
            val myCode = getMyCode() ?: return 0
            client.postgrest["referrals"]
                .select { filter { eq("code", myCode) } }
                .decodeList<ReferralRow>()
                .size
        } catch (_: Exception) { 0 }
    }

    sealed class ReferralState {
        object Idle    : ReferralState()
        object Loading : ReferralState()
        object Invalid : ReferralState()
        data class Success(val referrerName: String) : ReferralState()
        data class Error(val message: String)        : ReferralState()
    }
}