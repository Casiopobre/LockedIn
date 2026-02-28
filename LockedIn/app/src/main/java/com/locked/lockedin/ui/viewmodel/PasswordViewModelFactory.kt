package com.locked.lockedin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locked.lockedin.repository.PasswordRepository
import com.locked.lockedin.security.PwnedCheckManager

class PasswordViewModelFactory(
    private val repository: PasswordRepository,
    private val pwnedCheckManager: PwnedCheckManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PasswordViewModel::class.java)) {
            return PasswordViewModel(repository, pwnedCheckManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}