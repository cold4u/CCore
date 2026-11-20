package com.ccorea.loader

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Foreground floating service managing the loader overlay and HUD.
 * Renders an interactive draggable floating widget protected by OEM anti-capture reflections.
 */
class LoaderOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingHeadView: View? = null
    private var expandedDashboardView: View? = null
    private lateinit var headParams: WindowManager.LayoutParams
    private lateinit var dashParams: WindowManager.LayoutParams

    private var activePackageName: String = "com.pubg.imobile"
    private var isExpanded: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startServiceForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_PACKAGE_NAME)?.let {
            activePackageName = it
        }

        if (floatingHeadView == null) {
            setupFloatingHead()
            setupExpandedDashboard()
        }

        return START_NOT_STICKY
    }

    private fun startServiceForeground() {
        val channelId = "ccore_loader_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CCore Loader Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CCore Loader Active Overlay"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentTitle("CCore Loader Active")
                .setContentText("Container overlay is active in background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            startForeground(1001, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingHead() {
        headParams = OverlayWindowManager.createOverlayLayoutParams(
            width = dpToPx(56),
            height = dpToPx(56),
            x = 50,
            y = 200
        )

        val head = TextView(this).apply {
            text = "CC"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E50914"))
                setStroke(dpToPx(2), Color.parseColor("#FFFFFF"))
            }
            background = bg
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        head.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = headParams.x
                    initialY = headParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    headParams.x = initialX + dx
                    headParams.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(head, headParams)
                    } catch (ignored: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleDashboard()
                    }
                    true
                }
                else -> false
            }
        }

        floatingHeadView = head
        try {
            windowManager.addView(floatingHeadView, headParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupExpandedDashboard() {
        dashParams = OverlayWindowManager.createOverlayLayoutParams(
            width = dpToPx(280),
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            x = 60,
            y = 270
        )

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#F0121212"))
                setStroke(dpToPx(1), Color.parseColor("#333333"))
            }
            background = bg
        }

        val title = TextView(this).apply {
            text = "CCore Container HUD"
            textSize = 16f
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(title)

        val pkgText = TextView(this).apply {
            text = "Target: $activePackageName\nSlot: :p0 (Isolated Sandbox)\nAnti-Capture: Active (OEM Cloaked)"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        container.addView(pkgText)

        val closeBtn = Button(this).apply {
            text = "MINIMIZE"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2979FF"))
            setOnClickListener {
                toggleDashboard()
            }
        }
        container.addView(closeBtn)

        expandedDashboardView = container
    }

    private fun toggleDashboard() {
        isExpanded = !isExpanded
        if (isExpanded) {
            dashParams.x = headParams.x
            dashParams.y = headParams.y + dpToPx(60)
            try {
                windowManager.addView(expandedDashboardView, dashParams)
            } catch (ignored: Exception) {}
        } else {
            try {
                windowManager.removeView(expandedDashboardView)
            } catch (ignored: Exception) {}
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingHeadView?.let {
            try { windowManager.removeView(it) } catch (ignored: Exception) {}
        }
        expandedDashboardView?.let {
            try { windowManager.removeView(it) } catch (ignored: Exception) {}
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun start(context: Context, packageName: String) {
            val intent = Intent(context, LoaderOverlayService::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LoaderOverlayService::class.java))
        }
    }
}
