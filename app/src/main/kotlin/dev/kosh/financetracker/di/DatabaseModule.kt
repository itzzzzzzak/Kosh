package dev.kosh.financetracker.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.kosh.financetracker.data.database.FinanceDatabase
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.repository.RoomTransactionRepository
import dev.kosh.financetracker.data.repository.TransactionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, FinanceDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(database: FinanceDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository =
        RoomTransactionRepository(dao)
}
