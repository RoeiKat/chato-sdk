package com.chato.sdk.net.dto

data class SdkConfigRes(
    val app: AppInfo = AppInfo(),
    val prechat: PrechatConfig = PrechatConfig(),
    val theme: ThemeConfig = ThemeConfig()
)

data class AppInfo(
    val name: String = ""
)

data class PrechatConfig(
    val q1: String = "",
    val q2: String = "",
    val q3: String = "",
    val faqUrl: String = ""
)

data class ThemeConfig(
    val bubbleBg: String = "",
    val primary: String = "",
    val iconSvg: String = ""
)
