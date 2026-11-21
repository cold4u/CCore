package com.cold.loader

import android.content.Context
import android.util.Log
import com.ccore.CCoreCore
import com.ccore.entity.pm.InstallResult

/**
 * High-speed, zero-copy application mounting and launching service for Cold Loader.
 */
object ColdCloner {
    private const val TAG = "ColdCloner"

    fun isInstalledInSandbox(packageName: String, userId: Int = 0): Boolean {
        return try {
            CCoreCore.get().isInstalled(packageName, userId)
        } catch (t: Throwable) {
            false
        }
    }

    fun prepareAndInstall(context: Context, packageName: String, userId: Int = 0): Boolean {
        if (isInstalledInSandbox(packageName, userId)) {
            Log.i(TAG, "Package $packageName is already mounted in sandbox user $userId")
            return true
        }

        Log.i(TAG, "Mounting host package $packageName into CCore sandbox user $userId...")
        val result: InstallResult = CCoreCore.get().installPackageAsUser(packageName, userId)
        if (result.success) {
            Log.i(TAG, "Mounted $packageName successfully")
            return true
        } else {
            Log.e(TAG, "Failed to mount $packageName: ${result.msg}")
            return false
        }
    }

    fun launch(packageName: String, userId: Int = 0): Boolean {
        Log.i(TAG, "Launching $packageName in virtual sandbox slot...")
        return CCoreCore.get().launchApk(packageName, userId)
    }
}
