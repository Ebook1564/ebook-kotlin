package com.example.ebook_new_app.repository

import com.example.ebook_new_app.models.Book
import com.example.ebook_new_app.models.Category
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import android.util.Log

class BookRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot: QuerySnapshot = firestore.collection("categories").get().await()
            snapshot.documents.map { Category.fromDoc(it) }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error getting categories", e)
            emptyList()
        }
    }

    suspend fun getBooksByCategory(categoryDoc: DocumentSnapshot): List<Book> {
        val categoryId = categoryDoc.id
        val books = mutableListOf<Book>()

        // Method 1: Subcollection
        try {
            val subcollSnapshot = firestore.collection("categories")
                .document(categoryId)
                .collection(categoryId)
                .get()
                .await()
            
            if (!subcollSnapshot.isEmpty) {
                books.addAll(subcollSnapshot.documents.map { Book.fromDoc(it) })
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error checking subcollection $categoryId", e)
        }

        // Method 2: Data List (Manual Parsing)
        try {
            val dataList = categoryDoc.get("data") as? List<*>
            dataList?.forEach { item ->
                val bookMap = item as? Map<*, *> ?: return@forEach
                
                val title = (bookMap["name"] ?: bookMap["title"] ?: "No Title").toString()
                val id = (bookMap["id"] ?: bookMap["docId"] ?: title.replace(" ", "_")).toString()
                val url = (bookMap["fileUrl"] ?: bookMap["file_url"] ?: bookMap["url"])?.toString()
                val asset = (bookMap["originalFileName"] ?: bookMap["fileName"] ?: bookMap["original_file_name"])?.toString()
                val image = (bookMap["image"] ?: bookMap["coverImageUrl"] ?: bookMap["cover_image"])?.toString() ?: ""

                if (url == null && asset == null) {
                    Log.w("BookRepository", "Book '${title}' (ID: $id) is missing metadata. Available fields: ${bookMap.keys}")
                }

                books.add(Book(
                    id = id,
                    title = title,
                    author = (bookMap["author"] ?: "Unknown").toString(),
                    coverImageUrl = image,
                    fileUrl = url,
                    originalFileName = asset
                ))
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error checking data field in $categoryId", e)
        }

        return books
    }

    suspend fun getAllBooksAcrossCategories(): List<Book> {
        return try {
            val snapshot: QuerySnapshot = firestore.collection("categories").get().await()
            val allBooks = mutableListOf<Book>()
            
            for (categoryDoc in snapshot.documents) {
                allBooks.addAll(getBooksByCategory(categoryDoc))
            }
            
            // Also fetch from the flat "books" collection (Production Relay)
            try {
                val librarySnapshot = firestore.collection("books").get().await()
                allBooks.addAll(librarySnapshot.documents.map { Book.fromDoc(it) })
            } catch (e: Exception) {
                Log.e("BookRepository", "Error checking 'books' collection", e)
            }
            
            val distinctBooks = allBooks.distinctBy { 
                val key = (it.title + it.author).lowercase().replace(" ", "")
                key
            }
            Log.d("BookRepository", "Successfully synced ${distinctBooks.size} distinct books.")
            distinctBooks
        } catch (e: Exception) {
            Log.e("BookRepository", "Error in getAllBooksAcrossCategories", e)
            emptyList()
        }
    }

    suspend fun getLibraryBooks(): List<Book> {
        return try {
            val snapshot = firestore.collection("books")
                .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.map { Book.fromDoc(it) }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error getting library books", e)
            emptyList()
        }
    }
}
