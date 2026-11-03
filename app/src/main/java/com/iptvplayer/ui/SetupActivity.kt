package com.iptvplayer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.iptvplayer.R
import com.iptvplayer.data.Prefs
import com.iptvplayer.data.XtreamClient
import com.iptvplayer.data.XtreamUrl
import com.iptvplayer.model.Account
import com.iptvplayer.util.Expiry
import com.iptvplayer.util.Ui
import java.net.URL

class SetupActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var serverField: EditText
    private lateinit var userField: EditText
    private lateinit var passwordField: EditText
    private lateinit var showPassword: CheckBox
    private lateinit var connectButton: Button
    private lateinit var errorText: TextView
    private lateinit var accountInfo: TextView
    private var editMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        editMode = intent.getBooleanExtra(EXTRA_EDIT, false)

        if (!editMode && prefs.isConfigured()) {
            openHome()
            return
        }

        setContentView(R.layout.activity_setup)
        serverField = findViewById(R.id.serverField)
        userField = findViewById(R.id.userField)
        passwordField = findViewById(R.id.passwordField)
        showPassword = findViewById(R.id.showPassword)
        connectButton = findViewById(R.id.connectButton)
        errorText = findViewById(R.id.errorText)
        accountInfo = findViewById(R.id.accountInfo)

        if (editMode) {
            findViewById<TextView>(R.id.setupTitle).setText(R.string.setup_title_edit)
            findViewById<TextView>(R.id.setupHelp).setText(R.string.setup_help_edit)
            connectButton.setText(R.string.save)
            serverField.setText(prefs.server())
            userField.setText(prefs.username())
            passwordField.setText(prefs.password())
            if (prefs.isConfigured()) {
                accountInfo.visibility = View.VISIBLE
                accountInfo.text = Expiry.label(this, prefs.expiresAt())
            }
        }

        showPassword.setOnCheckedChangeListener { _, checked ->
            val cursor = passwordField.selectionEnd.coerceAtLeast(0)
            passwordField.inputType = if (checked) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordField.setSelection(cursor.coerceAtMost(passwordField.text.length))
        }

        passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                connect()
                true
            } else {
                false
            }
        }

        connectButton.setOnClickListener { connect() }
        serverField.requestFocus()
    }

    private fun connect() {
        hideError()
        val server = XtreamUrl.normalizeServer(serverField.text.toString())
        val user = userField.text.toString().trim()
        val password = passwordField.text.toString()

        if (server.isBlank()) {
            showError(R.string.error_empty_server)
            serverField.requestFocus()
            return
        }
        try {
            URL(server)
        } catch (e: Exception) {
            showError(R.string.err_network)
            serverField.requestFocus()
            return
        }
        if (user.isBlank()) {
            showError(R.string.error_empty_user)
            userField.requestFocus()
            return
        }
        if (password.isBlank()) {
            showError(R.string.error_empty_password)
            passwordField.requestFocus()
            return
        }

        hideKeyboard()
        setLoading(true)
        Ui.background({
            XtreamClient(server, user, password).login()
        }) { result ->
            if (isFinishing || isDestroyed) return@background
            result.onSuccess { account -> onLoginSuccess(server, user, password, account) }
                .onFailure { error ->
                    setLoading(false)
                    errorText.text = error.message ?: getString(R.string.err_network)
                    errorText.visibility = View.VISIBLE
                    connectButton.requestFocus()
                }
        }
    }

    private fun onLoginSuccess(server: String, user: String, password: String, account: Account) {
        prefs.save(server, user, password, account.expiresAt)
        if (editMode) {
            Toast.makeText(this, R.string.account_saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            openHome()
        }
    }

    private fun setLoading(loading: Boolean) {
        serverField.isEnabled = !loading
        userField.isEnabled = !loading
        passwordField.isEnabled = !loading
        showPassword.isEnabled = !loading
        connectButton.isEnabled = !loading
        connectButton.setText(if (loading) R.string.connecting else if (editMode) R.string.save else R.string.connect)
    }

    private fun showError(message: Int) {
        errorText.setText(message)
        errorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(serverField.windowToken, 0)
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    companion object {
        private const val EXTRA_EDIT = "edit"

        fun startEdit(context: Context) {
            context.startActivity(
                Intent(context, SetupActivity::class.java).putExtra(EXTRA_EDIT, true)
            )
        }
    }
}
