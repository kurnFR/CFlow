# CFlow

CASHFLOW AI - COMPLETE PRODUCTION PRD

Version: 2.0 (Production Ready)
Date: August 19, 2026
Status: Development Ready
TABLE OF CONTENTS

    Executive Summary

    Product Overview

    Goals & Success Metrics

    Target Users

    Technical Architecture

    Core Features (Detailed)

    AI Pipeline Specification

    Data Model & Storage

    Google Sheets Integration

    Dashboard & Analytics

    UI/UX Specifications

    Non-Functional Requirements

    Security & Privacy

    Testing Strategy

    Implementation Roadmap

    API & Dependencies

    Success Criteria

    Out of Scope

    Appendices

1. EXECUTIVE SUMMARY

CashFlow AI is a production-ready Android application that revolutionizes personal finance tracking by combining AI-powered receipt scanning with Google Sheets synchronization. Users can log transactions in under 15 seconds using manual entry or photo capture, with AI automatically extracting key details and categorizing expenses. The app provides real-time dashboard analytics and works completely offline-first.

Key Differentiators:

    AI-powered receipt scanning (≥95% accuracy)

    Google Sheets as source of truth

    Offline-first architecture

    Real-time analytics dashboard

    Multi-language support (EN/ID)

    No permanent server-side data storage

2. PRODUCT OVERVIEW
2.1 App Description

CashFlow AI is a mobile-first Android application enabling users to record income and expenses via manual text entry or receipt photos. The AI extracts description, total amount, date, tax, and discount from receipts, assigns categories, and stores transactions locally and in Google Sheets.
2.2 Value Proposition

    Speed: Log transactions in ≤15 seconds

    Automation: AI handles data extraction and categorization

    Control: User-owned Google Sheets as source of truth

    Insights: Real-time dashboard with charts and filters

    Privacy: No server-side storage of financial data

3. GOALS & SUCCESS METRICS
3.1 Primary Goals
Goal	Metric	Target
Transaction Speed	Time to log	≤15 seconds
AI Accuracy	Receipt extraction	≥95%
User Retention	Active users (30 days)	≥70%
App Rating	Play Store rating	≥4.5★
AI Adoption	Photo capture users/week	≥70%
3.2 Secondary Goals

    Zero data loss during offline operation

    Real-time sync (<5 seconds online)

    Dashboard load time <2 seconds

    100% crash-free session rate

4. TARGET USERS
4.1 User Personas

Persona 1: Indra (Freelancer)

    Age: 28

    Profession: UI/UX Designer

    Needs: Track project expenses, generate reports

    Tech comfort: High

    Frequency: Daily

Persona 2: Siti (Small Business Owner)

    Age: 35

    Profession: Warung owner

    Needs: Track daily sales, expenses

    Tech comfort: Medium

    Frequency: Multiple times daily

Persona 3: Budi (Corporate Employee)

    Age: 30

    Profession: Finance Manager

    Needs: Personal budget tracking

    Tech comfort: High

    Frequency: Weekly

4.2 User Requirements

    Indonesian and English speaking

    Android smartphone (minimum API 26)

    Google account holder

    Comfortable with Google Sheets

    Basic financial literacy

5. TECHNICAL ARCHITECTURE
5.1 Architecture Pattern

Clean Architecture + MVVM
text

┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  (Compose UI + ViewModels + Navigation)     │
├─────────────────────────────────────────────┤
│             Domain Layer                    │
│  (Use Cases + Business Logic + Models)      │
├─────────────────────────────────────────────┤
│              Data Layer                     │
│  (Repositories + Data Sources)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │   Room   │  │ Sheets   │  │   AI    │ │
│  │  Local   │  │  API    │  │ Pipeline │ │
│  └──────────┘  └──────────┘  └──────────┘ │
└─────────────────────────────────────────────┘

5.2 Component Diagram
text

┌─────────────────────────────────────────────────────────────┐
│                         Android App                         │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌─────────┐ │
│  │  Camera   │  │  Gallery  │  │  Manual   │  │ Settings│ │
│  │  Capture  │  │  Picker   │  │   Entry   │  │         │ │
│  └───────────┘  └───────────┘  └───────────┘  └─────────┘ │
│                         │                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │              AI Pipeline Service                │       │
│  │  ┌─────────┐  ┌─────────┐  ┌───────────────┐  │       │
│  │  │ ML Kit  │→│ Gemini  │→│ Category AI   │  │       │
│  │  │ OCR     │  │ Extract │  │ Classifier    │  │       │
│  │  └─────────┘  └─────────┘  └───────────────┘  │       │
│  └─────────────────────────────────────────────────┘       │
│                         │                                   │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐              │
│  │  Room DB  │←│  Sync     │→│  Google   │              │
│  │  Local    │  │  Manager  │  │  Sheets   │              │
│  └───────────┘  └───────────┘  └───────────┘              │
│                         │                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │              Dashboard Engine                   │       │
│  │  ┌─────────┐  ┌─────────┐  ┌───────────────┐  │       │
│  │  │ Summary │  │ Pie     │  │ Line Chart    │  │       │
│  │  │ Cards   │  │ Chart   │  │               │  │       │
│  │  └─────────┘  └─────────┘  └───────────────┘  │       │
│  └─────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘

5.3 Technology Stack
text

┌──────────────────┬──────────────────────────────────────────┐
│ Category         │ Technology                               │
├──────────────────┼──────────────────────────────────────────┤
│ Language         │ Kotlin 1.9.x                             │
│ UI Framework     │ Jetpack Compose (Material 3)            │
│ Architecture     │ Clean Architecture + MVVM               │
│ DI               │ Dagger Hilt / Koin                       │
│ Database         │ Room 2.6.1                              │
│ Network          │ Retrofit 2.9 + OkHttp 4.11             │
│ Image Load       │ Coil 2.6.0                              │
│ Camera           │ CameraX 1.4.0                           │
│ OCR              │ ML Kit Text Recognition v16.0.0         │
│ AI/ML            │ Google Gemini 1.5 Pro                   │
│ Charts           │ MPAndroidChart 3.1.0                    │
│ Auth             │ Google Sign-In 20.0.0                   │
│ Google APIs      │ Sheets API v4 + Drive API v3            │
│ Async            │ Coroutines 1.7.3 + Flow                 │
│ Background       │ WorkManager 2.9.0                       │
│ Security         │ EncryptedSharedPreferences 1.1.0        │
│ Testing          │ JUnit 4.13.2 + Espresso 3.5.1           │
│ Logging          │ Timber 5.1.0                            │
│ Analytics        │ Firebase Analytics 20.0.0               │
│ Crash Reporting  │ Firebase Crashlytics 18.3.0            │
└──────────────────┴──────────────────────────────────────────┘

6. CORE FEATURES (DETAILED)
6.1 Transaction Input
A. Manual Entry Flow
text

1. User taps "+" FAB
2. Select "Manual Entry"
3. Form fields:
   - Date (default: today, editable)
   - Description (free text, min 2 chars)
   - Amount (numeric with currency selector IDR/USD)
   - Category (auto-suggested, user can override)
   - Type (Income/Expense)
   - Notes (optional)
4. AI suggests category in real-time based on description
5. Save → Local DB → Sync Queue → Google Sheets

B. Photo/Gallery Capture Flow
text

1. User taps "+" FAB
2. Select "Take Photo" or "Choose from Gallery"
3. Camera/Gallery opens
4. Image captured/selected
5. AI Pipeline processes image:
   a. ML Kit OCR extracts text
   b. Gemini extracts fields:
      - Merchant/Description
      - Total Amount
      - Date
      - Tax (if exists)
      - Discount (if exists)
      - Items list (top 3)
   c. Category classifier assigns category
   d. Confidence scores for each field
6. Review screen shows extracted data:
   - All fields editable
   - Confidence indicators (High/Medium/Low)
   - Option to retake/recapture
7. User confirms → Save → Sync

6.2 Category System
Predefined Categories
kotlin

val SEED_CATEGORIES = listOf(
    // Food & Dining
    "Food & Dining", "Groceries", "Restaurant", "Coffee",
    
    // Transportation
    "Transport", "Fuel", "Public Transport", "Ride Hailing",
    
    // Shopping
    "Shopping", "Clothing", "Electronics", "Home Goods",
    
    // Bills & Utilities
    "Bills", "Electricity", "Water", "Internet", "Phone",
    
    // Housing
    "Rent", "Mortgage", "Maintenance", "Property Tax",
    
    // Health & Wellness
    "Healthcare", "Medical", "Pharmacy", "Fitness",
    
    // Entertainment
    "Entertainment", "Movies", "Music", "Games",
    
    // Education
    "Education", "Books", "Courses", "Training",
    
    // Income
    "Salary", "Freelance", "Investment", "Business Income",
    
    // Other
    "Insurance", "Tax", "Gifts", "Charity", "Other"
)

Category Rules

    AI auto-assigns category on every transaction

    User can override AI suggestion

    User can create custom categories

    Category change triggers learning feedback

    Categories persist across devices via Google Sheets

6.3 Income Input
text

Sidebar Menu Option:
1. Tap hamburger menu → "Add Income"
2. Date picker (default: today)
3. Total income amount
4. Description (optional)
5. Source category (dropdown)
6. Save → Treated as Income Type

7. AI PIPELINE SPECIFICATION
7.1 Receipt Processing Flow
text

┌─────────────────────────────────────────────────────────────────┐
│                     Receipt Image                              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Image Preprocessing                                │
│  - Rotation correction                                         │
│  - Contrast enhancement                                        │
│  - Resize (max 2048px)                                         │
│  - Format conversion (JPEG)                                    │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              ML Kit OCR (On-device)                            │
│  - Text detection                                              │
│  - Text recognition                                            │
│  - Block/line/word positioning                                 │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Gemini 1.5 Pro (Cloud)                            │
│  Prompt: Extract receipt fields with JSON output              │
│  Input: OCR text + Image (optional)                           │
│  Output: {                                                    │
│    merchant: string,                                          │
│    total: number,                                             │
│    date: string (YYYY-MM-DD),                                 │
│    tax: number?,                                              │
│    discount: number?,                                         │
│    items: string[],                                           │
│    confidence: {                                              │
│      merchant: 0-1,                                           │
│      total: 0-1,                                              │
│      date: 0-1                                                │
│    }                                                          │
│  }                                                            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Category Classifier                               │
│  - Keyword matching                                            │
│  - Gemini classification                                       │
│  - User history consideration                                  │
│  - Output: category + confidence score                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Validation & Fallback                             │
│  - Validate all extracted fields                              │
│  - If confidence < 70%: mark for review                       │
│  - If extraction fails: fallback to manual                    │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Review Screen                                     │
│  - Display all extracted data                                 │
│  - Show confidence indicators                                 │
│  - Allow edits                                                │
│  - User confirms                                              │
└─────────────────────────────────────────────────────────────────┘

7.2 Prompt Engineering
System Prompt (Receipt Extraction)
text

You are a specialized receipt extraction AI. Extract key information 
from the provided receipt text/image.

Return ONLY valid JSON with this structure:
{
  "merchant": "store or restaurant name",
  "total": 123.45,
  "currency": "IDR" or "USD",
  "date": "YYYY-MM-DD",
  "tax": 11.50,
  "discount": 5.00,
  "items": ["item1", "item2", "item3"],
  "confidence": {
    "merchant": 0.95,
    "total": 0.98,
    "date": 0.92
  }
}

Rules:
- If date not found, use current date
- Total amount must be the final payment amount
- If multiple totals, use the highest (grand total)
- For Indonesian receipts, recognize "Total", "Bayar", "Kembali"
- For English receipts, recognize "Total", "Amount Due", "Payment"
- If tax not found, omit field
- If discount not found, omit field
- Merchant name: most prominent business name
- Items: summarize to top 3 if more exist
- Confidence scores: how certain are you about each field

System Prompt (Category Classification)
text

You are a category classification AI for personal finance.

Classify the transaction into one of these categories:
[Food & Dining, Groceries, Transport, Shopping, Bills, 
 Rent, Healthcare, Entertainment, Education, Salary, 
 Freelance, Investment, Insurance, Other]

Rules:
1. Consider the description text
2. Consider the merchant name
3. Consider the amount (large amounts often bills/rent)
4. Income categories for positive amounts
5. Expense categories for negative amounts
6. Return ONLY the category name

Examples:
- "McDonald's" → "Food & Dining"
- "Grab ride" → "Transport"
- "Salary from PT ABC" → "Salary"
- "Indomaret" → "Groceries"
- "Electricity bill" → "Bills"
- "GoFood" → "Food & Dining"
- "Shopee" → "Shopping"
- "BPJS" → "Healthcare"
- "Netflix" → "Entertainment"

7.3 AI Fallback Strategy
text

Level 1: On-device ML Kit
- If confidence < 0.7 → Level 2

Level 2: Cloud Gemini
- If confidence < 0.7 → Level 3
- If network unavailable → Level 3

Level 3: Manual Entry
- Show extracted fields (if available)
- Allow full manual edit
- User confirms

8. DATA MODEL & STORAGE
8.1 Room Entity
kotlin

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "amount")
    val amount: Double, // Positive for income, negative for expense
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "type")
    val type: TransactionType, // INCOME or EXPENSE
    
    @ColumnInfo(name = "source")
    val source: TransactionSource, // MANUAL or PHOTO
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null, // Google Drive URL
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "currency")
    val currency: Currency = Currency.IDR,
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "sync_version")
    val syncVersion: Int = 1,
    
    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Double? = null, // 0-1
    
    @ColumnInfo(name = "ai_merchant")
    val aiMerchant: String? = null // Extracted merchant name
)

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionSource {
    MANUAL, PHOTO
}

enum class Currency {
    IDR, USD
}

8.2 Google Sheets Schema
text

Columns (exact order):
A: Date (YYYY-MM-DD)
B: Description
C: Amount (positive = income, negative = expense)
D: Category
E: Type (Income/Expense)
F: Source (Manual/Photo)
G: Image URL (optional)
H: Created At (ISO 8601)
I: Notes (optional)
J: Currency (IDR/USD)
K: AI Confidence (0-1)
L: Sync Version (integer)
M: Transaction ID (for conflict resolution)

8.3 Room DAOs
kotlin

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE category = :category AND date BETWEEN :startDate AND :endDate")
    fun getTransactionsByCategory(category: String, startDate: String, endDate: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTransactionsByType(type: TransactionType, startDate: String, endDate: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE is_synced = 0")
    fun getUnsyncedTransactions(): List<Transaction>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalAmount(type: TransactionType, startDate: String, endDate: String): Flow<Double?>
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    fun getExpenseByCategory(startDate: String, endDate: String): Flow<List<CategoryExpense>>
    
    @Query("SELECT strftime('%Y-%m', date) as month, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY month ORDER BY month")
    fun getMonthlyTotals(type: TransactionType, startDate: String, endDate: String): Flow<List<MonthlyTotal>>
}

8.4 Repository Pattern
kotlin

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Result<Long>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(transaction: Transaction): Result<Unit>
    fun getTransactions(dateRange: DateRange, category: String?, type: TransactionType?): Flow<List<Transaction>>
    fun getSummary(dateRange: DateRange): Flow<TransactionSummary>
    fun getCategoryExpenses(dateRange: DateRange): Flow<List<CategoryExpense>>
    fun getMonthlyTrends(dateRange: DateRange, type: TransactionType?): Flow<List<MonthlyTotal>>
    suspend fun syncWithGoogleSheets(): Result<SyncResult>
    suspend fun syncAll(): Result<Unit>
    suspend fun resolveConflicts(remoteTransactions: List<Transaction>): List<ConflictResolution>
}

9. GOOGLE SHEETS INTEGRATION
9.1 OAuth 2.0 Flow
text

1. Google Sign-In
   ↓
2. Request Scopes:
   - https://www.googleapis.com/auth/spreadsheets
   - https://www.googleapis.com/auth/drive.file
   ↓
3. User Consents
   ↓
4. Access Token & Refresh Token
   ↓
5. Store in EncryptedSharedPreferences

9.2 Setup Flow
text

1. First Launch:
   ↓
2. Google Sign-In
   ↓
3. Option A: Create New Sheet
   - Create spreadsheet with headers
   - Store spreadsheet ID
   - Add first row with column headers
   ↓
4. Option B: Select Existing Sheet
   - List user's spreadsheets
   - Validate column structure
   - Confirm or add missing columns
   ↓
5. Success → Dashboard

9.3 Sync Strategy
kotlin

class SyncManager {
    // Priority Queue
    private val syncQueue = PriorityQueue<SyncTask>()
    
    // Sync Types
    enum class SyncPriority {
        CRITICAL,   // New transactions
        HIGH,       // Updates/Deletions
        NORMAL,     // Pull updates
        LOW         // Background sync
    }
    
    suspend fun sync() {
        // 1. Push local changes first
        pushUnsyncedTransactions()
        
        // 2. Pull remote changes
        pullRemoteTransactions()
        
        // 3. Resolve conflicts
        resolveConflicts()
        
        // 4. Update local DB
        applyRemoteChanges()
    }
    
    // Batch updates: max 100 rows per request
    private suspend fun pushUnsyncedTransactions() {
        val unsynced = repository.getUnsyncedTransactions()
        val chunks = unsynced.chunked(100)
        chunks.forEach { chunk ->
            sheetsApi.batchUpdate(spreadsheetId, chunk)
        }
    }
}

9.4 Conflict Resolution
kotlin

enum class ConflictResolution {
    LOCAL_WINS,
    REMOTE_WINS,
    MERGE,
    USER_DECISION
}

data class ConflictResolutionStrategy(
    val lastUpdated: Long,
    val localVersion: Int,
    val remoteVersion: Int
) {
    fun resolve(): ConflictResolution {
        return when {
            localVersion > remoteVersion -> ConflictResolution.LOCAL_WINS
            remoteVersion > localVersion -> ConflictResolution.REMOTE_WINS
            else -> ConflictResolution.USER_DECISION
        }
    }
}

10. DASHBOARD & ANALYTICS
10.1 Summary Cards
text

┌──────────────────────────────────────────────┐
│  💰 CashFlow AI                             │
├──────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Income   │  │  Expense │  │  Net     │  │
│  │  Rp 8.5M  │  │  Rp 5.2M │  │  +Rp 3.3M│  │
│  │   ▲ 12%   │  │   ▼ 5%   │  │   ▲ 8%   │  │
│  └──────────┘  └──────────┘  └──────────┘  │
│                                              │
│  Filter: [Today] [This Week] [This Month]    │
│  Category: [All ▼]   Type: [All ▼]          │
│                                              │
│  ┌──────────────────────────────────────────┐ │
│  │      Expense by Category (Pie Chart)    │ │
│  │   🍔 Food  30%   🚗 Transport 20%      │ │
│  │   🏠 Bills 18%   🛍️ Shopping 15%      │ │
│  │   Other: 17%                            │ │
│  └──────────────────────────────────────────┘ │
│                                              │
│  ┌──────────────────────────────────────────┐ │
│  │     Monthly Trends (Line Chart)         │ │
│  │   Income ───  Expense ───  Net ──      │ │
│  │   10M │                    ╭──╮        │ │
│  │    5M │               ╭────╯  │        │ │
│  │    0M │──────────────╯       ╰──────── │ │
│  │        J F M A M J J A S O N D        │ │
│  └──────────────────────────────────────────┘ │
│                                              │
│  Recent Transactions:                         │
│  Today  |  Lunch  |  Rp 50K  |  Food 🍔   │
│  Today  |  Taxi   |  Rp 25K  |  Transport 🚗│
└──────────────────────────────────────────────┘

10.2 Chart Specifications
Pie Chart
kotlin

// MPAndroidChart Configuration
val pieChart = PieChart(context).apply {
    setUsePercentValues(true)
    description.isEnabled = false
    setExtraOffsets(5f, 10f, 5f, 5f)
    setDragDecelerationFrictionCoef(0.95f)
    isDrawHoleEnabled = true
    setHoleColor(Color.WHITE)
    setTransparentCircleRadius(61f)
    setDrawCenterText(true)
    rotationAngle = 0f
    isRotationEnabled = true
    isHighlightPerTapEnabled = true
    animateY(1400, Easing.EaseInOutQuad)
}

Line Chart
kotlin

val lineChart = LineChart(context).apply {
    description.isEnabled = false
    setTouchEnabled(true)
    setDragEnabled(true)
    setScaleEnabled(true)
    setDrawGridBackground(false)
    setPinchZoom(true)
    
    // X-axis configuration
    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        setDrawGridLines(false)
        valueFormatter = MonthAxisFormatter()
    }
    
    // Y-axis configuration
    axisLeft.apply {
        setDrawGridLines(true)
        axisMinimum = 0f
        valueFormatter = CurrencyAxisFormatter()
    }
    axisRight.isEnabled = false
    
    animateX(1500)
}

10.3 Filter System
kotlin

data class FilterOptions(
    val dateRange: DateRange = DateRange.THIS_MONTH,
    val category: String? = null,
    val type: TransactionType? = null,
    val amountMin: Double? = null,
    val amountMax: Double? = null,
    val searchQuery: String? = null
) {
    fun toQuery(): String {
        val conditions = mutableListOf<String>()
        // Build SQL WHERE clause based on filters
        return conditions.joinToString(" AND ")
    }
}

enum class DateRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    CUSTOM
}

11. UI/UX SPECIFICATIONS
11.1 Screen Layouts
Home Dashboard Screen
xml

Navigation: Bottom Nav (4 tabs)
- Dashboard (Home)
- Add Transaction (FAB)
- Transactions List
- Settings

Dashboard:
├── Top Bar (App name + Refresh)
├── Summary Cards (Horizontal scroll)
├── Filter Bar (Date range picker + Category dropdown)
├── Pie Chart (Expense by category)
├── Line Chart (Monthly trends)
└── Recent Transactions (Horizontal scroll)

Add Transaction Screen
xml

Add Transaction Flow:
├── Option Selection
│   ├── [Manual Entry]
│   └── [Take Photo]
│       └── [Choose from Gallery]
│
Manual Entry:
├── Date Picker
├── Description (TextField)
├── Amount (TextField with currency selector)
├── Category (Dropdown with AI suggestions)
├── Type (Toggle: Income/Expense)
├── Notes (Optional)
└── [Save] Button

Photo Capture:
├── Camera Preview (CameraX)
├── Capture Button
├── Gallery Button
└── Flash/Camera Switch

Receipt Review:
├── Image Preview
├── Extracted Fields (Editable)
│   ├── Merchant: [TextField]
│   ├── Total: [TextField]
│   ├── Date: [Date Picker]
│   ├── Category: [Dropdown]
│   ├── Tax: [TextField]
│   └── Discount: [TextField]
├── Confidence Indicators
├── Retake Button
└── [Save] Button

Transactions List Screen
xml

Transactions List:
├── Search Bar
├── Filter/Sort Options
├── Transaction List (RecyclerView)
│   ├── Date Header
│   ├── Transaction Item
│   │   ├── Category Icon
│   │   ├── Description
│   │   ├── Amount (Color-coded)
│   │   └── Category
│   └── Swipe Actions (Delete/Edit)
└── Floating Action Button (Add)

Settings Screen
xml

Settings:
├── Profile Section
│   ├── Google Account
│   └── Sync Status
├── Google Sheets
│   ├── Current Sheet Name
│   ├── Change Sheet
│   └── Manual Sync
├── Preferences
│   ├── Default Currency
│   ├── Default Category
│   └── Theme (Dark/Light/System)
├── AI Settings
│   ├── Enable AI (Toggle)
│   ├── Use Cloud AI (Toggle)
│   └── Confidence Threshold
├── Data Management
│   ├── Export Data
│   ├── Import Data
│   └── Clear Cache
├── About
│   ├── Version
│   ├── Privacy Policy
│   └── Support
└── Sign Out

11.2 Material 3 Theme
kotlin

// Colors
val md_theme_light_primary = Color(0xFF006A6A)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF7DF1F1)
val md_theme_light_onPrimaryContainer = Color(0xFF001F1F)
val md_theme_light_secondary = Color(0xFF4A6363)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE8E8)
val md_theme_light_onSecondaryContainer = Color(0xFF051F1F)
val md_theme_light_tertiary = Color(0xFF525C7D)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFDFE0FF)
val md_theme_light_onTertiaryContainer = Color(0xFF0E1935)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBFCFC)
val md_theme_light_onBackground = Color(0xFF191C1C)
val md_theme_light_surface = Color(0xFFFBFCFC)
val md_theme_light_onSurface = Color(0xFF191C1C)
val md_theme_light_surfaceVariant = Color(0xFFDBE4E4)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4848)
val md_theme_light_outline = Color(0xFF6F7979)

// Typography
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    // ... Full typography scale
)

11.3 Animations
kotlin

// Transaction Save Animation
@Composable
fun SaveAnimation() {
    AnimatedVisibility(
        visible = isSaving,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        CircularProgressIndicator()
    }
}

// Chart Animation
// Use MPAndroidChart's built-in animations

// Screen Transitions
val navController = rememberNavController()
NavHost(
    navController = navController,
    startDestination = "dashboard",
    modifier = Modifier.animateContentSize()
)

// FAB Animation
FloatingActionButton(
    onClick = { /* ... */ },
    modifier = Modifier
        .scale(if (isExpanded) 1f else 0f)
        .animateContentSize()
)

11.4 Error States
kotlin

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = "Error"
        )
        Text(text = message, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.Add,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(72.dp))
        Text(text = message, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAction) {
            Text("Add Transaction")
        }
    }
}

12. NON-FUNCTIONAL REQUIREMENTS
12.1 Performance Requirements
Metric	Target	Acceptable
App Startup	<2 seconds	<3 seconds
Camera Launch	<1 second	<1.5 seconds
AI Processing	<3 seconds	<5 seconds
Chart Rendering	<200ms	<500ms
Sync to Sheets	<5 seconds	<10 seconds
Database Query	<100ms	<300ms
Image Compression	<500ms	<1 second
Memory Usage	<200MB	<300MB
Battery Impact	<2%/hour	<5%/hour
12.2 Scalability Requirements
kotlin

// Support for:
- 10,000+ transactions locally
- 5,000+ transactions in Google Sheets
- 100+ categories (predefined + custom)
- 20 concurrent sync tasks
- 100MB cached images max
- 1,000+ sync queue items

12.3 Reliability Requirements

    99.9% crash-free session rate

    Zero data loss during offline operation

    Automatic retry with exponential backoff

    Data recovery mechanisms

    Integrity checks on app startup

    Backup before major operations

12.4 Accessibility Requirements
kotlin

// TalkBack Support
@Composable
fun TransactionItem(transaction: Transaction) {
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = "Transaction: ${transaction.description}, ${transaction.amount}"
            }
    )
}

// Color Contrast
// Use WCAG 2.1 AA compliant colors
// Minimum contrast ratio: 4.5:1

// Touch Targets
// Minimum 48dp x 48dp for interactive elements

// Text Scaling
// Support up to 200% font scaling

// Keyboard Navigation
// Full keyboard accessibility support

13. SECURITY & PRIVACY
13.1 Data Security
kotlin

// Encrypted Storage
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Database Encryption
class EncryptedRoomDatabase : RoomDatabase() {
    // Use SQLCipher for database encryption
}

// Network Security
// Certificate pinning
// TLS 1.2+ required
// Validate SSL certificates

13.2 Privacy Policy
text

Data Collection:
- Transaction data (stored locally and in user's Google Sheets)
- Receipt images (stored in user's Google Drive)
- Usage analytics (anonymized)
- Device information (for crash reporting)

Data Storage:
- All data owned by user
- No server-side storage
- User's Google Drive for images
- User's Google Sheets for transactions

Data Sharing:
- No third-party sharing
- No data selling
- Google APIs for authentication and storage
- AI APIs for receipt processing

User Rights:
- Delete all data at any time
- Export data in CSV format
- Revoke Google access
- Request data removal

13.3 Permissions
xml

<!-- Required Permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Optional Permissions -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Runtime Permission Handling -->
// Request permissions with proper justification
// Handle denial gracefully
// Explain why permissions needed

14. TESTING STRATEGY
14.1 Unit Tests
kotlin

@Test
fun `test AI parsing of receipt text`() {
    val receiptText = """
        TOKO MAKMUR
        Jl. Merdeka No. 123
        Total: Rp 45.000
        Tax: Rp 4.500
    """.trimIndent()
    
    val result = aiParser.parseReceipt(receiptText)
    
    assertEquals("TOKO MAKMUR", result.merchant)
    assertEquals(45000.0, result.total)
    assertEquals(4500.0, result.tax)
}

@Test
fun `test category classification`() {
    val description = "Makan siang di Warung Padang"
    val category = categoryClassifier.classify(description)
    assertEquals("Food & Dining", category)
}

14.2 Integration Tests
kotlin

@Test
fun `test sync with Google Sheets`() {
    // Arrange
    val transaction = Transaction(
        description = "Test Transaction",
        amount = 100000.0,
        category = "Food & Dining"
    )
    
    // Act
    repository.insertTransaction(transaction)
    val result = syncManager.sync()
    
    // Assert
    assertTrue(result.isSuccess)
    assertTrue(repository.getUnsyncedTransactions().isEmpty())
}

@Test
fun `test OAuth flow`() {
    // Test Google Sign-In
    // Test token refresh
    // Test permission handling
}

14.3 UI Tests
kotlin

@Test
fun `test manual entry flow`() {
    // Navigate to add screen
    composeTestRule.onNodeWithTag("add_fab").performClick()
    composeTestRule.onNodeWithText("Manual Entry").performClick()
    
    // Fill form
    composeTestRule.onNodeWithTag("description_field").performTextInput("Lunch")
    composeTestRule.onNodeWithTag("amount_field").performTextInput("50000")
    composeTestRule.onNodeWithTag("category_dropdown").performClick()
    composeTestRule.onNodeWithText("Food & Dining").performClick()
    
    // Save
    composeTestRule.onNodeWithTag("save_button").performClick()
    
    // Verify
    composeTestRule.onNodeWithText("Transaction saved").assertExists()
}

14.4 Performance Tests
kotlin

@Test
fun `test AI processing time`() {
    val startTime = System.currentTimeMillis()
    aiPipeline.processImage(receiptImage)
    val endTime = System.currentTimeMillis()
    val processingTime = endTime - startTime
    
    assertTrue(processingTime < 5000) // <5 seconds
}

@Test
fun `test database query performance`() {
    // Insert 1000 transactions
    // Query and measure time
    val startTime = System.currentTimeMillis()
    repository.getTransactions(DateRange.LAST_MONTH)
    val endTime = System.currentTimeMillis()
    
    assertTrue((endTime - startTime) < 300) // <300ms
}

14.5 Test Coverage Targets
text

Overall Coverage: 70%
- Domain Layer: 80%
- Data Layer: 70%
- Presentation Layer: 60%
- AI Pipeline: 90%
- Sync Service: 80%

15. IMPLEMENTATION ROADMAP
Phase 1: Foundation (Week 1-2)
text

Week 1:
- Project setup and configuration
- Architecture setup (Clean + MVVM)
- Room database setup
- Dependency injection configuration
- Navigation setup

Week 2:
- Repository pattern implementation
- Google OAuth integration
- Google Sheets API integration
- Basic data models and DAOs
- Unit test setup

Phase 2: Core Features (Week 3-4)
text

Week 3:
- CameraX integration
- Gallery picker
- ML Kit OCR setup
- Gemini API integration
- AI pipeline implementation

Week 4:
- Manual entry UI
- Receipt review UI
- Category system
- Transaction list
- Save flow with sync queue

Phase 3: Dashboard (Week 5-6)
text

Week 5:
- Summary cards
- MPAndroidChart integration
- Pie chart implementation
- Line chart implementation
- Filter system

Week 6:
- Transaction list with search
- Transaction detail view
- Edit/delete functionality
- Pull-to-refresh
- Sync status indicator

Phase 4: Polish & Testing (Week 7-8)
text

Week 7:
- Material 3 theming
- Dark/Light mode
- Animations
- Error handling
- Loading states
- Accessibility improvements

Week 8:
- Comprehensive testing
- Performance optimization
- Localization (EN/ID)
- Analytics integration
- Crash reporting

Phase 5: Production (Week 9-10)
text

Week 9:
- Proguard configuration
- App signing
- Play Store listing
- Screenshots and videos
- Privacy policy
- Documentation

Week 10:
- Beta testing
- Bug fixes
- Performance tuning
- Production release
- Monitoring setup

16. API & DEPENDENCIES
16.1 Google Cloud APIs
gradle

// Google Sign-In
implementation 'com.google.android.gms:play-services-auth:20.7.0'

// Google Sheets API
implementation 'com.google.api-client:google-api-client-android:2.2.0'
implementation 'com.google.apis:google-api-services-sheets:v4-rev20240704-2.0.0'
implementation 'com.google.apis:google-api-services-drive:v3-rev20240212-2.0.0'

// Google ML Kit
implementation 'com.google.mlkit:text-recognition:16.0.0'

// Google Gemini API
implementation 'com.google.ai.client.generativeai:generativeai:0.4.0'

16.2 Android Dependencies
gradle

// Compose
implementation platform('androidx.compose:compose-bom:2024.04.01')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.ui:ui-graphics'
implementation 'androidx.compose.ui:ui-tooling-preview'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.compose.material:material-icons-extended'

// Navigation
implementation 'androidx.navigation:navigation-compose:2.7.7'

// ViewModel
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'

// Room
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// CameraX
implementation 'androidx.camera:camera-camera2:1.4.0'
implementation 'androidx.camera:camera-lifecycle:1.4.0'
implementation 'androidx.camera:camera-view:1.4.0'

// Charts
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// Image Loading
implementation 'io.coil-kt:coil-compose:2.6.0'

// WorkManager
implementation 'androidx.work:work-runtime-ktx:2.9.0'

// Security
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Testing
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.8.0'
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.6.3'

16.3 Environment Setup
kotlin

// BuildConfig Keys
buildConfigField("String", "GEMINI_API_KEY", "\"${project.properties["GEMINI_API_KEY"]}\"")
buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${project.properties["GOOGLE_CLIENT_ID"]}\"")

// Local Properties (local.properties)
GEMINI_API_KEY=your_gemini_api_key_here
GOOGLE_CLIENT_ID=your_google_client_id_here

17. SUCCESS CRITERIA
17.1 Feature Completion

    ✅ Manual entry with AI category suggestion

    ✅ Camera/Gallery receipt scanning

    ✅ AI extraction (description, amount, date, tax, discount)

    ✅ AI category classification

    ✅ Google Sheets sync (real-time)

    ✅ Dashboard with summary cards

    ✅ Pie chart (expense by category)

    ✅ Line chart (monthly trends)

    ✅ Transaction list with search and filters

    ✅ Dark/Light theme

    ✅ Indonesian + English localization

    ✅ Offline support

    ✅ OAuth 2.0 Google Sign-In

17.2 Quality Metrics

    ✅ AI Accuracy ≥ 95% on common receipts

    ✅ Transaction time ≤ 15 seconds

    ✅ Sync time ≤ 5 seconds (online)

    ✅ 70%+ test coverage

    ✅ 99.9% crash-free rate

    ✅ 4.5+ star rating (3 months post-launch)

    ✅ 70%+ weekly active users use photo capture

17.3 User Experience

    ✅ App startup < 2 seconds

    ✅ Smooth animations and transitions

    ✅ Clear error messages

    ✅ Helpful onboarding

    ✅ Responsive design

    ✅ Accessibility support

18. OUT OF SCOPE (v1.0)

❌ Multi-user / Shared budgets

    No team or family accounts

    No sharing transactions

❌ Bank / Credit Card Integration

    No Open Banking APIs

    No automatic transaction import

❌ Recurring Transactions

    No scheduled transactions

    No subscription tracking

❌ Advanced Budgeting

    No budget creation

    No alerts or notifications

❌ iOS Version

    iOS not supported initially

    Future cross-platform consideration

❌ Advanced AI Features

    No natural language query ("How much did I spend on coffee?")

    No predictive analytics

❌ Web Dashboard

    No web version

    Google Sheets serves as web view

❌ Image Recognition Training

    No model training on user data

    Pre-trained models only

19. APPENDICES
Appendix A: Google Cloud Setup
bash

# 1. Create project in Google Cloud Console
# 2. Enable APIs:
#    - Google Sheets API
#    - Google Drive API
#    - Google Gemini API
# 3. Configure OAuth 2.0 credentials:
#    - Application type: Android
#    - SHA-1 fingerprint from keystore
#    - Package name: com.cashflow.ai
# 4. Add API keys to local.properties

Appendix B: Receipt Test Cases
text

Test Case 1: Indonesian Receipt
Store: Toko ABC
Items: 3 items
Total: Rp 75.000
Tax: Rp 7.500
Date: 2026-08-19

Test Case 2: English Receipt
Store: Starbucks
Items: 2 items
Total: $18.50
Tax: $1.85
Date: 2026-08-18

Test Case 3: Handwritten Receipt
Store: Warung Sederhana
Items: 4 items
Total: Rp 45.000
No tax
Date: 2026-08-17

Appendix C: Data Flow Diagrams
text

Transaction Flow:
┌─────────┐    ┌──────────┐    ┌─────────┐    ┌──────────┐
│  Input  │ → │ Validate │ → │  Save   │ → │   Sync   │
│         │    │  & AI    │    │  Local  │    │  Google  │
└─────────┘    └──────────┘    └─────────┘    └──────────┘
                                                   │
                                                   ▼
                                              ┌──────────┐
                                              │ Success  │
                                              └──────────┘

Appendix D: UI Mockups
text

[Placeholder for UI mockups]
- Dashboard View
- Add Transaction Flow
- Receipt Review
- Transactions List
- Settings

Appendix E: Deployment Checklist
text

□ Build signed APK/AAB
□ Test on multiple devices (API 26-34)
□ Verify Google Sign-In
□ Test sync with Google Sheets
□ Verify image upload to Drive
□ Test offline mode
□ Verify crash reporting
□ Check analytics events
□ Review privacy policy
□ Prepare Play Store listing
□ Create screenshots/video
□ Test all languages
□ Verify permissions
□ Check proguard obfuscation
□ Backup signing keys
□ Prepare release notes

CONTACT & SUPPORT

Product Team: product@cashflow.ai
Support: support@cashflow.ai
GitHub: github.com/cashflow-ai/android-app
Documentation: docs.cashflow.ai
DOCUMENT HISTORY
Version	Date	Changes
1.0	2026-08-01	Initial PRD
2.0	2026-08-19	Production-ready enhancements, AI pipeline details, testing strategy

END OF PRD
