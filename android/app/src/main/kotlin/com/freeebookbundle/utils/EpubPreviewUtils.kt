package com.freeebookbundle.utils

import android.util.Log
import com.freeebookbundle.EbookApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.toUrl
import java.io.File
import java.util.zip.ZipFile

object EpubPreviewUtils {

    suspend fun getFirstChapterHtmlReadium(app: EbookApp, epubFile: File): String? = withContext(Dispatchers.IO) {
        var publication: Publication? = null
        try {
            val url = epubFile.toUrl(isDirectory = false)
            val asset = app.assetRetriever.retrieve(url).getOrNull() ?: return@withContext null
            publication = app.publicationOpener.open(asset, allowUserInteraction = false).getOrNull() ?: return@withContext null
            
            // 1. Try to find the first item in the Table of Contents
            // If TOC is empty, fallback to the first item in the Reading Order (Spine)
            val link = publication.tableOfContents.firstOrNull() ?: publication.readingOrder.firstOrNull() ?: return@withContext null
            
            val resource = publication.get(link)
            val content = resource?.read()?.getOrNull() ?: return@withContext null
            
            String(content, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("EpubPreviewUtils", "Error using Readium for preview", e)
            null
        } finally {
            publication?.close()
        }
    }

    fun getFirstChapterHtml(epubFile: File): String? {
        try {
            if (!epubFile.exists()) return null
            val zip = ZipFile(epubFile)
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true)) {
                    val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    if (content.length > 500 && (content.contains("<p", true) || content.contains("<div", true))) {
                        return content
                    }
                }
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }
}
