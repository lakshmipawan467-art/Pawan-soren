package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class GaonFundRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun submitComplaint_publicVillager_succeeds() = runBlocking {
        // Public villager unauthenticated
        auth.signOut()
        val repository = GaonFundRepository(firestore)

        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.submitComplaint(
                senderName = "रामलाल मीणा",
                senderContact = "ramlal@example.com",
                type = "रसीद संबंधी",
                subject = "रसीद नहीं मिली",
                details = "कल दिए गए 500 रुपये दान की डिजिटल रसीद नहीं दिख रही।"
            )
        }
        assertTrue("Public complaint should succeed", result.isSuccess)
    }

    @Test
    fun addDonation_adminUser_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.addDonation(
                donorName = "सुरेश कुमार शर्मा",
                isAnonymous = false,
                amount = 2500.0,
                categoryId = "cat_temple",
                categoryName = "मंदिर निर्माण फंड",
                paymentMethod = "यूपीआई (UPI)",
                referenceNumber = "UPI987654321",
                purpose = "मंदिर गुंबद निर्माण हेतु",
                proofNote = "बैंक खाता में जमा",
                date = "2026-11-01",
                isVerified = true
            )
        }
        assertTrue("Admin donation creation should succeed", result.isSuccess)
    }

    @Test
    fun addExpense_adminUser_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.addExpense(
                amount = 1200.0,
                categoryId = "cat_drainage",
                categoryName = "सड़क एवं नाली फंड",
                purpose = "नाली सफाई एवं कीटनाशक छिड़काव",
                vendorName = "ग्राम सफाई दल",
                billNumber = "BILL-401",
                date = "2026-11-01",
                status = "Verified and Recorded"
            )
        }
        assertTrue("Admin expense creation should succeed", result.isSuccess)
    }

    @Test
    fun addDonation_anonymousDonor_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.addDonation(
                donorName = "",
                isAnonymous = true,
                amount = 501.0,
                categoryId = "cat_mandir",
                categoryName = "मंदिर निर्माण फंड",
                paymentMethod = "नकद (Cash)",
                referenceNumber = "CASH-001",
                purpose = "मंदिर सेवा",
                proofNote = "हस्तगत रसीद",
                date = "2026-11-01",
                isVerified = true
            )
        }
        assertTrue("Admin anonymous donation should succeed", result.isSuccess)
    }

    @Test
    fun addDonation_defaultCategoryFallback_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        // Passing empty categoryId should fallback safely to cat_mandir and succeed
        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.addDonation(
                donorName = "मोहन लाल",
                isAnonymous = false,
                amount = 1100.0,
                categoryId = "",
                categoryName = "",
                paymentMethod = "यूपीआई (UPI)",
                referenceNumber = "UPI11223344",
                purpose = "गाँव विकास कार्य",
                proofNote = "ऑनलाइन पुष्टि",
                date = "2026-11-01",
                isVerified = true
            )
        }
        assertTrue("Admin donation with category fallback should succeed", result.isSuccess)
    }

    @Test
    fun addDonation_pendingVerification_andVerify_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        // 1. Add donation in pending state (isVerified = false)
        val addResult = withTimeout(AUTH_TIMEOUT_MS) {
            repository.addDonation(
                donorName = "दिनेश शर्मा",
                isAnonymous = false,
                amount = 5100.0,
                categoryId = "cat_mandir",
                categoryName = "मंदिर निर्माण फंड",
                paymentMethod = "चेक (Cheque)",
                referenceNumber = "CHQ-889900",
                purpose = "मंदिर तोरण द्वार निर्माण",
                proofNote = "चेक क्लीयरेंस प्रतीक्षित",
                date = "2026-11-01",
                isVerified = false
            )
        }
        assertTrue("Pending donation creation should succeed", addResult.isSuccess)
        val donationId = addResult.getOrThrow()

        // 2. Admin verifies the pending donation
        val verifyResult = withTimeout(AUTH_TIMEOUT_MS) {
            repository.verifyDonation(donationId)
        }
        assertTrue("Admin verification of pending donation should succeed", verifyResult.isSuccess)
    }

    @Test
    fun addDonation_unauthenticated_fails() = runBlocking {
        auth.signOut()
        val repository = GaonFundRepository(firestore)

        val result = repository.addDonation(
            donorName = "अवैध प्रविष्टि",
            isAnonymous = false,
            amount = 1000.0,
            categoryId = "cat_mandir",
            categoryName = "मंदिर निर्माण",
            paymentMethod = "Cash",
            referenceNumber = "",
            purpose = "test",
            proofNote = "",
            date = "2026-11-01",
            isVerified = true
        )
        assertTrue("Unauthenticated donation must fail", result.isFailure)
    }

    @Test
    fun observeAuditLogs_unauthenticated_emitsEmptyListWithoutFirestoreError() = runBlocking {
        auth.signOut()
        val repository = GaonFundRepository(firestore)

        var logs: List<com.example.data.model.AuditLogItem>? = null
        withTimeout(AUTH_TIMEOUT_MS) {
            logs = repository.observeAuditLogs().first()
        }
        assertTrue("Unauthenticated query on audit logs must safely emit empty list", logs?.isEmpty() == true)
    }

    @Test
    fun submitDonationApplication_publicVillager_succeeds() = runBlocking {
        auth.signOut()
        val repository = GaonFundRepository(firestore)

        val result = withTimeout(AUTH_TIMEOUT_MS) {
            repository.submitDonationApplication(
                donorName = "महेश कुमार",
                isAnonymous = false,
                amount = 1500.0,
                categoryId = "cat_mandir",
                categoryName = "मंदिर निर्माण फंड",
                utrNumber = "UTR998877665544",
                phoneNumber = "9876543210",
                purpose = "मंदिर सेवा हेतु"
            )
        }
        assertTrue("Public villager donation application should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @Test
    fun rejectDonation_adminUser_succeeds() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = GaonFundRepository(firestore)

        val addResult = withTimeout(AUTH_TIMEOUT_MS) {
            repository.submitDonationApplication(
                donorName = "परीक्षण दानदाता",
                isAnonymous = false,
                amount = 800.0,
                categoryId = "cat_mandir",
                categoryName = "मंदिर निर्माण फंड",
                utrNumber = "UTR001122334455",
                phoneNumber = "9876543210",
                purpose = "दान"
            )
        }
        assertTrue(addResult.isSuccess)
        val donationId = addResult.getOrThrow()

        val rejectResult = withTimeout(AUTH_TIMEOUT_MS) {
            repository.rejectDonation(donationId, "बैंक खाते में UTR प्राप्त नहीं हुआ।")
        }
        assertTrue("Admin rejection should succeed", rejectResult.isSuccess)
    }
}

