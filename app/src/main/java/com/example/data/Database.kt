package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val phone: String,
    val name: String,
    val passwordHash: String,
    val role: String = "User", // "User" or "Admin"
    val hotelName: String? = null
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodId: Int,
    val quantity: Int
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val foodId: Int
)

@Entity(tableName = "order_history")
data class OrderHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAmount: Double,
    val itemsJson: String, // format: id:qty,id:qty...
    val deliveryAddress: String,
    val status: String
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodId: Int,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val category: String,
    val isTrending: Boolean = false,
    val isPopular: Boolean = false,
    val isAiRecommended: Boolean = false,
    val rating: Double = 4.5,
    val deliveryTimeMin: Int = 25,
    val hotelName: String = "SkyBite Hangar"
)

fun FoodItemEntity.toDomainModel() = FoodItem(
    id = id,
    name = name,
    price = price,
    description = description,
    imageUrl = imageUrl,
    category = category,
    isTrending = isTrending,
    isPopular = isPopular,
    isAiRecommended = isAiRecommended,
    rating = rating,
    deliveryTimeMin = deliveryTimeMin,
    hotelName = hotelName
)

fun FoodItem.toEntity() = FoodItemEntity(
    id = id,
    name = name,
    price = price,
    description = description,
    imageUrl = imageUrl,
    category = category,
    isTrending = isTrending,
    isPopular = isPopular,
    isAiRecommended = isAiRecommended,
    rating = rating,
    deliveryTimeMin = deliveryTimeMin,
    hotelName = hotelName
)

@Dao
interface AppDao {
    // User
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Food Items
    @Query("SELECT * FROM food_items")
    fun getFoodItemsFlow(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items")
    suspend fun getFoodItemsOnce(): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItem(id: Int)

    // Cart
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateCartQuantity(id: Int, quantity: Int)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Favorites
    @Query("SELECT * FROM favorites")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE foodId = :foodId")
    suspend fun deleteFavorite(foodId: Int)

    // Order History
    @Query("SELECT * FROM order_history ORDER BY timestamp DESC")
    fun getOrderHistory(): Flow<List<OrderHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderHistoryEntity): Long

    // Reviews
    @Query("SELECT * FROM reviews WHERE foodId = :foodId ORDER BY timestamp DESC")
    fun getReviewsForFood(foodId: Int): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Database(
    entities = [
        UserEntity::class,
        CartItemEntity::class,
        FavoriteEntity::class,
        OrderHistoryEntity::class,
        ReviewEntity::class,
        FoodItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skybite_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
