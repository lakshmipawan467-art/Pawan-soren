const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-gaonfund-c02ce54b-969a-451c-a1ef-0dec6e8b6dec";
const ADMIN_EMAIL = "lakshmipawan467@gmail.com";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Public unauthenticated users can read categories, donations, and expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertSucceeds(unauthDb.collection("categories").get());
  await assertSucceeds(unauthDb.collection("donations").get());
  await assertSucceeds(unauthDb.collection("expenses").get());
  await assertSucceeds(unauthDb.collection("projects").get());
  await assertSucceeds(unauthDb.collection("notices").get());
});

test("Unauthenticated user cannot create a donation or expense", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("donations").add({
    receiptNumber: "GF-001",
    amount: 5000,
    categoryId: "cat1",
    categoryName: "मंदिर निर्माण फंड",
    isVerified: true,
    date: "2026-11-01"
  }));
  await assertFails(unauthDb.collection("expenses").add({
    voucherNumber: "EX-001",
    amount: 2500,
    categoryId: "cat1",
    purpose: "सीमेंट खरीद",
    status: "Verified and Recorded",
    date: "2026-11-01"
  }));
});

test("Non-admin authenticated user cannot create a donation", async () => {
  const userDb = testEnv.authenticatedContext("regular_villager", { email: "villager@example.com" }).firestore(DATABASE_ID);
  await assertFails(userDb.collection("donations").add({
    receiptNumber: "GF-002",
    amount: 1000,
    categoryId: "cat1",
    categoryName: "मंदिर निर्माण फंड",
    isVerified: true,
    date: "2026-11-01"
  }));
});

test("Admin user can create valid donation, expense, project, and notice", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL }).firestore(DATABASE_ID);
  
  await assertSucceeds(adminDb.collection("categories").doc("cat1").set({
    nameHindi: "मंदिर निर्माण फंड",
    isActive: true
  }));

  const villageAdminDb = testEnv.authenticatedContext("village_admin_uid", { email: "bhadesarbishrupa@gmail.com" }).firestore(DATABASE_ID);
  await assertSucceeds(villageAdminDb.collection("categories").doc("cat2").set({
    nameHindi: "गाँव विकास फंड",
    isActive: true
  }));

  await assertSucceeds(adminDb.collection("donations").doc("don1").set({

    receiptNumber: "GF-BH-2026-0001",
    donorName: "राम प्रसाद",
    isAnonymous: false,
    amount: 5000,
    categoryId: "cat1",
    categoryName: "मंदिर निर्माण फंड",
    isVerified: true,
    date: "2026-11-01"
  }));

  await assertSucceeds(adminDb.collection("expenses").doc("exp1").set({
    voucherNumber: "EX-BH-2026-0001",
    amount: 2000,
    categoryId: "cat1",
    purpose: "लाइटिंग एवं तार खरीद",
    status: "Verified and Recorded",
    date: "2026-11-01"
  }));

  await assertSucceeds(adminDb.collection("projects").doc("proj1").set({
    name: "मंदिर निर्माण",
    categoryId: "cat1",
    approvedBudget: 500000,
    status: "In Progress"
  }));
});

test("Villager can submit a complaint with Pending status, but not update it", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertSucceeds(unauthDb.collection("complaints").doc("comp1").set({
    senderName: "सुरेश कुमार",
    type: "Missing Receipt",
    subject: "रसीद नहीं मिली",
    details: "दिनांक 28 अक्टूबर के 1000 रुपये दान की रसीद नहीं दिखाई दे रही।",
    status: "Pending"
  }));

  // Non-admin cannot resolve/update complaint
  await assertFails(unauthDb.collection("complaints").doc("comp1").update({
    status: "Resolved"
  }));

  // Admin can update complaint with response
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL }).firestore(DATABASE_ID);
  await assertSucceeds(adminDb.collection("complaints").doc("comp1").update({
    status: "Resolved",
    adminResponse: "रसीद संख्या GF-BH-0042 जारी कर दी गई है।"
  }));
});

test("Audit logs can only be created by Admin and cannot be modified or deleted", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL }).firestore(DATABASE_ID);
  await assertSucceeds(adminDb.collection("audit_logs").doc("log1").set({
    adminEmail: ADMIN_EMAIL,
    action: "VERIFY",
    entityType: "DONATION",
    entityId: "don1"
  }));

  // Update must fail (immutable)
  await assertFails(adminDb.collection("audit_logs").doc("log1").update({
    action: "MODIFIED"
  }));

  // Delete must fail
  await assertFails(adminDb.collection("audit_logs").doc("log1").delete());

  // Non-admin cannot read audit logs
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Villager can submit pending donation, but cannot verify it", async () => {
  const villagerDb = testEnv.authenticatedContext("regular_villager", { email: "villager@example.com" }).firestore(DATABASE_ID);
  await assertSucceeds(villagerDb.collection("donations").doc("don_pending").set({
    receiptNumber: "GF-BH-2026-9999",
    donorName: "रमेश चौधरी",
    isAnonymous: false,
    amount: 1100,
    categoryId: "cat1",
    categoryName: "मंदिर निर्माण फंड",
    isVerified: false,
    date: "2026-11-01"
  }));

  // Non-admin cannot verify donation (update isVerified to true)
  await assertFails(villagerDb.collection("donations").doc("don_pending").update({
    isVerified: true
  }));

  // Admin can verify donation
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL }).firestore(DATABASE_ID);
  await assertSucceeds(adminDb.collection("donations").doc("don_pending").update({
    isVerified: true
  }));
});

test("QR Codes can be read publicly, but created/updated only by Admin", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertSucceeds(unauthDb.collection("qr_codes").get());

  // Non-admin cannot create QR code
  await assertFails(unauthDb.collection("qr_codes").doc("qr1").set({
    name: "फर्जी क्यूआर",
    upiId: "fake@upi"
  }));

  // Admin can create QR code
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: ADMIN_EMAIL }).firestore(DATABASE_ID);
  await assertSucceeds(adminDb.collection("qr_codes").doc("qr1").set({
    name: "भदेसर गाँव विकास QR",
    upiId: "bhadesar.vikas@upi"
  }));
});

