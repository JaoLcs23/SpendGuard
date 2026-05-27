package com.joaolucas.spendguard

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration

@Entity(
    tableName = "purchase_history",
    indices = [Index(value = ["userId"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val itemName: String,
    val price: Double,
    val justification: String,
    val wasBlocked: Boolean,
    val aiMessage: String,
    val coolingOffTime: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isImported: Boolean = false,
    val category: String = SpendingCategory.OUTROS.name
)

@Dao
interface PurchaseDao {

    @Insert
    suspend fun insert(purchase: PurchaseEntity)

    @Insert
    suspend fun insertAll(purchases: List<PurchaseEntity>)

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchase_history WHERE itemName = :name ORDER BY timestamp DESC LIMIT 1")
    fun getLastPurchaseByName(name: String): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId OR userId = '' ORDER BY timestamp DESC")
    fun getPurchasesByUser(userId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId OR userId = '' ORDER BY timestamp DESC")
    suspend fun getPurchasesByUserDirect(userId: String): List<PurchaseEntity>

    @Query("SELECT * FROM purchase_history ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase_history ORDER BY timestamp DESC")
    suspend fun getAllPurchasesList(): List<PurchaseEntity>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId AND wasBlocked = 1 ORDER BY timestamp DESC")
    fun getBlockedPurchasesByUser(userId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId AND isImported = 1 ORDER BY timestamp DESC")
    fun getImportedPurchasesByUser(userId: String): Flow<List<PurchaseEntity>>

    @Delete
    suspend fun delete(purchase: PurchaseEntity)

    @Query("DELETE FROM purchase_history WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Query("DELETE FROM purchase_history WHERE userId = :userId AND isImported = 1")
    suspend fun deleteImportedByUser(userId: String)

    @Query("DELETE FROM purchase_history")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM purchase_history
        WHERE (LOWER(itemName) LIKE '%' || LOWER(:name) || '%'
           OR LOWER(:name) LIKE '%' || LOWER(itemName) || '%')
        AND timestamp > :since
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findSimilarRecentPurchase(name: String, since: Long): PurchaseEntity?

    @Query("SELECT * FROM purchase_history WHERE userId = :userId AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getPurchasesSince(userId: String, since: Long): List<PurchaseEntity>
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE purchase_history ADD COLUMN category TEXT NOT NULL DEFAULT 'OUTROS'"
        )
    }
}

@Database(entities = [PurchaseEntity::class], version = 4, exportSchema = false)
abstract class SpendGuardDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: SpendGuardDatabase? = null

        fun getDatabase(context: android.content.Context): SpendGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SpendGuardDatabase::class.java,
                    "spendguard_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}