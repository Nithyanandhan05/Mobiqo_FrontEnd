package com.simats.smartelectroai.utils

object ValidationUtils {
    fun isValidFullName(name: String): Boolean {
        return name.matches(Regex("^[A-Za-z ]+$")) && name.isNotBlank()
    }

    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9._%+-]+@(gmail\\.com|email\\.com|saveetha\\.com)$"))
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]{10}$"))
    }

    fun isValidPassword(password: String): Boolean {
        return password.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$"))
    }

    fun calculatePasswordStrength(password: String): Float {
        if (password.isEmpty()) return 0f
        var strength = 0.2f
        if (password.length >= 8) strength += 0.2f
        if (password.any { it.isUpperCase() }) strength += 0.2f
        if (password.any { it.isDigit() }) strength += 0.2f
        if (password.any { !it.isLetterOrDigit() }) strength += 0.2f
        return strength
    }
}