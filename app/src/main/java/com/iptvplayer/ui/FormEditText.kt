package com.iptvplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.EditText

/**
 * EditText qui laisse la croix directionnelle haut/bas changer de champ.
 * Sans cela, un EditText consomme la touche et le formulaire devient
 * impossible à parcourir avec une simple télécommande.
 */
class FormEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : EditText(context, attrs) {

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            val direction = if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                View.FOCUS_DOWN
            } else {
                View.FOCUS_UP
            }
            val next = focusSearch(direction)
            if (next != null) {
                next.requestFocus()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
