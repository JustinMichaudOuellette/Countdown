package ca.justinmo.countdown.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CountDownEntity::class], version = 1, exportSchema = true)
abstract class CountDownDatabase : RoomDatabase() {

  abstract fun countdownDao(): CountdownDao
}
