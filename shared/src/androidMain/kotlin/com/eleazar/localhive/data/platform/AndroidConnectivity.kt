package com.eleazar.localhive.data.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Follows the phone's default network for the life of the app. */
class AndroidConnectivity(context: Context) : Connectivity {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private val online = MutableStateFlow(manager.activeNetwork?.let(::works) ?: false)
    override val isOnline: StateFlow<Boolean> = online.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                online.value = capabilities.works()
            }

            override fun onLost(network: Network) {
                online.value = false
            }
        })
    }

    private fun works(network: Network): Boolean = manager.getNetworkCapabilities(network)?.works() ?: false

    // Validated: Android has checked it actually reaches the internet, not just that Wi-Fi is up.
    private fun NetworkCapabilities.works(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
