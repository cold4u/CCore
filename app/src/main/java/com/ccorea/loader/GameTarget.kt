package com.ccorea.loader

import android.content.Context
import android.content.pm.PackageManager

/**
 * Definition of target games supported by the CCore Loader container.
 */
data class GameTarget(
    val name: String,
    val packageName: String,
    val description: String,
    val defaultActivity: String = ""
) {
    fun isInstalledOnDevice(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        val BGMI = GameTarget(
            name = "Battlegrounds Mobile India (BGMI)",
            packageName = "com.pubg.imobile",
            description = "BGMI 3.x / 4.x Client (Krafton)"
        )

        val PUBG_GLOBAL = GameTarget(
            name = "PUBG Mobile (Global)",
            packageName = "com.tencent.ig",
            description = "PUBG Mobile Global Edition (Level Infinite / Tencent)"
        )

        val PUBG_KR = GameTarget(
            name = "PUBG Mobile (Korea)",
            packageName = "com.pubg.krmobile",
            description = "PUBG Mobile Korea / Japan (Krafton)"
        )

        val PUBG_VN = GameTarget(
            name = "PUBG Mobile (Vietnam)",
            packageName = "com.vng.pubgmobile",
            description = "PUBG Mobile Vietnam (VNG Game Publishing)"
        )

        val PUBG_TW = GameTarget(
            name = "PUBG Mobile (Taiwan)",
            packageName = "com.revmob.game",
            description = "PUBG Mobile Taiwan (HotCool Games)"
        )

        val ALL_TARGETS = listOf(BGMI, PUBG_GLOBAL, PUBG_KR, PUBG_VN, PUBG_TW)
        val ALL_PACKAGES = ALL_TARGETS.map { it.packageName }.toSet()
    }
}
