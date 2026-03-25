package com.example.ebook_new_app.reader

import com.example.ebook_new_app.models.Book
import org.readium.r2.shared.publication.Publication

object ReaderRepository {
    var currentPublication: Publication? = null
    var currentBook: Book? = null
}
