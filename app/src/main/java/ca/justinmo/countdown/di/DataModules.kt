package ca.justinmo.countdown.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import ca.justinmo.countdown.data.CountDownDatabase
import ca.justinmo.countdown.data.CountdownDao
import ca.justinmo.countdown.data.CountdownRepository
import ca.justinmo.countdown.data.DefaultCountdownRepository
import ca.justinmo.countdown.onboarding.DefaultOnboardingRepository
import ca.justinmo.countdown.onboarding.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Singleton
  @Binds
  abstract fun bindCountdownRepository(repository: DefaultCountdownRepository): CountdownRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Singleton
  @Provides
  fun provideDataBase(@ApplicationContext context: Context): CountDownDatabase {
    return Room.databaseBuilder(
            context.applicationContext,
            CountDownDatabase::class.java,
            "Countdown.db",
        )
        .build()
  }

  @Provides
  fun provideCountdownDao(database: CountDownDatabase): CountdownDao = database.countdownDao()
}

@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {

  @Singleton
  @Provides
  fun provideOnboardingPreferences(@ApplicationContext context: Context): SharedPreferences {
    return context.getSharedPreferences(
        DefaultOnboardingRepository.PREFS_NAME, Context.MODE_PRIVATE)
  }

  @Singleton
  @Provides
  fun provideOnboardingRepository(
      sharedPreferences: SharedPreferences
  ): OnboardingRepository = DefaultOnboardingRepository(sharedPreferences)
}
