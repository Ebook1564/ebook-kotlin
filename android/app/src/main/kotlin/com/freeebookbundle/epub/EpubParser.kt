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
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File
import java.util.zip.ZipFile

class EpubParser {
    companion object {
        private const val TAG = "EpubParser"
    }

    private var cachedFilePath: String? = null
    private var cachedZip: ZipFile? = null

    private fun getZip(filePath: String): ZipFile {
        if (cachedFilePath != filePath || cachedZip == null) {
            cachedZip?.close()
            cachedZip = ZipFile(filePath)
            cachedFilePath = filePath
        }
        return cachedZip!!
    }

    suspend fun createEpubBook(filePath: String): EpubBook = withContext(Dispatchers.IO) {
        val zip = getZip(filePath)
        
        // 1. Find OPF
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: throw Exception("Not an epub")
        val containerDoc = parseXml(zip.getInputStream(containerEntry).readBytes()) ?: throw Exception("Invalid container")
        val rootFile = containerDoc.getElementsByTagName("rootfile").item(0) as? org.w3c.dom.Element
        val opfPath = rootFile?.getAttribute("full-path") ?: throw Exception("OPF not found")
        
        val opfEntry = zip.getEntry(opfPath) ?: throw Exception("OPF missing: $opfPath")
        val opfDoc = parseXml(zip.getInputStream(opfEntry).readBytes()) ?: throw Exception("Invalid OPF")
        
        val basePath = File(opfPath).parent?.let { "$it/" } ?: ""

        // 2. Metadata
        val titles = opfDoc.getElementsByTagName("dc:title")
        val title = if (titles.length > 0) titles.item(0).textContent else "Unknown"
        val creators = opfDoc.getElementsByTagName("dc:creator")
        val author = if (creators.length > 0) creators.item(0).textContent else "Unknown Author"
        val language = opfDoc.getElementsByTagName("dc:language").let { if (it.length > 0) it.item(0).textContent else "en" }

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
        
        // 4. Spine (The actual content files)
        val itemrefs = opfDoc.getElementsByTagName("itemref")
        val spineHrefs = mutableListOf<String>()
        for (i in 0 until itemrefs.length) {
            val idref = (itemrefs.item(i) as org.w3c.dom.Element).getAttribute("idref")
            manifest[idref]?.let { spineHrefs.add(it) }
        }
        
        // 5. Parse TOC for User Navigation
        val chapters = mutableListOf<EpubChapter>()
        if (tocHref != null) {
            try {
                val fullTocPath = resolvePath(basePath, tocHref)
                zip.getEntry(fullTocPath)?.let { entry ->
                    val tocDoc = parseXml(zip.getInputStream(entry).readBytes())
                    val navPoints = tocDoc?.getElementsByTagName("navPoint")
                    if (navPoints != null) {
                        for (j in 0 until navPoints.length) {
                            val point = navPoints.item(j) as org.w3c.dom.Element
                            val label = point.getElementsByTagName("text").item(0)?.textContent?.trim() ?: continue
                            val content = point.getElementsByTagName("content").item(0) as? org.w3c.dom.Element
                            val fullSrc = content?.getAttribute("src") ?: continue
                            
                            val href = fullSrc.substringBefore("#")
                            val fragment = if (fullSrc.contains("#")) fullSrc.substringAfter("#") else null
                            
                            // NOISE FILTER
                            val lowerLabel = label.lowercase()
                            if (lowerLabel.contains("original size") || 
                                lowerLabel.contains("medium-size") || 
                                lowerLabel.contains("large-size") ||
                                lowerLabel.contains("gutenberg license")) continue

                            // Find which spine index this TOC entry belongs to
                            val spineIndex = spineHrefs.indexOf(href)
                            if (spineIndex != -1) {
                                chapters.add(EpubChapter(
                                    chapterIndex = spineIndex, // The physical file it belongs to
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

        // Fallback: If TOC is empty or missing, use Spine items
        if (chapters.isEmpty()) {
            spineHrefs.forEachIndexed { i, href ->
                chapters.add(EpubChapter(i, "Chapter ${i + 1}", href))
            }
        }

        // 6. Calculate total characters/weights using Spine
        var totalChars = 0L
        spineHrefs.forEach { href ->
            val entryPath = resolvePath(basePath, href)
            totalChars += zip.getEntry(entryPath)?.size ?: 0L
        }

        // 7. Cover

        // 5. Cover
        var coverImage: Bitmap? = null
        try {
            val metas = opfDoc.getElementsByTagName("meta")
            var coverId: String? = null
            for (i in 0 until metas.length) {
                val meta = metas.item(i) as org.w3c.dom.Element
                if (meta.getAttribute("name") == "cover") {
                    coverId = meta.getAttribute("content")
                    break
                }
            }
            val coverHref = coverId?.let { manifest[it] } ?: manifest.values.find { it.contains("cover", true) && it.contains("image", true) }
            if (coverHref != null) {
                val entryPath = resolvePath(basePath, coverHref)
                zip.getEntry(entryPath)?.let { entry ->
                    coverImage = BitmapFactory.decodeStream(zip.getInputStream(entry))
                }
            }
        } catch (e: Exception) {}

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

    suspend fun getChapterItems(book: EpubBook, chapterIndex: Int): List<ReaderItem> = withContext(Dispatchers.IO) {
        val chapter = book.chapters.getOrNull(chapterIndex) ?: return@withContext emptyList()
        val zip = getZip(book.filePath)
        val entryPath = resolvePath(book.basePath, chapter.href)
        val entry = zip.getEntry(entryPath) ?: return@withContext emptyList()
        
        val html = zip.getInputStream(entry).bufferedReader().readText()
        val doc = Jsoup.parse(html)
        val body = doc.body()

        val items = mutableListOf<ReaderItem>()
        var currentCharOffset = 0
        
        body.childNodes().forEach { node ->
            currentCharOffset = processNode(node, items, book.basePath, chapter.href, chapterIndex, currentCharOffset)
        }
        
        items
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
        val itemIndex = items.size
        
        if (node is TextNode) {
            val text: String = node.getWholeText()
            if (text.isNotEmpty()) {
                val annotated = AnnotatedString(text)
                val locator = "${node.parent()?.nodeName() ?: "root"}[${node.siblingIndex()}]"
                // Use a unified, unique ID scheme: chapter_index_item_count
                items.add(ReaderItem.Text("${chapterIndex}_item_${items.size}", annotated, offset, null, locator))
                offset += annotated.length
            }
            return offset
        }

        if (node is Element) {
            val styleAttr = node.attr("style")
            val textAlign = when {
                styleAttr.contains("text-align: center", true) -> "center"
                styleAttr.contains("text-align: right", true) -> "right"
                styleAttr.contains("text-align: justify", true) -> "justify"
                node.hasClass("center") || node.attr("align") == "center" -> "center"
                else -> null
            }

            when (node.tagName()) {
                "p", "div", "section", "blockquote", "li", "span" -> {
                    if (node.children().all { it.isInline }) {
                        val annotated = parseStyledText(node)
                        if (annotated.isNotBlank()) {
                            // Check for ID for anchor jumping
                            val id = node.id()
                            val locator = if (id.isNotEmpty()) id else "${node.tagName()}[${node.siblingIndex()}]"
                            items.add(ReaderItem.Text("${chapterIndex}_item_${items.size}", annotated, offset, textAlign, locator))
                            offset += annotated.length
                        }
                    } else {
                        node.childNodes().forEach { child ->
                            offset = processNode(child, items, basePath, chapterHref, chapterIndex, offset)
                        }
                    }
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = node.tagName().substring(1).toIntOrNull() ?: 2
                    val text = node.text()
                    val id = node.id()
                    val locator = if (id.isNotEmpty()) id else "h${level}_${items.size}"
                    items.add(ReaderItem.Heading("${chapterIndex}_item_${items.size}", text, level))
                    // Add a tiny empty text item if there is an ID so we can scroll to it precisely
                    if (id.isNotEmpty()) {
                        // We use the heading itself as the target for now, 
                        // but ensure its ID is tracked.
                    }
                    offset += text.length
                }
                "img" -> {
                    val src = node.attr("src")
                    if (src.isNotEmpty()) {
                        val folderPath = File(resolvePath(basePath, chapterHref)).parent ?: ""
                        val absPath = resolvePath(if (folderPath == ".") "" else "$folderPath/", src)
                        items.add(ReaderItem.Image("${chapterIndex}_item_${items.size}", absPath))
                    }
                }
                "image", "svg" -> {
                    val image = node.select("image").first()
                    val src = image?.attr("xlink:href") ?: node.attr("src")
                    if (src.isNotEmpty()) {
                        val folderPath = File(resolvePath(basePath, chapterHref)).parent ?: ""
                        val absPath = resolvePath(if (folderPath == ".") "" else "$folderPath/", src)
                        items.add(ReaderItem.Image("${chapterIndex}_item_${items.size}", absPath))
                    }
                }
                else -> {
                    node.childNodes().forEach { child ->
                        offset = processNode(child, items, basePath, chapterHref, chapterIndex, offset)
                    }
                }
            }
        }
        return offset
    }

    private val Element.isInline: Boolean
        get() = tagName() in listOf("span", "em", "i", "b", "strong", "a", "u", "font", "small", "big")

    private fun parseStyledText(element: Element): AnnotatedString {
        return buildAnnotatedString {
            parseNodeToAnnotated(element, this)
        }
    }

    private fun parseNodeToAnnotated(node: org.jsoup.nodes.Node, builder: AnnotatedString.Builder) {
        when (node) {
            is TextNode -> builder.append((node as TextNode).getWholeText())
            is Element -> {
                val style = when (node.tagName()) {
                    "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                    "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    "code", "pre", "tt" -> SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.2f))
                    "big" -> SpanStyle(fontSize = 1.2.times(1f).sp) // Approximation
                    "small" -> SpanStyle(fontSize = 0.8.times(1f).sp)
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

    suspend fun getImage(filePath: String, absPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val zip = getZip(filePath)
            val entry = zip.getEntry(absPath) ?: return@withContext null
            BitmapFactory.decodeStream(zip.getInputStream(entry))
        } catch (e: Exception) { null }
    }

    private fun resolvePath(base: String, href: String): String {
        val cleanHref = href.removePrefix("/")
        val combined = if (base.isEmpty()) cleanHref else if (base.endsWith("/")) "$base$cleanHref" else "$base/$cleanHref"
        
        // --- Zip-Slip Protection ---
        // We normalize the path and ensure it doesn't escape the root
        val parts = combined.split("/")
        val resolvedParts = mutableListOf<String>()
        for (part in parts) {
            when (part) {
                "." -> {}
                ".." -> {
                    if (resolvedParts.isNotEmpty()) {
                        resolvedParts.removeAt(resolvedParts.size - 1)
                    } else {
                        // Attempt to escape the root! Block it.
                        Log.w(TAG, "Security Warning: Path traversal attempt detected in EPUB: $combined")
                    }
                }
                else -> if (part.isNotEmpty()) resolvedParts.add(part)
            }
        }
        return resolvedParts.joinToString("/")
    }

    fun close() {
        cachedZip?.close()
        cachedZip = null
        cachedFilePath = null
    }

    private fun parseXml(data: ByteArray): org.w3c.dom.Document? {
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            builder.parse(java.io.ByteArrayInputStream(data))
        } catch (e: Exception) { null }
    }
}
