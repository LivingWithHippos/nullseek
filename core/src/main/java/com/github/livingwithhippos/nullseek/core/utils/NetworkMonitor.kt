package com.github.livingwithhippos.nullseek.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Tells when the device has a working internet connection. */
class NetworkMonitor(
    private val context: Context,
) {
    val isOnline: Flow<Boolean> =
        callbackFlow {
            val cm = getSystemService(context, ConnectivityManager::class.java)
            if (cm == null) {
                trySend(true)
                awaitClose()
                return@callbackFlow
            }
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                        )
                    }

                    override fun onLost(network: Network) {
                        trySend(false)
                    }
                }
            trySend(checkIfOnline(context))
            cm.registerDefaultNetworkCallback(callback)
            awaitClose { cm.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()

    /** Emits when a network becomes available or is validated, even if the device looked online already. */
    val events: Flow<Unit> =
        callbackFlow {
            val cm = getSystemService(context, ConnectivityManager::class.java)
            if (cm == null) {
                awaitClose()
                return@callbackFlow
            }
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(Unit)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) trySend(Unit)
                    }
                }
            cm.registerDefaultNetworkCallback(callback)
            awaitClose { cm.unregisterNetworkCallback(callback) }
        }
}
