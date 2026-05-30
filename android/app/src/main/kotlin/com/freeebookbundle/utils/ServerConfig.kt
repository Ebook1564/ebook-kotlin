package com.freeebookbundle.utils

import android.content.Context
import android.content.SharedPreferences

object ServerConfig {
    private const val PREFS_NAME = "server_config"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_API_PORT = "api_port"
    private const val DEFAULT_IP = "54.254.205.11"
    private const val DEFAULT_PORT = "8080"
    private const val DEFAULT_API_PORT = "8000"

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var applicationContext: Context? = null

    // Cached values — read once at initialize(), avoid repeated SharedPreferences locks
    @Volatile private var cachedIp: String = DEFAULT_IP
    @Volatile private var cachedPort: String = DEFAULT_PORT
    @Volatile private var cachedApiPort: String = DEFAULT_API_PORT

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        prefs = applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedIp = prefs?.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
        cachedPort = prefs?.getString(KEY_SERVER_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        cachedApiPort = prefs?.getString(KEY_API_PORT, DEFAULT_API_PORT) ?: DEFAULT_API_PORT
    }

    private fun getContext(): Context {
        return applicationContext ?: throw IllegalStateException("ServerConfig not initialized. Call initialize() in Application.onCreate()")
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
    }

    fun getServerIp(): String = cachedIp

    fun getServerPort(): String = cachedPort

    fun getServerUrl(): String = "http://$cachedIp:$cachedPort"

    fun getApiUrl(): String = "http://$cachedIp:$cachedApiPort"

    fun getApiPort(): String = cachedApiPort

    fun getCoversUrl(): String {
        return "${getServerUrl()}/covers"
    }

    fun getTtsUrl(): String {
        return "http://${getServerIp()}:5000"
    }

    fun getCatalogUrl(): String {
        return "${getServerUrl()}/catalog.json"
    }

    fun setServerIp(ip: String) {
        getPrefs().edit().putString(KEY_SERVER_IP, ip).apply()
        cachedIp = ip
    }

    fun setServerPort(port: String) {
        getPrefs().edit().putString(KEY_SERVER_PORT, port).apply()
        cachedPort = port
    }

    fun setApiPort(port: String) {
        getPrefs().edit().putString(KEY_API_PORT, port).apply()
        cachedApiPort = port
    }
}
