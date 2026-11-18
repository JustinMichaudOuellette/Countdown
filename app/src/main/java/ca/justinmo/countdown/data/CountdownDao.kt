package ca.justinmo.countdown.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {

  @Upsert suspend fun upsert(countdown: CountDownEntity)

  @Query("SELECT * FROM countdown") suspend fun getAll(): List<CountDownEntity>

  @Query("SELECT * FROM countdown") fun observeAll(): Flow<List<CountDownEntity>>

  @Query("DELETE FROM countdown WHERE id = :countdownId")
  suspend fun deleteById(countdownId: String)

  @Query("SELECT COUNT(*) FROM countdown") suspend fun count(): Int

  @Query("SELECT * FROM countdown WHERE id = :countdownId")
  fun observeCountdownById(countdownId: String): Flow<CountDownEntity?>

  @Query("SELECT * FROM countdown WHERE id = :id") suspend fun getById(id: String): CountDownEntity?

  @Query(
      """
        UPDATE countdown 
        SET emoji = :emoji, 
            title = :title, 
            targetYear = :targetYear, 
            targetMonth = :targetMonth, 
            targetDay = :targetDay, 
            targetHour = :targetHour, 
            targetMinute = :targetMinute,
            yearly = :yearly
        WHERE id = :id
    """
  )
  suspend fun updateCountdown(
      id: String,
      emoji: String,
      title: String?,
      targetYear: Int,
      targetMonth: Int,
      targetDay: Int,
      targetHour: Int,
      targetMinute: Int,
      yearly: Boolean,
  )
}
