package com.github.livingwithhippos.nullseek.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat.getSystemService

private fun checkIfOnline(context: Context): Boolean {
    val cm = getSystemService(context, ConnectivityManager::class.java)

    val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
