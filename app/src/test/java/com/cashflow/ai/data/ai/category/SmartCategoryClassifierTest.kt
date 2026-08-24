package com.cashflow.ai.data.ai.category

import com.cashflow.ai.domain.model.CategorySource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SmartCategoryClassifierTest {

    private lateinit var mockRepository: TransactionRepository
    private lateinit var classifier: SmartCategoryClassifier

    @Before
    fun setUp() {
        mockRepository = mock(TransactionRepository::class.java)
        classifier = SmartCategoryClassifier(
            transactionRepository = mockRepository,
            apiKey = "" // offline / rule mode
        )
    }

    @Test
    fun classify_foodAndDiningKeywords_classifiesCorrectly() = runBlocking {
        val suggestion1 = classifier.classify(description = "Makan siang di Warung Padang", type = TransactionType.EXPENSE)
        assertEquals("Food & Dining", suggestion1.category)
        assertEquals(CategorySource.RULE, suggestion1.source)

        val suggestion2 = classifier.classify(description = "Starbucks latte coffee", type = TransactionType.EXPENSE)
        assertEquals("Food & Dining", suggestion2.category)

        val suggestion3 = classifier.classify(description = "Order GoFood ayam geprek", type = TransactionType.EXPENSE)
        assertEquals("Food & Dining", suggestion3.category)
    }

    @Test
    fun classify_groceriesKeywords_classifiesCorrectly() = runBlocking {
        val suggestion1 = classifier.classify(description = "Belanja bulanan di Indomaret", type = TransactionType.EXPENSE)
        assertEquals("Groceries", suggestion1.category)

        val suggestion2 = classifier.classify(description = "Superindo beli daging dan sayur", type = TransactionType.EXPENSE)
        assertEquals("Groceries", suggestion2.category)
    }

    @Test
    fun classify_transportKeywords_classifiesCorrectly() = runBlocking {
        val suggestion1 = classifier.classify(description = "Isi bensin Pertamina", type = TransactionType.EXPENSE)
        assertEquals("Transport", suggestion1.category)

        val suggestion2 = classifier.classify(description = "Grab ride to office", type = TransactionType.EXPENSE)
        assertEquals("Transport", suggestion2.category)
    }

    @Test
    fun classify_billsKeywords_classifiesCorrectly() = runBlocking {
        val suggestion = classifier.classify(description = "Bayar tagihan listrik PLN", type = TransactionType.EXPENSE)
        assertEquals("Bills & Utilities", suggestion.category)
    }

    @Test
    fun classify_shoppingKeywords_classifiesCorrectly() = runBlocking {
        val suggestion = classifier.classify(description = "Beli baju Uniqlo di mall", type = TransactionType.EXPENSE)
        assertEquals("Shopping", suggestion.category)
    }

    @Test
    fun classify_healthcareKeywords_classifiesCorrectly() = runBlocking {
        val suggestion = classifier.classify(description = "Beli obat di Kimia Farma", type = TransactionType.EXPENSE)
        assertEquals("Healthcare", suggestion.category)
    }

    @Test
    fun classify_entertainmentKeywords_classifiesCorrectly() = runBlocking {
        val suggestion = classifier.classify(description = "Nonton di Cinema XXI", type = TransactionType.EXPENSE)
        assertEquals("Entertainment", suggestion.category)
    }

    @Test
    fun classify_incomeCategories_classifiesCorrectly() = runBlocking {
        val salarySuggestion = classifier.classify(description = "Gaji bulanan PT ABC", type = TransactionType.INCOME)
        assertEquals("Salary", salarySuggestion.category)

        val freelanceSuggestion = classifier.classify(description = "Proyek desain logo freelance", type = TransactionType.INCOME)
        assertEquals("Freelance", freelanceSuggestion.category)

        val investSuggestion = classifier.classify(description = "Dividen saham BBCA", type = TransactionType.INCOME)
        assertEquals("Investment", investSuggestion.category)
    }

    @Test
    fun classify_userHistoryMatch_prioritizesUserHistory() = runBlocking {
        `when`(mockRepository.getMostFrequentCategoryForQuery("Warung Bu Siti")).thenReturn("Groceries")

        val suggestion = classifier.classify(
            description = "Belanja",
            merchant = "Warung Bu Siti",
            type = TransactionType.EXPENSE
        )

        assertEquals("Groceries", suggestion.category)
        assertEquals(CategorySource.HISTORY, suggestion.source)
        assertTrue(suggestion.confidence >= 0.90)
    }
}
