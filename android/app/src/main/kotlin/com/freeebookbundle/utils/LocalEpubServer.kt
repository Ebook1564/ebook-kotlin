package com.freeebookbundle.utils

import android.content.res.AssetManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class LocalEpubServer(
    private val epubDirectory: java.io.File,
    private val assetManager: AssetManager? = null,
    port: Int = 0
) : NanoHTTPD(port) {

    private val mimeTypes = mapOf(
        "epub" to "application/epub+zip",
        "zip" to "application/zip",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "xml" to "application/xml",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "woff" to "font/woff",
        "woff2" to "font/woff2",
        "ttf" to "font/ttf",
        "otf" to "font/otf"
    )

    @Throws(IOException::class)
    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        Log.d("LocalEpubServer", "Request: ${session.method} $uri")
        
        // URL Decode the URI to handle spaces and special characters
        try {
            uri = java.net.URLDecoder.decode(uri, "UTF-8")
        } catch (e: Exception) {
            Log.e("LocalEpubServer", "Error decoding URI: $uri", e)
        }
        
        // Normalize URI - remove leading slash
        if (uri.startsWith("/")) {
            uri = uri.substring(1)
        }
        
        // Serve reader.html from assets with EPUB URL as query param
        if (uri == "reader.html" || uri == "") {
            val epubUrl = session.parameters["epub"]?.firstOrNull() ?: ""
            val response = serveAssetReader(epubUrl)
            Log.d("LocalEpubServer", "Response: ${response.status} for reader.html")
            return response
        }
        
        // Serve bundle.js from assets
        if (uri == "bundle.js") {
            val response = serveAsset("foliate-js/bundle.js", "application/javascript")
            Log.d("LocalEpubServer", "Response: ${response.status} for bundle.js")
            return response
        }
        
        // Serve foliate-js assets subdirectory
        if (uri.startsWith("foliate-js/")) {
            val assetPath = uri
            val response = serveAsset(assetPath, getMimeType(uri))
            Log.d("LocalEpubServer", "Response: ${response.status} for $uri")
            return response
        }

        // Security: Prevent directory traversal
        if (uri.contains("..") || uri.contains("\\")) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                MIME_PLAINTEXT,
                "Access denied"
            )
        }
        
        // Default to listing or index
        if (uri.isEmpty() || uri == "/") {
            return newFixedLengthResponse(
                Response.Status.OK,
                MIME_HTML,
                "<html><body><h1>Local EPUB Server</h1><p>Use /filename.epub to access files</p></body></html>"
            )
        }
        
        val file = java.io.File(epubDirectory, uri)
        
        // Security: Ensure file is within epub directory
        val canonicalEpubDir = try { epubDirectory.canonicalPath } catch (e: Exception) { epubDirectory.absolutePath }
        val canonicalFile = try {
            file.canonicalPath
        } catch (e: IOException) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                MIME_PLAINTEXT,
                "Access denied"
            )
        }
        
        val response = if (!canonicalFile.startsWith(canonicalEpubDir)) {
            Log.w("LocalEpubServer", "Access denied: $canonicalFile not in $canonicalEpubDir")
            // Try fallback: just check name if canonical fails due to some Android weirdness
            if (!file.absolutePath.startsWith(epubDirectory.absolutePath)) {
                newFixedLengthResponse(
                    Response.Status.FORBIDDEN,
                    MIME_PLAINTEXT,
                    "Access denied"
                )
            } else {
                serveFileOrAsset(uri, file)
            }
        } else {
            serveFileOrAsset(uri, file)
        }
        
        Log.d("LocalEpubServer", "Response: ${response.status} for $uri")
        return response
    }

    private fun serveFileOrAsset(uri: String, file: java.io.File): Response {
        // Check file exists
        if (!file.exists() || !file.isFile) {
            // Last ditch effort: see if it's an asset in foliate-js
            return try {
                serveAsset("foliate-js/$uri", getMimeType(uri))
            } catch (e: Exception) {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "File not found: $uri"
                )
            }
        }
        
        // Get MIME type
        val extension = uri.substringAfterLast('.', "").lowercase()
        val mimeType = mimeTypes[extension] ?: "application/octet-stream"
        
        return serveFile(file, mimeType)
    }
    
    private fun addSecurityHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "http://localhost")
        response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS")
        response.addHeader("X-Content-Type-Options", "nosniff")
        response.addHeader("X-Frame-Options", "DENY")
        response.addHeader("Content-Security-Policy", "default-src 'self' 'unsafe-inline' blob:; img-src 'self' data: blob:;")
    }

    private fun serveAsset(path: String, mimeType: String): Response {
        if (assetManager == null) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Asset manager not available"
            )
        }
        
        return try {
            val inputStream: InputStream = assetManager.open(path)
            val bytes = inputStream.readBytes()
            
            val response = newFixedLengthResponse(
                Response.Status.OK, 
                mimeType, 
                bytes.inputStream(), 
                bytes.size.toLong()
            )
            
            addSecurityHeaders(response)
            response
        } catch (e: IOException) {
            newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Asset not found: $path"
            )
        }
    }
    
    private fun serveAssetReader(epubUrl: String): Response {
        if (assetManager == null) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Asset manager not available"
            )
        }
        
        return try {
            val inputStream: InputStream = assetManager.open("foliate-js/reader.html")
            val content = inputStream.bufferedReader().use { it.readText() }
            
            val response = newFixedLengthResponse(
                Response.Status.OK,
                MIME_HTML,
                content
            )
            addSecurityHeaders(response)
            response
        } catch (e: IOException) {
            newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Reader HTML not found"
            )
        }
    }
    
    private fun serveFile(file: java.io.File, mimeType: String): Response {
        val fileSize = file.length()
        
        return try {
            val fis = FileInputStream(file)
            
            val response = newFixedLengthResponse(
                Response.Status.OK,
                mimeType,
                fis,
                fileSize
            )
            
            addSecurityHeaders(response)
            response.addHeader("Accept-Ranges", "bytes")
            
            response
        } catch (e: IOException) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error reading file: ${e.message}"
            )
        }
    }
    
    private fun getMimeType(uri: String): String {
        val extension = uri.substringAfterLast('.', "").lowercase()
        return mimeTypes[extension] ?: "application/octet-stream"
    }
    
    fun getServerUrl(): String {
        return "http://localhost:$listeningPort"
    }
    
    fun getEpubUrl(epubFileName: String): String {
        // Encode filename for URL
        val encodedName = android.net.Uri.encode(epubFileName)
        return "${getServerUrl()}/$encodedName"
    }
    
    fun getReaderUrl(epubFileName: String): String {
        return "${getServerUrl()}/reader.html?epub=${getEpubUrl(epubFileName)}"
    }
}
