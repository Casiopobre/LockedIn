package com.locked.lockedin.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.locked.lockedin.R
import com.locked.lockedin.data.database.PasswordDatabase
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.repository.PasswordRepository
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.security.VaultKeyHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PasswordAutofillService : AutofillService() {

    private lateinit var repository: PasswordRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val EXTRA_FILL_RESPONSE = "extra_fill_response"
    }

    override fun onCreate() {
        super.onCreate()
        val db = PasswordDatabase.getDatabase(this)
        repository = PasswordRepository(db.passwordDao(), CryptoManager())
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // 1. Obtener la estructura de la pantalla actual
        val structure = request.fillContexts.lastOrNull()?.structure
            ?: return callback.onSuccess(null)

        // 2. Parsear los campos de la pantalla
        val parsed = AutofillHelper.parseStructure(structure)

        // Si no hay campos de contraseña, no hacemos nada
        if (parsed.passwordNodes.isEmpty() && parsed.usernameNodes.isEmpty()) {
            return callback.onSuccess(null)
        }

        // 3. Buscar contraseñas relevantes en la base de datos
        val entries = runBlocking {
            findMatchingEntries(parsed.packageName, parsed.webDomain)
        }

        // 4. Construir el FillResponse
        val responseBuilder = FillResponse.Builder()

        // 4a. Si el vault está bloqueado → pedir autenticación
        if (!VaultKeyHolder.isUnlocked) {
            val authResponse = buildLockedResponse(parsed) ?: return callback.onSuccess(null)
            return callback.onSuccess(authResponse)
        }

        // 4b. Vault desbloqueado → añadir datasets
        if (entries.isEmpty()) {
            // Sin coincidencias: ofrecer opción de buscar manualmente
            val manualDataset = buildManualSearchDataset(parsed)
            if (manualDataset != null) responseBuilder.addDataset(manualDataset)
        } else {
            entries.forEach { entry ->
                val dataset = buildDataset(entry, parsed) ?: return@forEach
                responseBuilder.addDataset(dataset)
            }
        }

        // 4c. Configurar SaveInfo para guardar nuevas contraseñas
        buildSaveInfo(parsed)?.let { responseBuilder.setSaveInfo(it) }

        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: return callback.onFailure("No structure")
        val parsed = AutofillHelper.parseStructure(structure)

        serviceScope.launch {
            try {
                saveNewEntry(parsed, structure)
                callback.onSuccess()
            } catch (e: Exception) {
                callback.onFailure(e.message)
            }
        }
    }

    // ──────────────────────────────────────────────
    // Funciones privadas auxiliares
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
        }.take(5)                                    // ✅ take sobre List funciona
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
        // Construimos un FillResponse "vacío" que al seleccionarlo abre la auth
        val allIds = (parsed.usernameNodes + parsed.passwordNodes)
            .mapNotNull { it.autofillId }
            .toTypedArray()

        if (allIds.isEmpty()) return null

        // Creamos un FillResponse temporal con los campos sin valor
        // El usuario deberá autenticarse para ver las contraseñas
        val tempResponseBuilder = FillResponse.Builder()

        val authIntent = Intent(this, AutofillAuthActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1001, authIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val presentation = createPresentation("LockedIn bloqueado", "Toca para desbloquear")

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

        val presentation = createPresentation("No hay contraseñas", "Toca para buscar manualmente")
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
        // RemoteViews es necesario para mostrar sugerencias fuera de tu app
        return RemoteViews(packageName, R.layout.autofill_list_item).apply {
            setTextViewText(R.id.autofill_item_title, title)
            setTextViewText(R.id.autofill_item_subtitle, subtitle)
        }
    }

    private suspend fun saveNewEntry(parsed: ParsedFields, structure: AssistStructure) {
        // Si el vault está bloqueado, mejor no intentar guardar (o pedir desbloqueo)
        if (!VaultKeyHolder.isUnlocked) return

        val usernameId = parsed.usernameNodes.firstOrNull()?.autofillId
        val passwordId = parsed.passwordNodes.firstOrNull()?.autofillId

        val username = usernameId?.let { AutofillHelper.getValueFromNode(structure, it) } ?: ""
        val password = passwordId?.let { AutofillHelper.getValueFromNode(structure, it) } ?: ""

        if (password.isNotBlank()) {
            val title = parsed.packageName?.substringAfterLast('.') ?: "Nueva entrada"
            repository.insertPassword(
                title = title.replaceFirstChar { it.uppercase() },
                username = username,
                password = password,
                website = parsed.webDomain ?: parsed.packageName ?: ""
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}