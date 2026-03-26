package com.freeebookbundle.reader

import com.freeebookbundle.models.Book
import org.readium.r2.shared.publication.Publication

object ReaderRepository {
    var currentPublication: Publication? = null
    var currentBook: Book? = null
}
