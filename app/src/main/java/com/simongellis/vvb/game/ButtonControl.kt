package com.simongellis.vvb.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.simongellis.vvb.R
import com.simongellis.vvb.emulator.Input

class ButtonControl: Control {
    private val _button = ContextCompat.getDrawable(context, R.drawable.ic_button)!!
    private val _boundsPaint: Paint = Paint()
    private var _onTouch: (MotionEvent) -> Unit
    private var _isToggled = false

    private var _input: Input? = null
    private var _isToggleable = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }
    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    init {
        _boundsPaint.color = ColorUtils.setAlphaComponent(Color.DKGRAY, 0x80)
        _onTouch = ::handleTouch
        setOnTouchListener { v, event ->
            _onTouch(event)
            v.performClick()
            true
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ButtonControl)

        try {
            val inputStr = a.getString(R.styleable.ButtonControl_input)
            _input = inputStr?.let { Input.valueOf(it) }
            _isToggleable = a.getBoolean(R.styleable.ButtonControl_toggleable, false)
        } finally {
            a.recycle()
        }
    }

    override fun setPreferences(preferences: GamePreferences) {
        super.setPreferences(preferences)
        if (_isToggleable && preferences.toggleMode) {
            _onTouch = ::handleToggle
        }
    }

    private fun handleTouch(event: MotionEvent) {
        isPressed = event.action != ACTION_UP
    }

    private fun handleToggle(event: MotionEvent) {
        if (event.action == ACTION_DOWN) {
            isPressed = true
        }
        if (event.action == ACTION_UP) {
            if (_isToggled) {
                isPressed = false
            }
            _isToggled = !_isToggled
        }
    }

    override fun setPressed(pressed: Boolean) {
        if (isPressed == pressed) return
        super.setPressed(pressed)
        if (pressed) {
            _input?.also { controller?.press(it) }
            performHapticPress()
        } else {
            _input?.also { controller?.release(it) }
            performHapticRelease()
        }
        drawingState = if (pressed) { 1 } else { 0 }
    }

    override fun drawGrayscale(canvas: Canvas, width: Int, height: Int) {
        if (shouldDrawBounds) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), _boundsPaint)
        }
        _button.setBounds(0, 0, width, height)
        _button.alpha = if (isPressed) { 0xff } else { 0x80 }
        _button.draw(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("superState", super.onSaveInstanceState())
            putBoolean("pressed", isPressed)
            putBoolean("toggled", _isToggled)
        }
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(state: Parcelable?) {
        val realState: Parcelable?
        if (state is Bundle) {
            _isToggled = state.getBoolean("toggled")
            isPressed = state.getBoolean("pressed")
            realState = state.getParcelable("superState")
        } else {
            realState = state
        }
        super.onRestoreInstanceState(realState)
    }

}