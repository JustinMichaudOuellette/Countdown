package ca.justinmo.countdown.data

import ca.justinmo.countdown.di.ApplicationScope
import ca.justinmo.countdown.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCountdownRepository
@Inject
constructor(
    private val countdownDao: CountdownDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
) : CountdownRepository {

  override suspend fun createCountdown(countdown: CountDownEntity): String {
    val countDownId = withContext(dispatcher) { UUID.randomUUID().toString() }
    check(countdown.targetYear >= 0)
    check(countdown.targetMonth >= 0)
    check(countdown.targetDay >= 0)
    check(countdown.targetHour >= 0)
    check(countdown.targetMinute >= 0)
    val countdownEntity = countdown.copy(id = countDownId)
    countdownDao.upsert(countdownEntity)
    return countDownId
  }

  override suspend fun refresh() {
    TODO("Not yet implemented")
  }

  override fun getCountdownsStream(): Flow<List<CountDownEntity>> {
    return countdownDao.observeAll().map { coundowns -> withContext(dispatcher) { coundowns } }
  }

  override suspend fun refresh(id: String) {
    refresh()
  }

  override fun getCountdownStream(countdownId: String): Flow<CountDownEntity?> {
    return countdownDao.observeCountdownById(countdownId)
  }

  override suspend fun updateCountdown(countdown: CountDownEntity) {
    countdownDao.updateCountdown(
        countdown.id,
        countdown.emoji,
        countdown.title,
        countdown.targetYear,
        countdown.targetMonth,
        countdown.targetDay,
        countdown.targetHour,
        countdown.targetMinute,
        countdown.yearly,
    )
  }

  override suspend fun getCountdown(id: String): CountDownEntity? {
    return countdownDao.getById(id)
  }

  override suspend fun deleteCountdown(id: String) {
    countdownDao.deleteById(id)
  }

  override suspend fun getCountdownCount(): Int {
    return countdownDao.count()
  }
}
