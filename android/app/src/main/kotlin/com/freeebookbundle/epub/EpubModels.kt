package com.freeebookbundle.epub

import android.graphics.Bitmap
import androidx.compose.ui.text.AnnotatedString

data class EpubBook(
    val fileName: String,
    val title: String,
    val author: String,
    val language: String,
    val coverImage: Bitmap?,
    val chapters: List<EpubChapter>,
    val filePath: String,
    val basePath: String = "",
    val totalCharacters: Long = 0
)

data class EpubChapter(
    val chapterIndex: Int,
    val title: String,
    val href: String,
    val fragmentId: String? = null,
    val characterOffset: Long = 0,
    val characterCount: Int = 0
)

sealed class ReaderItem {
    abstract val id: String

    data class Text(
        override val id: String,
        val text: AnnotatedString,
        val charOffsetInChapter: Int = 0,
        val textAlign: String? = null,
        val locator: String? = null // e.g. "p[2]" or unique id
    ) : ReaderItem()

    data class Image(
        override val id: String,
        val path: String
    ) : ReaderItem()

    data class Heading(
        override val id: String,
        val text: String,
        val level: Int
    ) : ReaderItem()
}
