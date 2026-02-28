package com.locked.lockedin.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.locked.lockedin.R
import com.locked.lockedin.data.database.PasswordDatabase
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.security.VaultKeyHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PasswordAutofillService : AutofillService() {

    companion object {
        const val EXTRA_FILL_RESPONSE = "extra_fill_response"
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
            ?: return callback.onSuccess(null)

        val parsed = AutofillHelper.parseStructure(structure)

        if (parsed.passwordNodes.isEmpty() && parsed.usernameNodes.isEmpty()) {
            return callback.onSuccess(null)
        }

        val entries = runBlocking {
            findMatchingEntries(parsed.packageName, parsed.webDomain)
        }

        val responseBuilder = FillResponse.Builder()

        // If the vault is locked, ask for authentication
        if (!VaultKeyHolder.isUnlocked) {
            val authResponse = buildLockedResponse(parsed) ?: return callback.onSuccess(null)
            return callback.onSuccess(authResponse)
        }

        // Add existing matching entries
        if (entries.isEmpty()) {
            val manualDataset = buildManualSearchDataset(parsed)
            if (manualDataset != null) responseBuilder.addDataset(manualDataset)
        } else {
            entries.forEach { entry ->
                val dataset = buildDataset(entry, parsed) ?: return@forEach
                responseBuilder.addDataset(dataset)
            }
        }

        // ── NEW: Offer "Generate secure password" when we detect a registration form ──
        if (isRegistrationForm(parsed)) {
            val generateDataset = buildGeneratePasswordDataset(parsed)
            if (generateDataset != null) responseBuilder.addDataset(generateDataset)
        }

        // Configure SaveInfo so Android offers to save new credentials
        buildSaveInfo(parsed)?.let { responseBuilder.setSaveInfo(it) }

        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess()
            return
        }

        val parsed = AutofillHelper.parseStructure(structure)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                saveNewEntry(parsed, structure)
                callback.onSuccess()
            } catch (e: Exception) {
                callback.onFailure("Error saving: ${e.message}")
            }
        }
    }

    // ──────────────────────────────────────────────
    // NEW: Registration-form detection
    // ──────────────────────────────────────────────

    /**
     * Heuristic: a form is considered "registration" if it has at least
     * one password field. Extra confidence if there are 2+ password nodes
     * (password + confirm) or if the web domain / package has no saved entries.
     */
    private fun isRegistrationForm(parsed: ParsedFields): Boolean {
        // Always offer generation when there's a password field and vault is unlocked
        return parsed.passwordNodes.isNotEmpty()
    }

    // ──────────────────────────────────────────────
    // NEW: Generate-password dataset
    // ──────────────────────────────────────────────

    /**
     * Builds a Dataset that, when tapped, fills every password field with
     * a freshly generated secure password.
     */
    private fun buildGeneratePasswordDataset(parsed: ParsedFields): Dataset? {
        val passwordIds = parsed.passwordNodes.mapNotNull { it.autofillId }
        if (passwordIds.isEmpty()) return null

        val crypto = CryptoManager()
        val generatedPassword = crypto.generateSecurePassword(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )

        val presentation = createPresentation(
            "🔐 Generate secure password",
            "Tap to fill with a strong password"
        )

        val builder = Dataset.Builder(presentation)

        // Fill every password field with the same generated password
        passwordIds.forEach { id ->
            builder.setValue(id, AutofillValue.forText(generatedPassword))
        }

        // Optionally leave username fields untouched (user types their own)
        return builder.build()
    }

    // ──────────────────────────────────────────────
    // UPDATED: saveNewEntry — now actually saves
    // ──────────────────────────────────────────────

    /**
     * Extracts the username & password the user just typed/selected,
     * encrypts the password, and inserts a new [PasswordEntry] into the vault DB.
     */
    private suspend fun saveNewEntry(
        parsed: ParsedFields,
        structure: android.app.assist.AssistStructure
    ) {
        if (!VaultKeyHolder.isUnlocked) return  // Cannot save without the key

        // 1. Extract the plaintext values the user entered
        val username = parsed.usernameNodes
            .firstOrNull()
            ?.text
            ?.toString()
            .orEmpty()

        val plainPassword = parsed.passwordNodes
            .firstOrNull()
            ?.text
            ?.toString()
            ?: return  // Nothing to save if there's no password

        // 2. Determine a human-readable title from the web domain or package
        val title = parsed.webDomain
            ?: parsed.packageName?.split(".")?.takeLast(2)?.joinToString(".")
            ?: "Unknown site"

        val website = parsed.webDomain ?: ""

        // 3. Encrypt the password
        val crypto = CryptoManager()
        val encryptedPassword = crypto.encryptPassword(plainPassword)

        // 4. Persist to the local Room database
        val db = PasswordDatabase.getDatabase(applicationContext)
        val entry = PasswordEntry(
            title = title,
            username = username,
            encryptedPassword = encryptedPassword,
            website = website,
            // If your PasswordEntry has other fields (notes, group, etc.) set defaults:
            // notes = "",
            // groupId = null,
        )
        db.passwordDao().insertPassword(entry)
    }

    // ──────────────────────────────────────────────
    // Existing private helpers (unchanged)
    // ──────────────────────────────────────────────

    private suspend fun findMatchingEntries(
        packageName: String?,
        webDomain: String?
    ): List<PasswordEntry> {
        if (!VaultKeyHolder.isUnlocked) return emptyList()

        val db = PasswordDatabase.getDatabase(applicationContext)
        val allEntries = db.passwordDao().getAllPasswords().first()

        return allEntries.filter { entry ->
            val websiteLower = entry.website.lowercase()
            val titleLower = entry.title.lowercase()

            webDomain?.let { domain ->
                websiteLower.contains(domain.lowercase()) ||
                        domain.lowercase().contains(websiteLower.replace("www.", ""))
            } == true ||
                    packageName?.let { pkg ->
                        val parts = pkg.split(".")
                        parts.any { part ->
                            part.length > 3 && (
                                    titleLower.contains(part) ||
                                            websiteLower.contains(part)
                                    )
                        }
                    } == true
        }.take(5)
    }

    private fun buildDataset(
        entry: PasswordEntry,
        parsed: ParsedFields
    ): Dataset? {
        if (parsed.usernameNodes.isEmpty() && parsed.passwordNodes.isEmpty()) return null

        val decryptedPassword = try {
            CryptoManager().decryptPassword(entry.encryptedPassword)
        } catch (e: Exception) {
            return null
        }

        val presentation = createPresentation(entry.title, entry.username)
        val datasetBuilder = Dataset.Builder(presentation)

        parsed.usernameNodes.forEach { node ->
            node.autofillId?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(entry.username))
            }
        }

        parsed.passwordNodes.forEach { node ->
            node.autofillId?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(decryptedPassword))
            }
        }

        return datasetBuilder.build()
    }

    private fun buildLockedResponse(parsed: ParsedFields): FillResponse? {
        val allIds = (parsed.usernameNodes + parsed.passwordNodes)
            .mapNotNull { it.autofillId }
            .toTypedArray()

        if (allIds.isEmpty()) return null

        val tempResponseBuilder = FillResponse.Builder()

        val authIntent = Intent(this, AutofillAuthActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1001, authIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val presentation = createPresentation("🔒 Vault locked", "Tap to unlock")

        val datasetBuilder = Dataset.Builder(presentation)
            .setAuthentication(pendingIntent.intentSender)

        allIds.forEach { id ->
            datasetBuilder.setValue(id, AutofillValue.forText(""))
        }

        return tempResponseBuilder.addDataset(datasetBuilder.build()).build()
    }

    private fun buildManualSearchDataset(parsed: ParsedFields): Dataset? {
        val allIds = (parsed.usernameNodes + parsed.passwordNodes)
            .mapNotNull { it.autofillId }
        if (allIds.isEmpty()) return null

        val intent = Intent(this, com.locked.lockedin.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1002, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val presentation = createPresentation("🔑 Password Manager", "Search manually...")
        val builder = Dataset.Builder(presentation).setAuthentication(pendingIntent.intentSender)
        allIds.forEach { id -> builder.setValue(id, AutofillValue.forText("")) }

        return builder.build()
    }

    private fun buildSaveInfo(parsed: ParsedFields): SaveInfo? {
        val passwordIds = parsed.passwordNodes.mapNotNull { it.autofillId }.toTypedArray()
        if (passwordIds.isEmpty()) return null

        val usernameIds = parsed.usernameNodes.mapNotNull { it.autofillId }.toTypedArray()
        val requiredIds = if (usernameIds.isNotEmpty()) usernameIds else passwordIds

        return SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, requiredIds)
            .setOptionalIds(passwordIds)
            .build()
    }

    private fun createPresentation(title: String, subtitle: String): RemoteViews {
        return RemoteViews(packageName, R.layout.autofill_list_item).apply {
            setTextViewText(R.id.autofill_item_title, title)
            setTextViewText(R.id.autofill_item_subtitle, subtitle)
        }
    }
}