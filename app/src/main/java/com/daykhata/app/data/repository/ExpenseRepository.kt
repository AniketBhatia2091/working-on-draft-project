package com.daykhata.app.data.repository

import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.local.ExpenseDao
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesByDate(startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getExpensesByPeriod(startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpenseById(id: Long): Flow<Expense?>
    suspend fun insertExpense(expense: Expense): Long
    suspend fun insertAll(expenses: List<Expense>): List<Long>
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun deleteAllExpenses()
}

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getExpensesByDate(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByDate(startDate, endDate)
    }

    override fun getExpensesByPeriod(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByPeriod(startDate, endDate)
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
    }

    override fun getExpenseById(id: Long): Flow<Expense?> {
        return expenseDao.getExpenseById(id)
    }

    override suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insert(expense)
    }

    override suspend fun insertAll(expenses: List<Expense>): List<Long> {
        return expenseDao.insertAll(expenses)
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.update(expense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.delete(expense)
    }

    override suspend fun deleteAllExpenses() {
        expenseDao.deleteAllExpenses()
    }
}
