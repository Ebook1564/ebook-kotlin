package com.freeebookbundle.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogMetaDao {
    @Query("SELECT * FROM catalog_meta WHERE id = 1")
    suspend fun getCatalogMeta(): CatalogMetaEntity?

    @Query("SELECT catalogHash FROM catalog_meta WHERE id = 1")
    suspend fun getCatalogHash(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: CatalogMetaEntity)

    @Query("UPDATE catalog_meta SET catalogHash = :hash, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateHash(hash: String, timestamp: Long)
}
