package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogItem
import com.example.data.model.ComplaintItem
import com.example.data.model.DashboardSummary
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.FundCategory
import com.example.data.model.NoticeItem
import com.example.data.model.ProjectItem
import com.example.data.repository.GaonFundRepository
import com.example.ui.auth.GoogleAuthHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GaonFundViewModel(val repository: GaonFundRepository) : ViewModel() {

    val categories: StateFlow<List<FundCategory>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.defaultCategories)

    val donations: StateFlow<List<Donation>> = repository.observeDonations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectItem>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<NoticeItem>> = repository.observeNotices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaints: StateFlow<List<ComplaintItem>> = repository.observeComplaints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qrCodes: StateFlow<List<com.example.data.model.QrCodeItem>> = repository.observeQrCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.defaultQrCodes)

    val notifications: StateFlow<List<com.example.data.model.NotificationItem>> = repository.observeNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogs: StateFlow<List<AuditLogItem>> = GoogleAuthHelper.isAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) {
                repository.observeAuditLogs()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Home Dashboard balance & summary strictly calculated from verified records only!
    val summary: StateFlow<DashboardSummary> = combine(
        donations,
        expenses,
        categories,
        projects,
        complaints
    ) { donList, expList, catList, projList, compList ->
        val totalVerifiedDonations = donList.filter { it.isVerified || it.status == "verified" }.sumOf { it.amount }
        val totalVerifiedExpenses = expList.filter { it.status == "Verified and Recorded" }.sumOf { it.amount }
        val currentBalance = totalVerifiedDonations - totalVerifiedExpenses
        val activeFundsCount = catList.count { it.isActive }
        val totalProjectsCount = projList.size
        val pendingExpenseRequestsCount = expList.count {
            it.status == "Pending Approval" || it.status == "Payment Pending" || it.status == "Paid – Awaiting Verification"
        }
        val pendingComplaintsCount = compList.count { it.status == "Pending" }
        val pendingDonationsCount = donList.count { !it.isVerified && it.status != "rejected" }
        val pendingDonationsTotal = donList.filter { !it.isVerified && it.status != "rejected" }.sumOf { it.amount }
        val rejectedDonationsCount = donList.count { it.status == "rejected" }

        DashboardSummary(
            totalVerifiedDonations = totalVerifiedDonations,
            totalVerifiedExpenses = totalVerifiedExpenses,
            currentBalance = currentBalance,
            activeFundsCount = activeFundsCount,
            totalProjectsCount = totalProjectsCount,
            pendingExpenseRequestsCount = pendingExpenseRequestsCount,
            pendingComplaintsCount = pendingComplaintsCount,
            pendingDonationsCount = pendingDonationsCount,
            pendingDonationsTotal = pendingDonationsTotal,
            rejectedDonationsCount = rejectedDonationsCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfEmpty()
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun addDonation(
        donorName: String,
        isAnonymous: Boolean,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethod: String,
        referenceNumber: String,
        purpose: String,
        proofNote: String,
        date: String,
        isVerified: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addDonation(
                donorName = donorName,
                isAnonymous = isAnonymous,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber,
                purpose = purpose,
                proofNote = proofNote,
                date = date,
                isVerified = isVerified
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = if (isVerified) {
                    "दान सफलतापूर्वक दर्ज एवं सत्यापित किया गया (रसीद संख्या जारी)"
                } else {
                    "दान सफलतापूर्वक दर्ज किया गया (स्थिति: सत्यापन प्रतीक्षित)"
                }
                onSuccess()
            } else {
                _userMessage.value = "दान दर्ज करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // Public villager donation application submission
    fun submitDonationApplication(
        donorName: String,
        isAnonymous: Boolean,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethod: String,
        referenceNumber: String,
        utrNumber: String = "",
        purpose: String,
        proofNote: String,
        screenshotUrl: String = "",
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.submitDonationApplication(
                donorName = donorName,
                isAnonymous = isAnonymous,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber,
                utrNumber = utrNumber,
                purpose = purpose,
                proofNote = proofNote,
                screenshotUrl = screenshotUrl,
                date = date
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "दान अंशदान सफलतापूर्वक सबमिट हुआ। बैंक सत्यापन के उपरांत आधिकारिक फंड में जुड़ेगा।"
                onSuccess()
            } else {
                _userMessage.value = "दान सबमिट करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun verifyDonation(donationId: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.verifyDonation(donationId)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "दान रिकॉर्ड सत्यापित किया गया। फंड बैलेंस अपडेट हुआ।"
            } else {
                _userMessage.value = "सत्यापन विफल: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun rejectDonation(donationId: String, reason: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.rejectDonation(donationId, reason)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "दान रिकॉर्ड अस्वीकृत चिह्नित किया गया।"
                onSuccess()
            } else {
                _userMessage.value = "अस्वीकार करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addOrUpdateQrCode(item: com.example.data.model.QrCodeItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addOrUpdateQrCode(item)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "QR कोड सफलतापूर्वक सहेजा गया।"
                onSuccess()
            } else {
                _userMessage.value = "QR कोड त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addOrUpdateQrCode(
        id: String = "",
        name: String,
        description: String,
        upiId: String,
        fundCategoryId: String = "",
        fundCategoryName: String = "",
        isActive: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        val item = com.example.data.model.QrCodeItem(
            id = id,
            name = name,
            description = description,
            upiId = upiId,
            fundCategoryId = fundCategoryId,
            fundCategoryName = fundCategoryName,
            isActive = isActive
        )
        addOrUpdateQrCode(item, onSuccess)
    }

    fun toggleQrCodeActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.toggleQrCodeActive(id, isActive)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = if (isActive) "QR कोड सक्रिय किया गया" else "QR कोड निष्क्रिय किया गया"
            } else {
                _userMessage.value = "स्थिति अपडेट त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addExpense(
        amount: Double,
        categoryId: String,
        categoryName: String,
        purpose: String,
        vendorName: String,
        billNumber: String,
        date: String,
        status: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addExpense(
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                purpose = purpose,
                vendorName = vendorName,
                billNumber = billNumber,
                date = date,
                status = status
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "खर्च अनुरोध सफलतापूर्वक दर्ज किया गया (वाउचर तैयार)"
                onSuccess()
            } else {
                _userMessage.value = "खर्च दर्ज करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateExpenseStatus(expenseId: String, newStatus: String, reason: String = "") {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.updateExpenseStatus(expenseId, newStatus, reason)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "खर्च स्थिति बदलकर '$newStatus' कर दी गई。"
            } else {
                _userMessage.value = "स्थिति अपडेट विफल: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addProject(
        name: String,
        description: String,
        categoryId: String,
        estimatedBudget: Double,
        approvedBudget: Double,
        startDate: String,
        expectedCompletionDate: String,
        status: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addProject(
                name = name,
                description = description,
                categoryId = categoryId,
                estimatedBudget = estimatedBudget,
                approvedBudget = approvedBudget,
                startDate = startDate,
                expectedCompletionDate = expectedCompletionDate,
                status = status
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "नई विकास परियोजना जोड़ी गई"
                onSuccess()
            } else {
                _userMessage.value = "परियोजना जोड़ने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateProjectProgress(
        projectId: String,
        progressPercent: Int,
        status: String,
        updateNote: String,
        spentDelta: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.updateProjectProgress(
                projectId = projectId,
                progressPercent = progressPercent,
                status = status,
                updateNote = updateNote,
                spentDelta = spentDelta
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "परियोजना प्रगति अपडेट की गई"
                onSuccess()
            } else {
                _userMessage.value = "अपडेट विफल: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addNotice(
        title: String,
        content: String,
        category: String,
        publishedDate: String,
        isPinned: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addNotice(
                title = title,
                content = content,
                category = category,
                publishedDate = publishedDate,
                isPinned = isPinned
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "नई सूचना प्रकाशित कर दी गई"
                onSuccess()
            } else {
                _userMessage.value = "सूचना जोड़ने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.deleteNotice(noticeId)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "सूचना हटा दी गई"
            } else {
                _userMessage.value = "सूचना हटाने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun submitComplaint(
        senderName: String,
        senderContact: String,
        type: String,
        subject: String,
        details: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.submitComplaint(
                senderName = senderName,
                senderContact = senderContact,
                type = type,
                subject = subject,
                details = details
            )
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "आपकी शिकायत / प्रश्न सफलतापूर्वक दर्ज किया गया। व्यवस्थापक शीघ्र समीक्षा करेंगे।"
                onSuccess()
            } else {
                _userMessage.value = "शिकायत दर्ज करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun respondToComplaint(complaintId: String, response: String, status: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.respondToComplaint(complaintId, response, status)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "शिकायत का आधिकारिक उत्तर दर्ज कर दिया गया"
                onSuccess()
            } else {
                _userMessage.value = "उत्तर दर्ज करने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addCategory(
        nameHindi: String,
        nameEnglish: String,
        description: String,
        iconName: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.addCategory(nameHindi, nameEnglish, description, iconName)
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "नया फंड श्रेणी सफलतापूर्वक जोड़ा गया"
                onSuccess()
            } else {
                _userMessage.value = "फंड जोड़ने में त्रुटि: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }
}

class GaonFundViewModelFactory(private val repository: GaonFundRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GaonFundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GaonFundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
