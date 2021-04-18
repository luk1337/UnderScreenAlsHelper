package com.luk.underscreenalshelper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.luk.underscreenalshelper.databinding.ActivityFullscreenBinding
import com.luk.underscreenalshelper.databinding.DialogSetCoordinatesBinding

class FullscreenActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var circle: Circle

    private var lux = 0.0f
    private var scanMode = ScanMode()

    data class ScanMode(
        var enabled: Boolean = false,
        var lux: Float = 0.0f,
        var pos: Pair<Float, Float> = Pair(0.0f, 0.0f),
        var minX: Float = 0.0f,
        var minY: Float = 0.0f,
        var maxX: Float = 0.0f,
        var maxY: Float = 0.0f,
        var step: Float = 10.0f
    ) {
        fun reset() {
            lux = 0.0f
            pos = Pair(0.0f, 0.0f)
        }

        fun setPrefs(minX: Float?, minY: Float?, maxX: Float?, maxY: Float?, step: Float?) {
            if (minX != null) {
                this.minX = minX
            }
            if (minY != null) {
                this.minY = minY
            }
            if (maxX != null) {
                this.maxX = maxX
            }
            if (maxY != null) {
                this.maxY = maxY
            }
            if (step != null) {
                this.step = step
            }
        }

        override fun toString(): String {
            return "ScanMode{enabled: ${enabled}, lux: $lux, pos: $pos}"
        }
    }

    internal class Circle(context: Context) : View(context) {
        private val paint: Paint = Paint()

        init {
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawCircle(SIZE, SIZE, SIZE, paint)
        }

        override fun toString(): String {
            val center = getCenter()
            return "Circle{x: ${center.first.toInt()}, y: ${center.second.toInt()}}"
        }

        fun getCenter(): Pair<Float, Float> {
            return Pair(x + SIZE, y + SIZE)
        }

        fun setCenter(x: Float?, y: Float?) {
            if (x != null) {
                this.x = x - SIZE
            }
            if (y != null) {
                this.y = y - SIZE
            }
        }

        fun reset() {
            setCenter(
                resources.displayMetrics.widthPixels.toFloat() / 2,
                resources.displayMetrics.heightPixels.toFloat() / 2
            )
        }

        companion object {
            private const val SIZE = 40.0f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        window.attributes.screenBrightness = 1.0f
        window.insetsController?.let {
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsets.Type.systemBars())
        }

        circle = Circle(this)
        circle.reset()

        scanMode.maxX = resources.displayMetrics.widthPixels.toFloat()
        scanMode.maxY = resources.displayMetrics.heightPixels.toFloat()

        val doubleTapDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                AlertDialog.Builder(this@FullscreenActivity).apply {
                    val view = DialogSetCoordinatesBinding.inflate(layoutInflater)

                    val center = circle.getCenter()
                    view.circleX.setText(center.first.toString())
                    view.circleY.setText(center.second.toString())

                    view.scanModeMinX.setText(scanMode.minX.toString())
                    view.scanModeMinY.setText(scanMode.minY.toString())
                    view.scanModeMaxX.setText(scanMode.maxX.toString())
                    view.scanModeMaxY.setText(scanMode.maxY.toString())
                    view.scanModeStep.setText(scanMode.step.toString())

                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                        circle.setCenter(
                            view.circleX.text.toString().toFloatOrNull()!!,
                            view.circleY.text.toString().toFloatOrNull()!!
                        )
                        scanMode.setPrefs(
                            view.scanModeMinX.text.toString().toFloatOrNull()!!,
                            view.scanModeMinY.text.toString().toFloatOrNull()!!,
                            view.scanModeMaxX.text.toString().toFloatOrNull()!!,
                            view.scanModeMaxY.text.toString().toFloatOrNull()!!,
                            view.scanModeStep.text.toString().toFloatOrNull()!!
                        )
                        dialog.dismiss()
                    }

                    setTitle(R.string.settings)
                    setView(view.root)
                    create()
                    show()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                if (scanMode.enabled) {
                    Toast.makeText(
                        this@FullscreenActivity,
                        R.string.scan_mode_off,
                        Toast.LENGTH_SHORT
                    ).show()
                    circle.reset()
                } else {
                    Toast.makeText(
                        this@FullscreenActivity,
                        R.string.scan_mode_on,
                        Toast.LENGTH_SHORT
                    ).show()
                    circle.setCenter(scanMode.minX, scanMode.minY)
                    scanMode.reset()
                }

                scanMode.enabled = !scanMode.enabled
            }
        })
        var touchCoordinates = Pair(circle.x, circle.y)

        binding.frame.addView(circle)
        binding.frame.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    touchCoordinates = Pair(motionEvent.x, motionEvent.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    circle.x -= touchCoordinates.first - motionEvent.x
                    circle.y -= touchCoordinates.second - motionEvent.y
                    touchCoordinates = Pair(motionEvent.x, motionEvent.y)
                }
            }

            updateStatusText()
            doubleTapDetector.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }

        val sensorManager = getSystemService(SensorManager::class.java)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        lux = event?.values?.get(0)!!
        updateScanModeData()
        updateStatusText()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // don't care
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatusText() {
        binding.status.text = "lux: ${lux}\n${circle}\n${scanMode}"
    }

    private fun updateScanModeData() {
        if (!scanMode.enabled) return;

        val center = circle.getCenter()

        if (lux > scanMode.lux) {
            scanMode.lux = lux
            scanMode.pos = center
        }

        when {
            center.first == scanMode.maxX && center.second == scanMode.maxY -> {
                scanMode.enabled = false
            }
            center.first == scanMode.maxX -> {
                circle.setCenter(scanMode.minX, center.second + scanMode.step)
            }
            else -> {
                circle.x += scanMode.step
            }
        }
    }
}