package com.ccorea.loader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Main launcher dashboard for CCore Loader.
 * Manages runtime permission gates, discovers device game installations,
 * and orchestrates zero-copy container launching alongside the anti-capture overlay.
 */
class LoaderActivity : AppCompatActivity() {

    private lateinit var statusContainer: LinearLayout
    private var selectedTarget: GameTarget = GameTarget.BGMI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshTargetList()
    }

    private fun buildUi() {
        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(0xFF0F0F12.toInt())
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
        }
        rootScroll.addView(mainLayout)

        // Header Title
        val title = TextView(this).apply {
            text = "CCORE LOADER"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        mainLayout.addView(title)

        val subtitle = TextView(this).apply {
            text = "Hardware-Isolated Android Container & Overlay System"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 48)
        }
        mainLayout.addView(subtitle)

        // Target Games Container
        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(statusContainer)

        // Launch Action Button
        val launchBtn = Button(this).apply {
            text = "START SANDBOX & OVERLAY"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFE50914.toInt())
            setPadding(0, 36, 0, 36)
            setOnClickListener {
                onLaunchClicked()
            }
        }
        mainLayout.addView(launchBtn)

        setContentView(rootScroll)
    }

    private fun refreshTargetList() {
        statusContainer.removeAllViews()

        for (target in GameTarget.ALL_TARGETS) {
            val isInstalled = target.isInstalledOnDevice(this)
            val isCloned = AppCloner.isContainerInstalled(target.packageName, 0)
            val isSelected = target.packageName == selectedTarget.packageName

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
                setBackgroundColor(if (isSelected) 0xFF1E2640.toInt() else 0xFF18181D.toInt())

                val header = TextView(this@LoaderActivity).apply {
                    text = target.name
                    textSize = 15f
                    setTextColor(if (isSelected) 0xFF448AFF.toInt() else 0xFFDDDDDD.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                addView(header)

                val details = TextView(this@LoaderActivity).apply {
                    val installStatus = when {
                        isCloned -> "STATUS: MOUNTED IN SANDBOX"
                        isInstalled -> "STATUS: INSTALLED ON DEVICE (READY TO CLONE)"
                        else -> "STATUS: NOT FOUND ON DEVICE"
                    }
                    text = "${target.packageName}\n$installStatus"
                    textSize = 11f
                    setTextColor(if (isInstalled || isCloned) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
                    setPadding(0, 8, 0, 0)
                }
                addView(details)

                setOnClickListener {
                    selectedTarget = target
                    refreshTargetList()
                }
            }

            val marginParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            statusContainer.addView(card, marginParams)
        }
    }

    private fun onLaunchClicked() {
        if (!checkAndRequestPermissions()) {
            Toast.makeText(this, "Please grant required permissions first", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Ensure target app is cloned into sandbox
        val ready = AppCloner.ensureInstalled(this, selectedTarget.packageName, 0)
        if (!ready) {
            Toast.makeText(this, "Target game is not installed on this device", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Start anti-capture overlay service
        LoaderOverlayService.start(this, selectedTarget.packageName)

        // 3. Launch target inside CCore sandbox
        val launched = AppCloner.launchGame(selectedTarget.packageName, 0)
        if (launched) {
            Toast.makeText(this, "Game launched in isolated slot :p0", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Failed to launch game process", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        // 1. System Alert Window (Overlay)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        }

        // 2. All Files Access (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        }

        // 3. Storage Permissions (Android 10 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
                return false
            }
        }

        return true
    }
}
