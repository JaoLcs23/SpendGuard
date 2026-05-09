package com.joaolucas.spendguard

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IntentionsManager(context: Context) {

    private val prefs = context.getSharedPreferences("spendguard_intentions", Context.MODE_PRIVATE)

    private val _intention = MutableStateFlow(getIntention())
    val intention: StateFlow<String> = _intention

    fun getIntention(): String = prefs.getString("current_intention", "") ?: ""

    fun setIntention(text: String) {
        prefs.edit().putString("current_intention", text.take(300)).apply()
        _intention.value = text.take(300)
    }

    fun clear() {
        prefs.edit().remove("current_intention").apply()
        _intention.value = ""
    }
}
