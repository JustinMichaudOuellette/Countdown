package ca.justinmo.countdown.onboarding

/** Persists whether the first-run onboarding has been completed. */
interface OnboardingRepository {

  suspend fun isCompleted(): Boolean

  suspend fun setCompleted()
}
