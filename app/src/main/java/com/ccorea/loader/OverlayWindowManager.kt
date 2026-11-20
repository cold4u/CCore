package com.ccorea.loader

import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Window manager configuration for floating loader overlays.
 * Implements OEM-level display reflections to bypass screen recording and frame capture
 * across AOSP, Meizu Flyme, and Samsung OneUI architectures.
 */
object OverlayWindowManager {
    private const val TAG = "OverlayWindowManager"

    // AOSP Extended Private Flag (0x00100000)
    private const val PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY = 0x00100000

    // Meizu Flyme OS Flag
    private const val MEIZU_FLAG_RECORD_FILTER = 0x2000

    // Samsung OneUI Extension Flag
    private const val SEM_EXTENSION_FLAG_RESIZE_DISABLE = 0x00000001

    /**
     * Builds layout parameters for an anti-capture overlay window.
     */
    fun createOverlayLayoutParams(width: Int, height: Int, x: Int = 100, y: Int = 100): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            width,
            height,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        applyAntiRecordReflections(params)
        return params
    }

    /**
     * Applies vendor-specific private flags via reflection to hide overlay surface
     * from system screen recorders and external capture cards.
     */
    fun applyAntiRecordReflections(params: WindowManager.LayoutParams) {
        // 1. AOSP privateFlags: Mark window as rounded corners overlay
        try {
            val privateFlagsField: Field = WindowManager.LayoutParams::class.java.getDeclaredField("privateFlags")
            privateFlagsField.isAccessible = true
            val currentFlags = privateFlagsField.getInt(params)
            privateFlagsField.setInt(params, currentFlags or PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY)
            Log.d(TAG, "Applied AOSP privateFlags: 0x00100000")
        } catch (e: Throwable) {
            Log.d(TAG, "AOSP privateFlags not available on this device")
        }

        // 2. Meizu Flyme OS reflection
        try {
            val meizuClass = Class.forName("android.view.MeizuLayoutParams")
            val flagField = meizuClass.getDeclaredField("flags")
            flagField.isAccessible = true
            val meizuInstance = meizuClass.getDeclaredConstructor().newInstance()
            flagField.setInt(meizuInstance, MEIZU_FLAG_RECORD_FILTER)

            val meizuParamsField = WindowManager.LayoutParams::class.java.getDeclaredField("meizuParams")
            meizuParamsField.isAccessible = true
            meizuParamsField.set(params, meizuInstance)
            Log.d(TAG, "Applied Meizu MEIZU_FLAG_RECORD_FILTER")
        } catch (e: Throwable) {
            // Not a Meizu device
        }

        // 3. Samsung OneUI semAddExtensionFlags reflection
        try {
            val semMethod: Method = WindowManager.LayoutParams::class.java.getMethod("semAddExtensionFlags", Int::class.javaPrimitiveType)
            semMethod.isAccessible = true
            semMethod.invoke(params, SEM_EXTENSION_FLAG_RESIZE_DISABLE)
            Log.d(TAG, "Applied Samsung semAddExtensionFlags")
        } catch (e: Throwable) {
            // Not a Samsung device
        }
    }
}
