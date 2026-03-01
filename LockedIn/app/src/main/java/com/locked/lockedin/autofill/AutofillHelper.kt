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

        // 1. autofillHints (más fiable)
        node.autofillHints?.let { hints ->
            if (hints.any { it.contains(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) })
                return true
        }

        // 2. inputType
        val inputType = node.inputType
        if (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) return true
        if (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0) return true
        if (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0) return true
        if (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0) return true  // ← "mostrar contraseña"

        // 3. HTML atributos (muy útil en WebViews y Chrome)
        val htmlAttrs = listOf(
            node.htmlInfo?.tag,
            node.htmlInfo?.attributes?.find { it.first == "type" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "name" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "id" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "autocomplete" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "placeholder" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "aria-label" }?.second,
        ).filterNotNull().map { it.lowercase() }

        // type="password" es la señal más directa en HTML
        if (htmlAttrs.any { it == "password" }) return true

        val passwordKeywords = listOf(
            "password", "passwd", "pass", "pwd", "contraseña",
            "clave", "secret", "pin", "código", "code",
            "current-password", "new-password"
        )
        if (htmlAttrs.any { attr -> passwordKeywords.any { kw -> attr.contains(kw) } }) return true

        // 4. Hint, contentDescription, text del nodo
        val textSources = listOf(
            node.hint,
            node.contentDescription?.toString(),
            node.text?.toString(),
            node.idEntry  // resource-id del View (ej. "et_password", "input_pwd")
        ).filterNotNull().map { it.lowercase() }

        if (textSources.any { src -> passwordKeywords.any { kw -> src.contains(kw) } }) return true

        // 5. className (ej. android.widget.EditText con transformación activa)
        // Si el texto está siendo transformado (••••) casi seguro es contraseña
        if (node.text != null && isTextTransformed(node)) return true

        return false
    }

    private fun isUsernameField(node: ViewNode): Boolean {

        // 1. autofillHints
        node.autofillHints?.let { hints ->
            if (hints.any {
                    it.contains(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                            it.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true) ||
                            it.contains(View.AUTOFILL_HINT_PHONE, ignoreCase = true)
                }) return true
        }

        // 2. inputType
        val inputType = node.inputType
        if (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0) return true
        if (inputType and InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS != 0) return true
        if (inputType and InputType.TYPE_CLASS_PHONE != 0) return true  // ← login por teléfono

        // 3. HTML atributos
        val htmlAttrs = listOf(
            node.htmlInfo?.attributes?.find { it.first == "type" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "name" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "id" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "autocomplete" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "placeholder" }?.second,
            node.htmlInfo?.attributes?.find { it.first == "aria-label" }?.second,
        ).filterNotNull().map { it.lowercase() }

        val usernameKeywords = listOf(
            "username", "user", "usuario", "email", "e-mail",
            "correo", "login", "account", "phone", "teléfono",
            "telefono", "mobile", "identifier", "handle",
            "nickname", "nick", "nombre", "name"
        )
        if (htmlAttrs.any { attr -> usernameKeywords.any { kw -> attr.contains(kw) } }) return true

        // 4. Hint, contentDescription, idEntry
        val textSources = listOf(
            node.hint,
            node.contentDescription?.toString(),
            node.text?.toString(),
            node.idEntry
        ).filterNotNull().map { it.lowercase() }

        if (textSources.any { src -> usernameKeywords.any { kw -> src.contains(kw) } }) return true

        // 5. type="email" en HTML → siempre es campo de usuario
        if (htmlAttrs.any { it == "email" || it == "tel" }) return true

        return false
    }

    // Detecta si el texto visible está enmascarado (●●●●)
    // Compara longitud del texto con el número de caracteres de transformación
    private fun isTextTransformed(node: ViewNode): Boolean {
        val text = node.text?.toString() ?: return false
        if (text.isEmpty()) return false
        // Los campos de contraseña suelen mostrar bullets u otros caracteres únicos
        val uniqueChars = text.toSet()
        return uniqueChars.size == 1 && text.length > 1
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