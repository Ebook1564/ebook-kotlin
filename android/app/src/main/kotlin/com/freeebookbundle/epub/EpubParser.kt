package com.freeebookbundle.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class EpubParser {
    companion object {
        private const val TAG = "EpubParser"
        private const val MAX_ITEM_CHARS = 1000
    }

    // ZipFile is not thread-safe — guard all access with a mutex.
    private val zipMutex = Mutex()
    private var cachedFilePath: String? = null
    private var cachedZip: ZipFile? = null

    // Properly configured factory for XML builders
    private val docFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isValidating = false
        try {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        } catch (e: Exception) {
            Log.w(TAG, "XML features not supported: ${e.message}")
        }
    }

    // Must be called inside zipMutex
    private fun getZipLocked(filePath: String): ZipFile {
        if (cachedFilePath != filePath || cachedZip == null) {
            cachedZip?.close()
            cachedZip = ZipFile(filePath)
            cachedFilePath = filePath
        }
        return cachedZip!!
    }

    suspend fun createEpubBook(filePath: String): EpubBook = withContext(Dispatchers.IO) {
        zipMutex.withLock {
            val zip = getZipLocked(filePath)

            // 1. Container → OPF path
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: throw Exception("Not an epub")
            val containerBytes = zip.getInputStream(containerEntry).use { it.readBytes() }
            val containerDoc = parseXmlLocked(containerBytes) ?: throw Exception("Invalid container")
            val rootFile = containerDoc.getElementsByTagName("rootfile").item(0)
                as? org.w3c.dom.Element
            val opfPath = rootFile?.getAttribute("full-path")
                ?: throw Exception("OPF not found")

            val opfEntry = zip.getEntry(opfPath) ?: throw Exception("OPF missing: $opfPath")
            val opfBytes = zip.getInputStream(opfEntry).use { it.readBytes() }
            val opfDoc = parseXmlLocked(opfBytes) ?: throw Exception("Invalid OPF")

            val basePath = File(opfPath).parent?.let { if (it.isEmpty()) "" else "$it/" } ?: ""

            // 2. Metadata
            val title = opfDoc.getElementsByTagName("dc:title")
                .let { if (it.length > 0) it.item(0).textContent else "Unknown" }
            val author = opfDoc.getElementsByTagName("dc:creator")
                .let { if (it.length > 0) it.item(0).textContent else "Unknown Author" }
            val language = opfDoc.getElementsByTagName("dc:language")
                .let { if (it.length > 0) it.item(0).textContent else "en" }

            // 3. Manifest
            val manifest = mutableMapOf<String, String>()
            var tocHref: String? = null
            val manifestItems = opfDoc.getElementsByTagName("item")
            for (i in 0 until manifestItems.length) {
                val item = manifestItems.item(i) as org.w3c.dom.Element
                val id = item.getAttribute("id")
                val href = item.getAttribute("href")
                val mediaType = item.getAttribute("media-type")
                if (id.isNotEmpty() && href.isNotEmpty()) manifest[id] = href
                if (mediaType == "application/x-dtbncx+xml" || id == "ncx" || id == "toc") {
                    tocHref = href
                }
            }

            // 4. Spine
            val itemrefs = opfDoc.getElementsByTagName("itemref")
            val spineHrefs = mutableListOf<String>()
            for (i in 0 until itemrefs.length) {
                val idref = (itemrefs.item(i) as org.w3c.dom.Element).getAttribute("idref")
                manifest[idref]?.let { spineHrefs.add(it) }
            }

            // 5. TOC
            val chapters = mutableListOf<EpubChapter>()
            if (tocHref != null) {
                try {
                    val fullTocPath = resolvePath(basePath, tocHref)
                    zip.getEntry(fullTocPath)?.let { entry ->
                        val tocBytes = zip.getInputStream(entry).use { it.readBytes() }
                        val tocDoc = parseXmlLocked(tocBytes)
                        val navPoints = tocDoc?.getElementsByTagName("navPoint")
                        if (navPoints != null) {
                            for (j in 0 until navPoints.length) {
                                val point = navPoints.item(j) as org.w3c.dom.Element
                                val label = point.getElementsByTagName("text")
                                    .item(0)?.textContent?.trim() ?: continue
                                val content = point.getElementsByTagName("content")
                                    .item(0) as? org.w3c.dom.Element
                                val fullSrc = content?.getAttribute("src") ?: continue

                                val href = fullSrc.substringBefore("#")
                                val fragment = if (fullSrc.contains("#"))
                                    fullSrc.substringAfter("#") else null

                                val lowerLabel = label.lowercase()
                                if (lowerLabel.contains("original size") ||
                                    lowerLabel.contains("medium-size") ||
                                    lowerLabel.contains("large-size") ||
                                    lowerLabel.contains("gutenberg license")) continue

                                val spineIndex = spineHrefs.indexOf(href)
                                if (spineIndex != -1) {
                                    chapters.add(EpubChapter(
                                        chapterIndex = spineIndex,
                                        title = label,
                                        href = href,
                                        fragmentId = fragment
                                    ))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing TOC: ${e.message}")
                }
            }

            if (chapters.isEmpty()) {
                spineHrefs.forEachIndexed { i, href ->
                    chapters.add(EpubChapter(i, "Chapter ${i + 1}", href))
                }
            }

            // 6. Total characters
            var totalChars = 0L
            spineHrefs.forEach { href ->
                totalChars += zip.getEntry(resolvePath(basePath, href))?.size ?: 0L
            }

            // 7. Cover
            var coverImage: Bitmap? = null
            try {
                val metas = opfDoc.getElementsByTagName("meta")
                var coverId: String? = null
                for (i in 0 until metas.length) {
                    val meta = metas.item(i) as org.w3c.dom.Element
                    if (meta.getAttribute("name") == "cover") {
                        coverId = meta.getAttribute("content"); break
                    }
                }
                val coverHref = coverId?.let { manifest[it] }
                    ?: manifest.values.find {
                        it.contains("cover", ignoreCase = true) &&
                        it.contains("image", ignoreCase = true)
                    }
                if (coverHref != null) {
                    val entryPath = resolvePath(basePath, coverHref)
                    zip.getEntry(entryPath)?.let { entry ->
                        zip.getInputStream(entry).use { coverImage = BitmapFactory.decodeStream(it) }
                    }
                }
            } catch (e: Exception) { /* cover is optional */ }

            EpubBook(
                fileName = File(filePath).name,
                title = title,
                author = author,
                language = language,
                coverImage = coverImage,
                chapters = chapters,
                filePath = filePath,
                basePath = basePath,
                totalCharacters = totalChars
            )
        }
    }

    // getChapterItems reads HTML from the zip — must hold zipMutex for the read
    suspend fun getChapterItems(book: EpubBook, chapterIndex: Int): List<ReaderItem> =
        withContext(Dispatchers.IO) {
            val chapter = book.chapters.getOrNull(chapterIndex)
                ?: return@withContext emptyList()

            // Read compressed bytes under the lock
            val html = zipMutex.withLock {
                val zip = getZipLocked(book.filePath)
                val entryPath = resolvePath(book.basePath, chapter.href)
                val entry = zip.getEntry(entryPath)
                
                if (entry == null) {
                    Log.e(TAG, "Entry not found: $entryPath (base=${book.basePath}, href=${chapter.href})")
                    return@withContext emptyList<ReaderItem>()
                }
                
                zip.getInputStream(entry).use { it.bufferedReader(Charsets.UTF_8).readText() }
            }

            if (html.isBlank()) {
                Log.w(TAG, "Empty HTML content for chapter ${chapter.href}")
                return@withContext emptyList<ReaderItem>()
            }

            val doc = Jsoup.parse(html)
            val body = doc.body()

            val rawItems = mutableListOf<ReaderItem>()
            var currentCharOffset = 0

            body.childNodes().forEach { node ->
                currentCharOffset = processNode(
                    node, rawItems, book.basePath, chapter.href, chapterIndex, currentCharOffset
                )
            }

            Log.d(TAG, "Parsed chapter $chapterIndex: ${rawItems.size} raw items found")

            val result = ArrayList<ReaderItem>(rawItems.size)
            rawItems.forEach { item ->
                if (item is ReaderItem.Text && item.text.length > MAX_ITEM_CHARS) {
                    result.addAll(splitLongItem(item))
                } else {
                    result.add(item)
                }
            }
            
            Log.d(TAG, "Final chapter $chapterIndex: ${result.size} optimized items")
            result
        }

    private fun splitLongItem(item: ReaderItem.Text): List<ReaderItem.Text> {
        if (item.text.length <= MAX_ITEM_CHARS) return listOf(item)

        val chunks = mutableListOf<ReaderItem.Text>()
        var offset = 0
        var chunkIndex = 0
        val text = item.text.text

        while (offset < text.length) {
            val end = (offset + MAX_ITEM_CHARS).coerceAtMost(text.length)
            val breakPoint = if (end >= text.length) end else {
                val sentenceEnd = text.lastIndexOf(". ", end)
                if (sentenceEnd > offset + MAX_ITEM_CHARS / 2) sentenceEnd + 2 else end
            }
            
            chunks.add(ReaderItem.Text(
                id = "${item.id}_chunk$chunkIndex",
                text = AnnotatedString(text.substring(offset, breakPoint)),
                charOffsetInChapter = item.charOffsetInChapter + offset,
                textAlign = item.textAlign,
                locator = if (chunkIndex == 0) item.locator else null
            ))
            offset = breakPoint
            chunkIndex++
        }
        return chunks
    }

    private fun processNode(
        node: org.jsoup.nodes.Node,
        items: MutableList<ReaderItem>,
        basePath: String,
        chapterHref: String,
        chapterIndex: Int,
        currentOffset: Int
    ): Int {
        var offset = currentOffset

        if (node is TextNode) {
            val text = node.getWholeText()
            if (text.isNotBlank()) {
                val annotated = AnnotatedString(text)
                val locator = "${node.parent()?.nodeName() ?: "root"}[${node.siblingIndex()}]"
                items.add(ReaderItem.Text(
                    "${chapterIndex}_item_${items.size}", annotated, offset, null, locator
                ))
                offset += annotated.length
            }
            return offset
        }

        if (node is Element) {
            val styleAttr = node.attr("style")
            val textAlign = when {
                styleAttr.contains("text-align: center", ignoreCase = true) -> "center"
                styleAttr.contains("text-align: right", ignoreCase = true) -> "right"
                styleAttr.contains("text-align: justify", ignoreCase = true) -> "justify"
                node.hasClass("center") || node.attr("align") == "center" -> "center"
                else -> null
            }

            when (node.tagName()) {
                "p", "div", "section", "blockquote", "li", "span" -> {
                    if (node.children().all { it.isInline }) {
                        val annotated = parseStyledText(node)
                        if (annotated.isNotBlank()) {
                            val id = node.id()
                            val locator = if (id.isNotEmpty()) id
                                          else "${node.tagName()}[${node.siblingIndex()}]"
                            items.add(ReaderItem.Text(
                                "${chapterIndex}_item_${items.size}", annotated,
                                offset, textAlign, locator
                            ))
                            offset += annotated.length
                        }
                    } else {
                        node.childNodes().forEach { child ->
                            offset = processNode(child, items, basePath, chapterHref,
                                chapterIndex, offset)
                        }
                    }
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = node.tagName().substring(1).toIntOrNull() ?: 2
                    val text = node.text()
                    val id = node.id()
                    val locator = if (id.isNotEmpty()) id else "h${level}_${items.size}"
                    items.add(ReaderItem.Heading(
                        "${chapterIndex}_item_${items.size}", text, level, locator
                    ))
                    offset += text.length
                }
                "img" -> {
                    val src = node.attr("src")
                    if (src.isNotEmpty()) {
                        val folderPath = File(resolvePath(basePath, chapterHref)).parent ?: ""
                        val absPath = resolvePath(
                            if (folderPath == ".") "" else "$folderPath/", src
                        )
                        items.add(ReaderItem.Image("${chapterIndex}_item_${items.size}", absPath))
                    }
                }
                "image", "svg" -> {
                    val image = node.select("image").first()
                    val src = image?.attr("xlink:href") ?: node.attr("src")
                    if (src.isNotEmpty()) {
                        val folderPath = File(resolvePath(basePath, chapterHref)).parent ?: ""
                        val absPath = resolvePath(
                            if (folderPath == ".") "" else "$folderPath/", src
                        )
                        items.add(ReaderItem.Image("${chapterIndex}_item_${items.size}", absPath))
                    }
                }
                else -> node.childNodes().forEach { child ->
                    offset = processNode(child, items, basePath, chapterHref, chapterIndex, offset)
                }
            }
        }
        return offset
    }

    private val Element.isInline: Boolean
        get() = tagName() in setOf("span", "em", "i", "b", "strong", "a", "u",
            "font", "small", "big", "br")

    private fun parseStyledText(element: Element): AnnotatedString = buildAnnotatedString {
        parseNodeToAnnotated(element, this)
    }

    private fun parseNodeToAnnotated(
        node: org.jsoup.nodes.Node,
        builder: AnnotatedString.Builder
    ) {
        when (node) {
            is TextNode -> builder.append(node.getWholeText())
            is Element -> {
                val style = when (node.tagName()) {
                    "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                    "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    "code", "pre", "tt" -> SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = Color.LightGray.copy(alpha = 0.2f)
                    )
                    "big" -> SpanStyle(fontSize = 1.2.sp)
                    "small" -> SpanStyle(fontSize = 0.8.sp)
                    else -> null
                }
                if (style != null) {
                    builder.withStyle(style) {
                        node.childNodes().forEach { parseNodeToAnnotated(it, builder) }
                    }
                } else {
                    node.childNodes().forEach { parseNodeToAnnotated(it, builder) }
                }
            }
        }
    }

    suspend fun getImage(filePath: String, absPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bytes = zipMutex.withLock {
                    val zip = getZipLocked(filePath)
                    val entry = zip.getEntry(absPath) ?: return@withLock null
                    zip.getInputStream(entry).use { it.readBytes() }
                }
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } catch (e: Exception) { null }
        }

    private fun resolvePath(base: String, href: String): String {
        val cleanHref = href.removePrefix("/")
        val combined = when {
            base.isEmpty() -> cleanHref
            base.endsWith("/") -> "$base$cleanHref"
            else -> "$base/$cleanHref"
        }
        val parts = combined.split("/")
        val resolvedParts = ArrayList<String>(parts.size)
        for (part in parts) {
            when (part) {
                "." -> {}
                ".." -> {
                    if (resolvedParts.isNotEmpty()) resolvedParts.removeAt(resolvedParts.size - 1)
                    else Log.w(TAG, "Security: path traversal blocked in: $combined")
                }
                else -> if (part.isNotEmpty()) resolvedParts.add(part)
            }
        }
        return resolvedParts.joinToString("/")
    }

    private fun parseXmlLocked(data: ByteArray): org.w3c.dom.Document? {
        return try {
            val builder = docFactory.newDocumentBuilder()
            builder.parse(java.io.ByteArrayInputStream(data))
        } catch (e: Exception) {
            Log.e(TAG, "XML parsing failed", e)
            null
        }
    }

    fun close() {
        cachedZip?.close()
        cachedZip = null
        cachedFilePath = null
    }
}
