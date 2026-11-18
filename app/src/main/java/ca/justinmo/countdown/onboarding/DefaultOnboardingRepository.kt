package ca.justinmo.countdown.onboarding

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOnboardingRepository
@Inject
constructor(
    private val sharedPreferences: SharedPreferences,
) : OnboardingRepository {

  override suspend fun isCompleted(): Boolean {
    return sharedPreferences.getBoolean(KEY_COMPLETED, false)
  }

  override suspend fun setCompleted() {
    sharedPreferences.edit().putBoolean(KEY_COMPLETED, true).apply()
  }

  companion object {
    const val PREFS_NAME = "onboarding"
    const val KEY_COMPLETED = "onboarding_completed"
  }
}
