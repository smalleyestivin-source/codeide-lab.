package com.codeide.lab

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Laboratorio de viabilidad. Responde una sola pregunta:
 * puede esta tablet mostrar una actividad (propia o de otra app) dentro de un panel nuestro?
 */
class MainActivity : Activity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private var surface: Surface? = null
    private var surfaceW = 0
    private var surfaceH = 0
    private var virtualDisplay: VirtualDisplay? = null
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private fun px(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LabBus.sink = { msg -> log(msg) }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0B0E14"))
        root.setPadding(px(12), px(12), px(12), px(12))

        val title = TextView(this)
        title.text = "CodeIDE Lab: prueba de viabilidad"
        title.setTextColor(Color.parseColor("#E6EAF2"))
        title.textSize = 18f
        title.typeface = Typeface.DEFAULT_BOLD
        root.addView(title)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        addButton(row, "1. Actividad propia en panel") {
            launchOnPanel(Intent(this, GuestActivity::class.java), "GuestActivity (propia)")
        }
        addButton(row, "2. Ajustes en panel") {
            launchOnPanel(Intent(Settings.ACTION_SETTINGS), "Ajustes del sistema")
        }
        addButton(row, "3. Catalogo: abrir normal") { showCatalog(false) }
        addButton(row, "4. Catalogo: probar en panel") { showCatalog(true) }
        addButton(row, "Copiar resultados") { copyLog() }
        val rowScroll = HorizontalScrollView(this)
        rowScroll.addView(row)
        root.addView(rowScroll)

        val panel = FrameLayout(this)
        panel.setBackgroundColor(Color.parseColor("#131A27"))
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        panel.addView(
            surfaceView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        val panelLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        panelLp.topMargin = px(8)
        panelLp.bottomMargin = px(8)
        root.addView(panel, panelLp)

        logView = TextView(this)
        logView.setTextColor(Color.parseColor("#E6EAF2"))
        logView.textSize = 12f
        logView.typeface = Typeface.MONOSPACE
        logView.setTextIsSelectable(true)
        logScroll = ScrollView(this)
        logScroll.addView(logView)
        root.addView(
            logScroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(180))
        )

        setContentView(root)

        log("Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL +
            ", Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        log("Toca un boton. Si una app aparece a pantalla completa en vez de en el panel, Android la rechazo para el panel.")
    }

    private fun addButton(row: LinearLayout, label: String, onClick: () -> Unit) {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = px(8)
        row.addView(b, lp)
    }

    private fun log(msg: String) {
        runOnUiThread {
            logView.append(timeFmt.format(Date()) + "  " + msg + System.lineSeparator())
            logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun copyLog() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("resultados", logView.text))
        Toast.makeText(this, "Resultados copiados", Toast.LENGTH_SHORT).show()
    }

    // ---- Panel (SurfaceView) ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surface = holder.surface
        surfaceW = width
        surfaceH = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (virtualDisplay != null) {
            log("El panel se destruyo: se libera la pantalla virtual")
        }
        releaseDisplay()
        surface = null
    }

    private fun releaseDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private fun ensureDisplay(): VirtualDisplay? {
        val s = surface
        if (s == null || !s.isValid || surfaceW <= 0 || surfaceH <= 0) {
            log("El panel aun no esta listo")
            return null
        }
        releaseDisplay()
        return try {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            val vd = dm.createVirtualDisplay(
                "codeide-lab-panel",
                surfaceW,
                surfaceH,
                resources.displayMetrics.densityDpi,
                s,
                flags
            )
            if (vd == null) {
                log("Android devolvio null al crear la pantalla virtual")
                null
            } else {
                virtualDisplay = vd
                log("Pantalla virtual creada: id=" + vd.display.displayId + ", " + surfaceW + "x" + surfaceH)
                vd
            }
        } catch (e: Exception) {
            log("ERROR al crear la pantalla virtual: " + e.javaClass.simpleName + ": " + e.message)
            null
        }
    }

    private fun launchOnPanel(intent: Intent, label: String) {
        val vd = ensureDisplay() ?: return
        try {
            val opts = ActivityOptions.makeBasic()
            opts.setLaunchDisplayId(vd.display.displayId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent, opts.toBundle())
            log("Lanzado: " + label + " hacia el display " + vd.display.displayId)
            log("Mira el panel. Si es la prueba 1, toca el boton dentro: si el contador no sube, el toque no llega al panel.")
        } catch (e: Exception) {
            log("ERROR al lanzar " + label + ": " + e.javaClass.simpleName + ": " + e.message)
        }
    }

    // ---- Catalogo ----

    private fun showCatalog(inPanel: Boolean) {
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        if (apps.isEmpty()) {
            log("El catalogo esta vacio (Android no mostro aplicaciones)")
            return
        }
        val names = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(if (inPanel) "Probar en el panel" else "Abrir con Android")
            .setItems(names) { _, i ->
                val info = apps[i].activityInfo
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(info.packageName, info.name)
                if (inPanel) {
                    launchOnPanel(intent, names[i])
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        startActivity(intent)
                        log("Abierta con Android (pantalla completa): " + names[i])
                    } catch (e: Exception) {
                        log("ERROR al abrir " + names[i] + ": " + e.javaClass.simpleName + ": " + e.message)
                    }
                }
            }
            .show()
    }

    override fun onDestroy() {
        releaseDisplay()
        LabBus.sink = null
        super.onDestroy()
    }
}
