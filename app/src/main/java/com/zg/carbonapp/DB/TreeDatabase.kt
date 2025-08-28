package com.zg.carbonapp.DB

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey val id: String,
    val treeType: String,
    val plantTime: Long,
    val lastWaterTime: Long,
    val growthSpeed: Float
)

@Dao
interface TreeDao {
    @Query("SELECT * FROM trees ORDER BY plantTime DESC")
    suspend fun getAllTrees(): List<TreeEntity>

    @Insert
    suspend fun addTree(tree: TreeEntity)

    @Update
    suspend fun updateTree(tree: TreeEntity)

    @Query("DELETE FROM trees WHERE id = :id")
    suspend fun deleteTree(id: String)

    @Query("SELECT COUNT(*) FROM trees")
    suspend fun getTotalCount(): Int
}

@Database(entities = [TreeEntity::class], version = 1)
abstract class TreeDatabase : RoomDatabase() {
    abstract fun treeDao(): TreeDao

    companion object {
        @Volatile
        private var INSTANCE: TreeDatabase? = null

        fun getInstance(context: Context): TreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeDatabase::class.java,
                    "tree_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}