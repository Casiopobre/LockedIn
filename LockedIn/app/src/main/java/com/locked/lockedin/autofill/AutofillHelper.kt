package com.locked.lockedin.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.text.InputType
import android.view.View

data class ParsedFields(
    val usernameNodes: List<ViewNode>,
    val passwordNodes: List<ViewNode>,
    val packageName: String?,
    val webDomain: String?
)

object AutofillHelper {

    fun parseStructure(structure: AssistStructure): ParsedFields {
        val usernameNodes = mutableListOf<ViewNode>()
        val passwordNodes = mutableListOf<ViewNode>()
        var webDomain: String? = null

        // Recorre recursivamente el árbol de vistas
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, usernameNodes, passwordNodes)
            if (webDomain == null) {
                webDomain = extractWebDomain(windowNode.rootViewNode)
            }
        }

        return ParsedFields(
            usernameNodes = usernameNodes,
            passwordNodes = passwordNodes,
            packageName = structure.activityComponent?.packageName,
            webDomain = webDomain
        )
    }

    private fun traverseNode(
        node: ViewNode,
        usernameNodes: MutableList<ViewNode>,
        passwordNodes: MutableList<ViewNode>
    ) {
        if (isPasswordField(node)) {
            passwordNodes.add(node)
        } else if (isUsernameField(node)) {
            usernameNodes.add(node)
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i)!!, usernameNodes, passwordNodes)
        }
    }

    private fun isPasswordField(node: ViewNode): Boolean {
        // Detecta por autofillHints (lo más fiable)
        val hints = node.autofillHints
        if (hints != null && hints.any {
                it.contains(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true)
            }) return true

        // Detecta por inputType
        val inputType = node.inputType
        if (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) return true
        if (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0) return true
        if (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0) return true

        // Detecta por hint/texto del campo
        val hint = node.hint?.lowercase() ?: ""
        return hint.contains("password") || hint.contains("contraseña") || hint.contains("passwd")
    }

    private fun isUsernameField(node: ViewNode): Boolean {
        val hints = node.autofillHints
        if (hints != null && hints.any {
                it.contains(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                        it.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true)
            }) return true

        val inputType = node.inputType
        if (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0) return true
        if (inputType and InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS != 0) return true

        val hint = node.hint?.lowercase() ?: ""
        return hint.contains("user") || hint.contains("email") ||
                hint.contains("usuario") || hint.contains("correo") ||
                hint.contains("login")
    }

    private fun extractWebDomain(node: ViewNode): String? {
        if (!node.webDomain.isNullOrEmpty()) return node.webDomain
        for (i in 0 until node.childCount) {
            val result = extractWebDomain(node.getChildAt(i)!!)
            if (result != null) return result
        }
        return null
    }
}