package com.grouptwo.lokcet.view.register

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.grouptwo.lokcet.R
import com.grouptwo.lokcet.di.service.AccountService
import com.grouptwo.lokcet.di.service.LocationService
import com.grouptwo.lokcet.navigation.Screen
import com.grouptwo.lokcet.ui.component.global.snackbar.SnackbarManager
import com.grouptwo.lokcet.utils.isValidEmail
import com.grouptwo.lokcet.utils.isValidName
import com.grouptwo.lokcet.utils.isValidPassword
import com.grouptwo.lokcet.utils.isValidPhoneNumber
import com.grouptwo.lokcet.view.LokcetViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val accountService: AccountService,
    private val locationService: LocationService,
    private val savedStateHandle: SavedStateHandle,
) : LokcetViewModel() {
    // Initialize the state of the register screen
    var uiState = mutableStateOf(
        RegisterUiState(
            email = savedStateHandle["email"] ?: "",
            password = savedStateHandle["password"] ?: "",
            lastName = savedStateHandle["lastName"] ?: "",
            firstName = savedStateHandle["firstName"] ?: "",
            phoneNumber = savedStateHandle["phoneNumber"] ?: "",
        )
    )
        private set

    private val email get() = uiState.value.email
    private val password get() = uiState.value.password
    private val lastName get() = uiState.value.lastName
    private val firstName get() = uiState.value.firstName
    private val location get() = uiState.value.location
    private val phoneNumber get() = uiState.value.phoneNumber
    private val isButtonEmailEnable get() = uiState.value.isButtonEmailEnable
    private val isButtonPasswordEnable get() = uiState.value.isButtonPasswordEnable
    private val isButtonNameEnable get() = uiState.value.isButtonNameEnable
    private val isButtonPhoneEnable get() = uiState.value.isButtonPhoneEnable
    private val isCheckingEmail get() = uiState.value.isCheckingEmail
    private val isCheckingPhone get() = uiState.value.isCheckingPhone


    fun onEmailChange(email: String) {
        uiState.value = uiState.value.copy(email = email, isButtonEmailEnable = email.isNotBlank())
        savedStateHandle["email"] = email
    }

    fun onPasswordChange(password: String) {
        uiState.value =
            uiState.value.copy(password = password, isButtonPasswordEnable = password.isNotBlank())
        savedStateHandle["password"] = password
    }

    fun onLastNameChange(lastName: String) {
        uiState.value = uiState.value.copy(
            lastName = lastName,
            isButtonNameEnable = lastName.isNotBlank() && firstName.isNotBlank()
        )
        savedStateHandle["lastName"] = lastName
    }

    fun onFirstNameChange(firstName: String) {
        uiState.value = uiState.value.copy(
            firstName = firstName,
            isButtonNameEnable = lastName.isNotBlank() && firstName.isNotBlank()
        )
        savedStateHandle["firstName"] = firstName
    }

    fun onBackClick(popUp: () -> Unit) {
        popUp()
    }

    fun onPhoneNumberChange(phoneNumber: String) {

        uiState.value = uiState.value.copy(
            phoneNumber = phoneNumber,
            isButtonPhoneEnable = phoneNumber.isNotBlank()
        )
        savedStateHandle["phoneNumber"] = phoneNumber
    }

    fun onMailClick(navigate: (String) -> Unit) {
        // If email is empty then do nothing
        if (!isButtonEmailEnable) {
            return
        }

        if (!email.isValidEmail()) {
            SnackbarManager.showMessage(R.string.email_invalid)
            return
        }

        launchCatching {
            try {
                uiState.value = uiState.value.copy(isCheckingEmail = true)
                // Check if the email is already used
                if (accountService.isEmailUsed(email)) {
                    SnackbarManager.showMessage(R.string.email_used)
                } else {
                    // Navigate to the next screen if the email is valid
                    navigate(Screen.RegisterScreen_2.route)
                }
            } finally {
                uiState.value = uiState.value.copy(isCheckingEmail = false)
            }
        }
    }

    fun onPasswordClick(navigate: (String) -> Unit) {
        if (!isButtonPasswordEnable) {
            return
        }
        if (!password.isValidPassword()) {
            SnackbarManager.showMessage(R.string.password_invalid)
            return
        }
        // Navigate to the next screen if the password is valid
        navigate(Screen.RegisterScreen_3.route)
    }

    fun onNameClick(navigate: (String) -> Unit) {
        if (!isButtonNameEnable) {
            return
        }
        if (!lastName.isValidName() || !firstName.isValidName()) {
            SnackbarManager.showMessage(R.string.name_invalid)
            return
        }
        // Call location service to get the current location
        launchCatching {
            uiState.value = uiState.value.copy(location = locationService.getCurrentLocation())
            // After getting the location, navigate to the next screen
            navigate(Screen.RegisterScreen_4.route)
        }
    }

    fun onPhoneNumberClick() {
        if (!isButtonPhoneEnable) {
            return
        }
        if (!phoneNumber.isValidPhoneNumber()) {
            SnackbarManager.showMessage(R.string.phone_invalid)
            return
        }
        launchCatching {
            try {
                uiState.value = uiState.value.copy(isCheckingPhone = true)
                // Check if the email is already used
                if (accountService.isPhoneUsed(phoneNumber)) {
                    SnackbarManager.showMessage(R.string.phone_used)
                } else {
                    Log.d("RegisterViewModel", "Phone number: $phoneNumber")
                    // Call create account function
                    accountService.createAccount(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        location = location,
                        phoneNumber = phoneNumber
                    )
                    // Navigate to the next screen if the email is valid
                    Log.d("RegisterViewModel", "Navigate to the next screen")
//                    navigate(Screen.RegisterScreen_2.route)
                }
            } finally {
                uiState.value = uiState.value.copy(isCheckingPhone = false)
            }
        }

    }
}