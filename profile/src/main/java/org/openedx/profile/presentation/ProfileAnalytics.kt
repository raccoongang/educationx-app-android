package org.openedx.profile.presentation

interface ProfileAnalytics {
    fun profileEditClickedEvent()
    fun profileEditDoneClickedEvent()
    fun profileDeleteAccountClickedEvent()
    fun profileVideoSettingsClickedEvent()
    fun privacyPolicyClickedEvent()
    fun cookiePolicyClickedEvent()
    fun emailSupportClickedEvent()
    fun logoutEvent(force: Boolean)
}