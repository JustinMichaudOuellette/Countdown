package ca.justinmo.countdown.data

import kotlinx.coroutines.flow.Flow

/** Interface to the data layer. */
interface CountdownRepository {

  fun getCountdownsStream(): Flow<List<CountDownEntity>>

  suspend fun refresh()

  suspend fun refresh(id: String)

  suspend fun createCountdown(countdown: CountDownEntity): String

  suspend fun updateCountdown(countdown: CountDownEntity)

  suspend fun getCountdown(id: String): CountDownEntity?

  suspend fun deleteCountdown(id: String)

  suspend fun getCountdownCount(): Int

  fun getCountdownStream(countdownId: String): Flow<CountDownEntity?>
}
