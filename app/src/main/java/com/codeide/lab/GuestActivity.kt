package com.codeide.lab

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Actividad propia que sirve de "invitada" en la prueba 1.
 * Si el contador sube al tocarla dentro del panel, el toque llega a la pantalla virtual.
 */
class GuestActivity : Activity() {

    private var count = 0

    @Suppress("DEPRECATION")
    private fun currentDisplayId(): Int {
        return if (Build.VERSION.SDK_INT >= 30) {
            display?.displayId ?: -1
        } else {
            windowManager.defaultDisplay.displayId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#16303A"))

        val info = TextView(this)
        info.text = "Actividad propia (GuestActivity)"
        info.setTextColor(Color.WHITE)
        info.textSize = 18f
        info.gravity = Gravity.CENTER

        val displayInfo = TextView(this)
        displayInfo.text = "Display id: " + currentDisplayId()
        displayInfo.setTextColor(Color.WHITE)
        displayInfo.gravity = Gravity.CENTER

        val counter = TextView(this)
        counter.text = "Toques: 0"
        counter.setTextColor(Color.WHITE)
        counter.textSize = 22f
        counter.gravity = Gravity.CENTER

        val button = Button(this)
        button.text = "Tocame"
        button.setOnClickListener {
            count++
            counter.text = "Toques: " + count
            LabBus.log("Toque recibido en GuestActivity (#" + count + ")")
        }

        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        layout.addView(info, LinearLayout.LayoutParams(wrap, wrap))
        layout.addView(displayInfo, LinearLayout.LayoutParams(wrap, wrap))
        layout.addView(counter, LinearLayout.LayoutParams(wrap, wrap))
        layout.addView(button, LinearLayout.LayoutParams(wrap, wrap))
        setContentView(layout)

        LabBus.log("GuestActivity creada en el display " + currentDisplayId())
    }
}
