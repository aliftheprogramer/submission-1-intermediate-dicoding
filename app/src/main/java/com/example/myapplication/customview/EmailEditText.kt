package com.example.myapplication.customview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText

class EmailEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs), View.OnTouchListener {

    init {
        setOnTouchListener(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        hint = "masukan email anda"
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return false
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        if (!isValidEmail(text)){
            error = "Email tidak valid"
        } else {
            error = null
        }
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
    }

    private fun isValidEmail(email: CharSequence?): Boolean{
        return !email.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}