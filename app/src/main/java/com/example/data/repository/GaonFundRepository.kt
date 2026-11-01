package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class GaonFundRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireAdminEmail(): String {
        val email = auth.currentUser?.email
        check(!email.isNullOrBlank()) { "Admin must be signed in with an authorized account." }
        return email
    }

    // --- Predefined Categories List ---
    val defaultCategories = listOf(
        FundCategory(id = "cat_mandir", nameHindi = "मंदिर निर्माण फंड", nameEnglish = "Temple Construction Fund", description = "गाँव के मंदिर निर्माण, नवीनीकरण और सौंदर्यीकरण हेतु", iconName = "temple"),
        FundCategory(id = "cat_vikas", nameHindi = "गाँव विकास फंड", nameEnglish = "Village Development Fund", description = "गाँव के सामान्य विकास एवं सामुदायिक कार्यों हेतु", iconName = "home_work"),
        FundCategory(id = "cat_sohrai", nameHindi = "सोहराय पर्व फंड", nameEnglish = "Sohrai Festival Fund", description = "पारंपरिक सोहराय पर्व एवं सांस्कृतिक आयोजनों हेतु", iconName = "celebration"),
        FundCategory(id = "cat_khelkud", nameHindi = "खेलकूद फंड", nameEnglish = "Sports Fund", description = "गाँव के युवाओं हेतु खेल सामग्री एवं खेल प्रतियोगिताओं हेतु", iconName = "sports_cricket"),
        FundCategory(id = "cat_shiksha", nameHindi = "शिक्षा सहायता फंड", nameEnglish = "Education Support Fund", description = "गाँव के जरूरतमंद विद्यार्थियों की शिक्षा एवं छात्रवृत्ति हेतु", iconName = "school"),
        FundCategory(id = "cat_emergency", nameHindi = "आपातकालीन सहायता फंड", nameEnglish = "Emergency Support Fund", description = "चिकित्सा एवं प्राकृतिक आपदा में तत्काल सहायता हेतु", iconName = "emergency"),
        FundCategory(id = "cat_sadak_nali", nameHindi = "सड़क एवं नाली फंड", nameEnglish = "Road & Drainage Fund", description = "गाँव की आंतरिक सड़कों, खड़ंजा व पक्की नालियों के निर्माण हेतु", iconName = "add_road"),
        FundCategory(id = "cat_anya", nameHindi = "अन्य फंड", nameEnglish = "Other Funds", description = "गाँव के अन्य आवश्यक जनहित एवं कल्याणकारी कार्यों हेतु", iconName = "account_balance_wallet")
    )

    val defaultQrCodes = listOf(
        QrCodeItem(
            id = "qr_all_funds",
            name = "समस्त भदेसर गाँव कोष QR",
            description = "गाँव के समस्त सार्वजनिक विकास कार्यों हेतु अधिकृत UPI खाता",
            upiId = "bhadesar.vikas@upi",
            fundCategoryId = "",
            fundCategoryName = "सभी फंड / सामान्य कोष",
            isActive = true
        ),
        QrCodeItem(
            id = "qr_mandir_fund",
            name = "मंदिर निर्माण फंड QR",
            description = "मंदिर जीर्णोद्धार, गुंबद एवं परिसर विकास हेतु अधिकृत खाता",
            upiId = "bhadesar.mandir@upi",
            fundCategoryId = "cat_mandir",
            fundCategoryName = "मंदिर निर्माण फंड",
            isActive = true
        )
    )

    suspend fun seedDefaultCategoriesIfEmpty() {
        val currentEmail = auth.currentUser?.email?.trim()?.lowercase()
        if (currentEmail == null || (currentEmail !in setOf("bhadesarbishrupa@gmail.com", "lakshmipawan467@gmail.com"))) {
            return
        }
        try {
            val snapshot = db.collection("categories").limit(1).get().await()
            if (snapshot.isEmpty) {
                for (cat in defaultCategories) {
                    val docRef = db.collection("categories").document(cat.id)
                    val payload = mapOf(
                        "nameHindi" to cat.nameHindi,
                        "nameEnglish" to cat.nameEnglish,
                        "description" to cat.description,
                        "iconName" to cat.iconName,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
            seedDefaultQrCodesIfEmpty()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories")
        }
    }

    suspend fun seedDefaultQrCodesIfEmpty() {
        try {
            val snapshot = db.collection("qr_codes").limit(1).get().await()
            if (snapshot.isEmpty) {
                for (qr in defaultQrCodes) {
                    val docRef = db.collection("qr_codes").document(qr.id)
                    val payload = mapOf(
                        "name" to qr.name,
                        "description" to qr.description,
                        "upiId" to qr.upiId,
                        "qrImageUrl" to qr.qrImageUrl,
                        "fundCategoryId" to qr.fundCategoryId,
                        "fundCategoryName" to qr.fundCategoryName,
                        "isActive" to qr.isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "qr_codes")
        }
    }

    // --- Real-time Flows ---

    fun observeCategories(): Flow<List<FundCategory>> {
        val path = "categories"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(FundCategory::class.java)
                if (list.isEmpty()) defaultCategories else list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(defaultCategories)
            }
    }

    fun observeDonations(): Flow<List<Donation>> {
        val path = "donations"
        return db.collection(path)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(Donation::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeQrCodes(): Flow<List<QrCodeItem>> {
        val path = "qr_codes"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(QrCodeItem::class.java)
                if (list.isEmpty()) defaultQrCodes else list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(defaultQrCodes)
            }
    }

    fun observeNotifications(): Flow<List<NotificationItem>> {
        val path = "notifications"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(NotificationItem::class.java).sortedByDescending { it.createdAt } }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeExpenses(): Flow<List<Expense>> {
        val path = "expenses"
        return db.collection(path)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(Expense::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeProjects(): Flow<List<ProjectItem>> {
        val path = "projects"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(ProjectItem::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeNotices(): Flow<List<NoticeItem>> {
        val path = "notices"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(NoticeItem::class.java).sortedByDescending { it.isPinned } }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeComplaints(): Flow<List<ComplaintItem>> {
        val path = "complaints"
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(ComplaintItem::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeAuditLogs(): Flow<List<AuditLogItem>> {
        val path = "audit_logs"
        val currentEmail = auth.currentUser?.email?.trim()?.lowercase()
        val isAdminEmail = currentEmail != null && currentEmail in setOf(
            "bhadesarbishrupa@gmail.com",
            "lakshmipawan467@gmail.com"
        )
        if (!isAdminEmail) {
            return flowOf(emptyList())
        }
        return db.collection(path)
            .snapshots()
            .map { snap -> snap.toObjects(AuditLogItem::class.java) }
            .catch { error ->
                if (error is Exception) {
                    val msg = error.message ?: ""
                    if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                        msg.contains("insufficient permissions", ignoreCase = true) ||
                        msg.contains("Error getting Query snapshot", ignoreCase = true)
                    ) {
                        android.util.Log.w("GaonFundRepository", "Audit logs query restricted: $msg")
                    } else {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                }
                emit(emptyList())
            }
    }

    // --- Helpers for unique numbers ---

    fun generateReceiptNumber(): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val randomNum = 1000 + Random.nextInt(9000)
        return "GF-BH-$year-$randomNum"
    }

    fun generateVoucherNumber(): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val randomNum = 1000 + Random.nextInt(9000)
        return "EX-BH-$year-$randomNum"
    }

    // --- Mutations ---

    suspend fun addDonation(
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
        isVerified: Boolean
    ): Result<String> {
        val path = "donations"
        return try {
            val adminEmail = requireAdminEmail()
            val receiptNo = generateReceiptNumber()
            val docRef = db.collection(path).document()
            val cleanCategoryId = categoryId.trim().ifBlank { "cat_mandir" }
            val cleanCategoryName = categoryName.trim().ifBlank { "मंदिर निर्माण फंड" }
            val cleanDate = date.trim().let {
                if (it.length >= 8) it else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            val cleanDonorName = if (isAnonymous) "गुमनाम दानदाता" else donorName.trim().ifBlank { "ग्रामवासी" }

            val payload = mutableMapOf<String, Any>(
                "receiptNumber" to receiptNo,
                "donorName" to cleanDonorName,
                "isAnonymous" to isAnonymous,
                "amount" to amount,
                "categoryId" to cleanCategoryId,
                "categoryName" to cleanCategoryName,
                "paymentMethod" to paymentMethod.ifBlank { "नकद (Cash)" },
                "referenceNumber" to referenceNumber.trim(),
                "utrNumber" to referenceNumber.trim(),
                "purpose" to purpose.trim().ifBlank { "गाँव विकास एवं जनहित" },
                "proofNote" to proofNote.trim(),
                "screenshotUrl" to "",
                "status" to if (isVerified) "verified" else "pending",
                "isVerified" to isVerified,
                "rejectionReason" to "",
                "date" to cleanDate,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isVerified) {
                payload["verifiedAt"] = FieldValue.serverTimestamp()
                payload["verifiedBy"] = adminEmail
            }

            docRef.set(payload).await()
            try {
                recordAuditLog(
                    adminEmail = adminEmail,
                    action = "CREATE",
                    entityType = "DONATION",
                    entityId = docRef.id,
                    newValue = "₹$amount - $cleanCategoryName ($receiptNo)",
                    details = "दान राशि दर्ज की गई (सत्यापित: $isVerified)"
                )
            } catch (_: Exception) {
                // Non-fatal if audit logging encounters an issue
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // Public / Villager Donation Application submission
    suspend fun submitDonationApplication(
        donorName: String,
        isAnonymous: Boolean,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethod: String = "यूपीआई (UPI)",
        referenceNumber: String = "",
        utrNumber: String = "",
        phoneNumber: String = "",
        purpose: String = "गाँव विकास हेतु",
        proofNote: String = "",
        screenshotUrl: String = "",
        date: String = ""
    ): Result<String> {
        val path = "donations"
        return try {
            val receiptNo = generateReceiptNumber()
            val docRef = db.collection(path).document()
            val cleanCategoryId = categoryId.trim().ifBlank { "cat_mandir" }
            val cleanCategoryName = categoryName.trim().ifBlank { "मंदिर निर्माण फंड" }
            val cleanDate = date.trim().let {
                if (it.length >= 8) it else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            val cleanDonorName = if (isAnonymous) "गुमनाम दानदाता" else donorName.trim().ifBlank { "ग्रामवासी" }
            val cleanRef = utrNumber.trim().ifBlank { referenceNumber.trim() }

            val payload = mutableMapOf<String, Any>(
                "receiptNumber" to receiptNo,
                "donorName" to cleanDonorName,
                "isAnonymous" to isAnonymous,
                "amount" to amount,
                "categoryId" to cleanCategoryId,
                "categoryName" to cleanCategoryName,
                "paymentMethod" to paymentMethod.ifBlank { "यूपीआई (UPI)" },
                "referenceNumber" to cleanRef,
                "utrNumber" to cleanRef,
                "purpose" to purpose.trim().ifBlank { "गाँव विकास एवं जनहित" },
                "proofNote" to proofNote.trim(),
                "screenshotUrl" to screenshotUrl.trim(),
                "status" to "pending",
                "isVerified" to false,
                "rejectionReason" to "",
                "date" to cleanDate,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            try {
                sendNotification(
                    title = "नया दान सत्यापन प्रतीक्षित",
                    message = "₹$amount का दान $cleanCategoryName हेतु प्राप्त हुआ (रसीद: $receiptNo, UTR: $cleanRef)।",
                    type = "DONATION_SUBMITTED",
                    referenceId = docRef.id
                )
            } catch (_: Exception) {}
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyDonation(donationId: String): Result<Unit> {
        val path = "donations/$donationId"
        return try {
            val adminEmail = requireAdminEmail()
            // Duplicate Verification Protection via Atomic Transaction
            var alreadyVerified = false
            var donAmount = 0.0
            var donCategory = ""

            db.runTransaction { tx ->
                val docRef = db.collection("donations").document(donationId)
                val snapshot = tx.get(docRef)
                val isCurrentlyVerified = snapshot.getBoolean("isVerified") ?: false
                val currentStatus = snapshot.getString("status") ?: ""

                if (isCurrentlyVerified || currentStatus == "verified") {
                    alreadyVerified = true
                    return@runTransaction
                }

                donAmount = snapshot.getDouble("amount") ?: 0.0
                donCategory = snapshot.getString("categoryName") ?: ""

                tx.update(
                    docRef,
                    mapOf(
                        "isVerified" to true,
                        "status" to "verified",
                        "verifiedAt" to FieldValue.serverTimestamp(),
                        "verifiedBy" to adminEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            if (!alreadyVerified) {
                try {
                    recordAuditLog(
                        adminEmail = adminEmail,
                        action = "VERIFY",
                        entityType = "DONATION",
                        entityId = donationId,
                        details = "दान रिकॉर्ड आधिकारिक रूप से सत्यापित किया गया। राशि फंड बैलेंस में जोड़ी गई।"
                    )
                    sendNotification(
                        title = "दान रिकॉर्ड सत्यापित",
                        message = "₹$donAmount का दान $donCategory में आधिकारिक रूप से सत्यापित एवं कोष में जोड़ा गया।",
                        type = "DONATION_VERIFIED",
                        referenceId = donationId
                    )
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectDonation(donationId: String, reason: String = ""): Result<Unit> {
        val path = "donations/$donationId"
        return try {
            val adminEmail = requireAdminEmail()
            val docRef = db.collection("donations").document(donationId)
            docRef.update(
                mapOf(
                    "isVerified" to false,
                    "status" to "rejected",
                    "rejectionReason" to reason.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            try {
                recordAuditLog(
                    adminEmail = adminEmail,
                    action = "REJECT",
                    entityType = "DONATION",
                    entityId = donationId,
                    details = "दान अस्वीकृत किया गया: ${reason.ifBlank { "बैंक रिकॉर्ड से मिलान न होने के कारण" }}"
                )
                sendNotification(
                    title = "दान अस्वीकृत",
                    message = "दान रिकॉर्ड अस्वीकृत किया गया। कारण: ${reason.ifBlank { "अमान्य विवरण" }}",
                    type = "DONATION_REJECTED",
                    referenceId = donationId
                )
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addOrUpdateQrCode(item: QrCodeItem): Result<String> {
        val adminEmail = requireAdminEmail()
        val path = "qr_codes"
        return try {
            val docRef = if (item.id.isBlank()) db.collection(path).document() else db.collection(path).document(item.id)
            val payload = mapOf(
                "name" to item.name.trim(),
                "description" to item.description.trim(),
                "upiId" to item.upiId.trim(),
                "qrImageUrl" to item.qrImageUrl.trim(),
                "fundCategoryId" to item.fundCategoryId.trim(),
                "fundCategoryName" to item.fundCategoryName.trim(),
                "isActive" to item.isActive,
                "createdAt" to (item.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = if (item.id.isBlank()) "CREATE_QR" else "UPDATE_QR",
                entityType = "QR_CODE",
                entityId = docRef.id,
                newValue = "${item.name} (${item.upiId})",
                details = "QR कोड प्रबंधित किया गया"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleQrCodeActive(id: String, isActive: Boolean): Result<Unit> {
        val adminEmail = requireAdminEmail()
        val path = "qr_codes/$id"
        return try {
            db.collection("qr_codes").document(id).update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "TOGGLE_QR",
                entityType = "QR_CODE",
                entityId = id,
                newValue = if (isActive) "सक्रिय" else "निष्क्रिय",
                details = "QR कोड स्थिति बदली गई"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        type: String = "GENERAL",
        referenceId: String = ""
    ): Result<String> {
        val path = "notifications"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "referenceId" to referenceId,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        amount: Double,
        categoryId: String,
        categoryName: String,
        purpose: String,
        vendorName: String,
        billNumber: String,
        date: String,
        status: String
    ): Result<String> {
        val path = "expenses"
        return try {
            val adminEmail = requireAdminEmail()
            val voucherNo = generateVoucherNumber()
            val docRef = db.collection(path).document()
            val cleanCategoryId = categoryId.trim().ifBlank { "cat_vikas" }
            val cleanCategoryName = categoryName.trim().ifBlank { "गाँव विकास फंड" }
            val cleanDate = date.trim().let {
                if (it.length >= 8) it else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }

            val payload = mapOf(
                "voucherNumber" to voucherNo,
                "amount" to amount,
                "categoryId" to cleanCategoryId,
                "categoryName" to cleanCategoryName,
                "purpose" to purpose.trim().ifBlank { "गाँव विकास कार्य" },
                "vendorName" to vendorName.trim(),
                "billNumber" to billNumber.trim(),
                "date" to cleanDate,
                "status" to status,
                "rejectionReason" to "",
                "approvedBy" to if (status != "Pending Approval") adminEmail else "",
                "verifiedBy" to if (status == "Verified and Recorded") adminEmail else "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            try {
                recordAuditLog(
                    adminEmail = adminEmail,
                    action = "CREATE",
                    entityType = "EXPENSE",
                    entityId = docRef.id,
                    newValue = "₹$amount - $purpose ($voucherNo) [$status]",
                    details = "खर्च अनुरोध दर्ज किया गया"
                )
            } catch (_: Exception) {
                // Non-fatal
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateExpenseStatus(
        expenseId: String,
        newStatus: String,
        reason: String = ""
    ): Result<Unit> {
        val adminEmail = requireAdminEmail()
        val path = "expenses/$expenseId"
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newStatus == "Approved") {
            updates["approvedBy"] = adminEmail
        } else if (newStatus == "Verified and Recorded") {
            updates["verifiedBy"] = adminEmail
        } else if (newStatus == "Rejected") {
            updates["rejectionReason"] = reason
        }

        return try {
            db.collection("expenses").document(expenseId).update(updates).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "UPDATE_STATUS",
                entityType = "EXPENSE",
                entityId = expenseId,
                newValue = newStatus,
                details = "खर्च स्थिति बदलकर $newStatus की गई ${if (reason.isNotBlank()) "कारण: $reason" else ""}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addProject(
        name: String,
        description: String,
        categoryId: String,
        estimatedBudget: Double,
        approvedBudget: Double,
        startDate: String,
        expectedCompletionDate: String,
        status: String
    ): Result<String> {
        val adminEmail = requireAdminEmail()
        val path = "projects"
        val docRef = db.collection(path).document()
        val payload = mapOf(
            "name" to name,
            "description" to description,
            "categoryId" to categoryId,
            "estimatedBudget" to estimatedBudget,
            "approvedBudget" to approvedBudget,
            "spentAmount" to 0.0,
            "startDate" to startDate,
            "expectedCompletionDate" to expectedCompletionDate,
            "status" to status,
            "progressPercent" to 0,
            "updates" to "परियोजना स्वीकृत",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "CREATE",
                entityType = "PROJECT",
                entityId = docRef.id,
                newValue = "$name (बजट: ₹$approvedBudget)",
                details = "नई विकास परियोजना शुरू की गई"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectProgress(
        projectId: String,
        progressPercent: Int,
        status: String,
        updateNote: String,
        spentDelta: Double = 0.0
    ): Result<Unit> {
        val adminEmail = requireAdminEmail()
        val path = "projects/$projectId"
        val updates = mutableMapOf<String, Any>(
            "progressPercent" to progressPercent,
            "status" to status,
            "updates" to updateNote,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (spentDelta > 0) {
            updates["spentAmount"] = FieldValue.increment(spentDelta)
        }

        return try {
            db.collection("projects").document(projectId).update(updates).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "UPDATE",
                entityType = "PROJECT",
                entityId = projectId,
                newValue = "प्रगति $progressPercent% - $status",
                details = updateNote
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addNotice(
        title: String,
        content: String,
        category: String,
        publishedDate: String,
        isPinned: Boolean
    ): Result<String> {
        val adminEmail = requireAdminEmail()
        val path = "notices"
        val docRef = db.collection(path).document()
        val payload = mapOf(
            "title" to title,
            "content" to content,
            "category" to category,
            "publishedDate" to publishedDate,
            "authorName" to "भदेसर ग्राम विकास समिति",
            "isPinned" to isPinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "CREATE",
                entityType = "NOTICE",
                entityId = docRef.id,
                newValue = title,
                details = "नई सूचना प्रकाशित की गई"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        val adminEmail = requireAdminEmail()
        val path = "notices/$noticeId"
        return try {
            db.collection("notices").document(noticeId).delete().await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "DELETE",
                entityType = "NOTICE",
                entityId = noticeId,
                details = "सूचना हटाई गई"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // Public submission (Villagers without admin account)
    suspend fun submitComplaint(
        senderName: String,
        senderContact: String,
        type: String,
        subject: String,
        details: String
    ): Result<String> {
        val path = "complaints"
        val docRef = db.collection(path).document()
        val payload = mapOf(
            "senderName" to senderName.ifBlank { "ग्रामवासी" },
            "senderContact" to senderContact,
            "type" to type,
            "subject" to subject,
            "details" to details,
            "status" to "Pending",
            "adminResponse" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToComplaint(
        complaintId: String,
        adminResponse: String,
        status: String
    ): Result<Unit> {
        val adminEmail = requireAdminEmail()
        val path = "complaints/$complaintId"
        val updates = mapOf(
            "adminResponse" to adminResponse,
            "status" to status,
            "respondedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.collection("complaints").document(complaintId).update(updates).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "RESPOND",
                entityType = "COMPLAINT",
                entityId = complaintId,
                newValue = status,
                details = "शिकायत का उत्तर दिया गया"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCategory(
        nameHindi: String,
        nameEnglish: String,
        description: String,
        iconName: String
    ): Result<String> {
        val adminEmail = requireAdminEmail()
        val path = "categories"
        val docRef = db.collection(path).document()
        val payload = mapOf(
            "nameHindi" to nameHindi,
            "nameEnglish" to nameEnglish,
            "description" to description,
            "iconName" to iconName,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            recordAuditLog(
                adminEmail = adminEmail,
                action = "CREATE",
                entityType = "CATEGORY",
                entityId = docRef.id,
                newValue = nameHindi,
                details = "नया फंड श्रेणी जोड़ा गया"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun recordAuditLog(
        adminEmail: String,
        action: String,
        entityType: String,
        entityId: String,
        previousValue: String = "",
        newValue: String = "",
        details: String = ""
    ) {
        val path = "audit_logs"
        try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "adminEmail" to adminEmail,
                "action" to action,
                "entityType" to entityType,
                "entityId" to entityId,
                "previousValue" to previousValue,
                "newValue" to newValue,
                "details" to details,
                "timestamp" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }
}
