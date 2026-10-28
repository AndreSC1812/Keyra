package com.ascdev.keyra.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

data class ParsedFields(
    val usernameIds: List<AutofillId>,
    val passwordIds: List<AutofillId>,
    val packageName: String?,
    val webDomain: String?,
) {
    val isEmpty: Boolean get() = usernameIds.isEmpty() && passwordIds.isEmpty()
}

/** Walks the view hierarchy of the requesting app looking for login fields. */
object AutofillParser {

    private val USERNAME_HINTS = listOf("user", "email", "e-mail", "login", "account", "usuario", "correo")
    private val PASSWORD_HINTS = listOf("password", "passwd", "pass", "contraseña", "pin")

    fun parse(structure: AssistStructure): ParsedFields {
        val usernames = mutableListOf<AutofillId>()
        val passwords = mutableListOf<AutofillId>()
        var webDomain: String? = null

        fun visit(node: AssistStructure.ViewNode) {
            if (webDomain == null && !node.webDomain.isNullOrBlank()) webDomain = node.webDomain
            val id = node.autofillId
            if (id != null && node.autofillType == View.AUTOFILL_TYPE_TEXT) {
                when {
                    node.isPasswordField() -> passwords += id
                    node.isUsernameField() -> usernames += id
                }
            }
            for (index in 0 until node.childCount) visit(node.getChildAt(index))
        }

        for (index in 0 until structure.windowNodeCount) visit(structure.getWindowNodeAt(index).rootViewNode)

        return ParsedFields(
            usernameIds = usernames,
            passwordIds = passwords,
            packageName = structure.activityComponent?.packageName,
            webDomain = webDomain,
        )
    }

    private fun AssistStructure.ViewNode.isPasswordField(): Boolean {
        val hints = autofillHints.orEmpty()
        if (hints.any { it.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) || it.contains("password", true) }) {
            return true
        }
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass == InputType.TYPE_CLASS_TEXT && variation in TEXT_PASSWORD_VARIATIONS) return true
        if (inputClass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            return true
        }
        if (htmlInfo?.attributes?.any { it.first == "type" && it.second == "password" } == true) return true
        return textHints().any { hint -> PASSWORD_HINTS.any { hint.contains(it) } }
    }

    private fun AssistStructure.ViewNode.isUsernameField(): Boolean {
        val hints = autofillHints.orEmpty()
        if (hints.any {
                it.equals(View.AUTOFILL_HINT_USERNAME, true) || it.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, true)
            }
        ) {
            return true
        }
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        ) {
            return true
        }
        return textHints().any { hint -> USERNAME_HINTS.any { hint.contains(it) } }
    }

    private fun AssistStructure.ViewNode.textHints(): List<String> =
        listOfNotNull(hint, idEntry, contentDescription?.toString()).map { it.lowercase() }

    private val TEXT_PASSWORD_VARIATIONS = setOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
    )
}
