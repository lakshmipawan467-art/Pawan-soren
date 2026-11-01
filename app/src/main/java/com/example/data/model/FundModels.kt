package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FundCategory(
    @DocumentId val id: String = "",
    val nameHindi: String = "",
    val nameEnglish: String = "",
    val description: String = "",
    val iconName: String = "account_balance",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Donation(
    @DocumentId val id: String = "",
    val receiptNumber: String = "",
    val donorName: String = "गुमनाम दानदाता",
    val isAnonymous: Boolean = false,
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val paymentMethod: String = "नकद (Cash)",
    val referenceNumber: String = "",
    val utrNumber: String = "",
    val purpose: String = "गाँव विकास हेतु",
    val proofNote: String = "",
    val screenshotUrl: String = "",
    val status: String = "pending", // "pending", "verified", "rejected"
    val isVerified: Boolean = false,
    val rejectionReason: String = "",
    val date: String = "",
    val verifiedAt: Timestamp? = null,
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QrCodeItem(
    @DocumentId val id: String = "",
    val name: String = "गाँव कोष UPI QR",
    val description: String = "भदेसर ग्राम विकास समिति अधिकृत बैंक खाता",
    val upiId: String = "bhadesar.vikas@upi",
    val qrImageUrl: String = "",
    val fundCategoryId: String = "", // empty means All Funds
    val fundCategoryName: String = "सभी फंड / सामान्य कोष",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",
    val referenceId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val voucherNumber: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val purpose: String = "",
    val vendorName: String = "",
    val billNumber: String = "",
    val date: String = "",
    val status: String = "Pending Approval",
    val rejectionReason: String = "",
    val approvedBy: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isVerified: Boolean get() = status == "Verified and Recorded"
}

@IgnoreExtraProperties
data class ProjectItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val estimatedBudget: Double = 0.0,
    val approvedBudget: Double = 0.0,
    val spentAmount: Double = 0.0,
    val startDate: String = "",
    val expectedCompletionDate: String = "",
    val status: String = "प्रगति पर", // नियोजित, प्रगति पर, पूर्ण, स्थगित
    val progressPercent: Int = 0,
    val updates: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NoticeItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "सामान्य सूचना",
    val publishedDate: String = "",
    val authorName: String = "ग्राम विकास समिति",
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ComplaintItem(
    @DocumentId val id: String = "",
    val senderName: String = "",
    val senderContact: String = "",
    val type: String = "रसीद संबंधी", // रसीद संबंधी, प्रविष्टि त्रुटि, दोहराव रिकॉर्ड, खर्च स्पष्टीकरण, अन्य
    val subject: String = "",
    val details: String = "",
    val status: String = "Pending", // Pending, In Progress, Resolved
    val adminResponse: String = "",
    val respondedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLogItem(
    @DocumentId val id: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class SystemConfig(
    @DocumentId val id: String = "",
    val adminEmail: String = "lakshmipawan467@gmail.com",
    val villageName: String = "भदेसर गाँव",
    val villageState: String = "चित्तौड़गढ़, राजस्थान",
    val rulesVersion: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DashboardSummary(
    val totalVerifiedDonations: Double = 0.0,
    val totalVerifiedExpenses: Double = 0.0,
    val currentBalance: Double = 0.0,
    val activeFundsCount: Int = 0,
    val totalProjectsCount: Int = 0,
    val pendingExpenseRequestsCount: Int = 0,
    val pendingComplaintsCount: Int = 0,
    val pendingDonationsCount: Int = 0,
    val pendingDonationsTotal: Double = 0.0,
    val rejectedDonationsCount: Int = 0
)
