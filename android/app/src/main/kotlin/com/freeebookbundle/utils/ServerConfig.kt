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

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        prefs = applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getContext(): Context {
        return applicationContext ?: throw IllegalStateException("ServerConfig not initialized. Call initialize() in Application.onCreate()")
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
    }

    fun getServerIp(): String {
        return getPrefs().getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun getServerPort(): String {
        return getPrefs().getString(KEY_SERVER_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }

    fun getServerUrl(): String {
        return "http://${getServerIp()}:${getServerPort()}"
    }

    fun getApiUrl(): String {
        return "http://${getServerIp()}:${getApiPort()}"
    }

    fun getApiPort(): String {
        return getPrefs().getString(KEY_API_PORT, DEFAULT_API_PORT) ?: DEFAULT_API_PORT
    }

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
    }

    fun setServerPort(port: String) {
        getPrefs().edit().putString(KEY_SERVER_PORT, port).apply()
    }

    fun setApiPort(port: String) {
        getPrefs().edit().putString(KEY_API_PORT, port).apply()
    }
}
