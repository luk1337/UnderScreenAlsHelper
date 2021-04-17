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
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.luk.underscreenalshelper.databinding.ActivityFullscreenBinding

class FullscreenActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var circle: Circle
    private lateinit var status: TextView

    private var lux = 0.0f

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

        companion object {
            const val SIZE = 40.0f
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
        circle.x = (resources.displayMetrics.widthPixels.toFloat() - Circle.SIZE) / 2
        circle.y = (resources.displayMetrics.heightPixels.toFloat() - Circle.SIZE) / 2

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
            return@setOnTouchListener true
        }

        status = binding.status

        val sensorManager = getSystemService(SensorManager::class.java)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        lux = event?.values?.get(0)!!
        updateStatusText()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // don't care
    }

    @SuppressLint("SetTextI18n")
    fun updateStatusText() {
        status.text = "lux: ${lux}\n(x: ${circle.x.toInt()}, y: ${circle.y.toInt()})"
    }
}