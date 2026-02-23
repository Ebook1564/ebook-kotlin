package com.example.ebook_new_app.models

import com.google.firebase.firestore.DocumentSnapshot

data class Category(
    val id: String = "",
    val title: String = "",
    val icon: String = "📚",
    val color: String = "#2196F3"
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Category {
            return Category(
                id = doc.id,
                title = doc.getString("title") ?: "",
                icon = doc.getString("icon") ?: "📚",
                color = doc.getString("color") ?: "#2196F3"
            )
        }
    }
}
