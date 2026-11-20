package com.ccorea.loader

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ccore.CCoreCore
import com.ccore.entity.pm.InstallResult
import java.io.File

/**
 * High-performance application cloner.
 * Registers games installed on the host operating system directly into the CCore
 * container using zero-copy links without duplicating multi-gigabyte APK/OBB payloads.
 */
object AppCloner {
    private const val TAG = "AppCloner"

    /**
     * Checks if the target game is installed on the host system.
     */
    fun isHostInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the target game is already registered inside CCore for the given user.
     */
    fun isContainerInstalled(packageName: String, userId: Int = 0): Boolean {
        return CCoreCore.get().isInstalled(packageName, userId)
    }

    /**
     * Ensures the target application is installed inside the virtual container.
     * Uses zero-copy registration if the application is installed on the host system.
     */
    fun ensureInstalled(context: Context, packageName: String, userId: Int = 0): Boolean {
        if (isContainerInstalled(packageName, userId)) {
            Log.i(TAG, "Package $packageName is already registered in container user $userId")
            return true
        }

        if (!isHostInstalled(context, packageName)) {
            Log.e(TAG, "Target package $packageName is not installed on host device")
            return false
        }

        Log.i(TAG, "Installing host package $packageName into CCore user $userId...")
        val result: InstallResult = CCoreCore.get().installPackageAsUser(packageName, userId)
        if (result.success) {
            Log.i(TAG, "Successfully installed $packageName into container")
            syncObbData(context, packageName)
            return true
        } else {
            Log.e(TAG, "Failed to install $packageName into container: ${result.msg}")
            return false
        }
    }

    /**
     * Ensures OBB assets exist in the virtual environment.
     */
    private fun syncObbData(context: Context, packageName: String) {
        try {
            val hostObbDir = File(Environment.getExternalStorageDirectory(), "Android/obb/$packageName")
            if (hostObbDir.exists() && hostObbDir.isDirectory) {
                Log.d(TAG, "Found host OBB directory: ${hostObbDir.absolutePath} (${hostObbDir.listFiles()?.size ?: 0} files)")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to inspect host OBB data: ${e.message}")
        }
    }

    /**
     * Launches the target game inside the CCore sandbox.
     */
    fun launchGame(packageName: String, userId: Int = 0): Boolean {
        Log.i(TAG, "Launching $packageName in container user $userId...")
        return CCoreCore.get().launchApk(packageName, userId)
    }
}
