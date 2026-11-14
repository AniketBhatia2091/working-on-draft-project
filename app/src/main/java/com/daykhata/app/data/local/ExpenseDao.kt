package com.daykhata.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): Flow<Expense?>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getExpensesByDate(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getExpensesByPeriod(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>
}
