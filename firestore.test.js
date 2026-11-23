const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shop_123";
const BOB_UID = "bob_shop_456";
const ADMIN_UID = "admin_user_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
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

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").get()
  );
  await assertFails(
    unauthDb.collection("support_tickets").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(unauthDb.collection("announcements").get());
});

test("UserProfile & BusinessProfile: owner create/update works, shadow fields & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sharma Kirana",
      email: "sharma@example.com",
      planTier: "FREE",
      language: "EN",
      accountStatus: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner cannot self-suspend or inject ghost fields
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "SUSPENDED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner can update language & activeBusinessId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "HI",
      activeBusinessId: "biz_1",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Business create & update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("businesses")
      .doc("biz_1")
      .set({
        userId: ALICE_UID,
        businessName: "Sharma General Store",
        ownerName: "Rajesh Sharma",
        businessType: "Kirana & Grocery",
        upiId: "sharma@okaxis",
        logoUri: "file:///data/user/0/com.aistudio.khatamitra.xqwmzp/files/branding/logo_1.png",
        signatureUri: "file:///data/user/0/com.aistudio.khatamitra.xqwmzp/files/branding/sig_1.png",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("businesses")
      .doc("biz_1")
      .update({
        logoUri: "file:///data/user/0/com.aistudio.khatamitra.xqwmzp/files/branding/logo_2.png",
        signatureUri: "",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Oversized logoUri (> 500 chars) rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("businesses")
      .doc("biz_1")
      .update({
        logoUri: "x".repeat(501),
        updatedAt: new Date(Date.now() - 200),
      })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("businesses")
      .doc("biz_1")
      .get()
  );
});

test("Customers, Suppliers & Audit-Safe Transactions: Kotlin payloads & terminal REVERSED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const nowMs = Math.floor(Date.now()) - 2000;

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customers")
      .doc("cust_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        name: "Rahul Verma",
        phone: "9876543210",
        category: "Regular",
        openingBalance: 0,
        currentBalance: 1500,
        lastTransactionAtMillis: nowMs,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("suppliers")
      .doc("sup_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        name: "Gupta Distributors",
        companyName: "Gupta FMCG Pvt Ltd",
        openingBalance: 0,
        currentBalance: 4200,
        lastTransactionAtMillis: nowMs,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Create transaction (NO @DocumentId `id` key)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        partyId: "cust_1",
        partyName: "Rahul Verma",
        partyType: "CUSTOMER",
        txnType: "CREDIT_GIVEN",
        amount: 1500,
        balanceImpact: 1500,
        paymentMode: "CREDIT",
        description: "Monthly ration",
        status: "ACTIVE",
        txnTimeMillis: nowMs,
        auditHistory: ["Created ₹1500 CREDIT_GIVEN"],
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Edit transaction with audit trail
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_1")
      .update({
        amount: 1600,
        balanceImpact: 1600,
        status: "EDITED",
        auditHistory: [
          "Created ₹1500 CREDIT_GIVEN",
          "Edited to ₹1600",
        ],
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reverse transaction -> terminal state
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_1")
      .update({
        status: "REVERSED",
        reversedByTxnId: "txn_rev_1",
        auditHistory: [
          "Created ₹1500 CREDIT_GIVEN",
          "Edited to ₹1600",
          "Reversed by txn_rev_1",
        ],
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Subsequent edit on REVERSED transaction must fail!
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_1")
      .update({
        amount: 2000,
        status: "EDITED",
        updatedAt: new Date(Date.now() - 100),
      })
  );

  // Deleting transaction is strictly forbidden
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("txn_1")
      .delete()
  );

  // Cross-user list query rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Expenses, Sales, Reminders & Notifications: valid Kotlin writes & queries succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const nowMs = Math.floor(Date.now()) - 2000;

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("expenses")
      .doc("exp_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        amount: 850,
        category: "Electricity",
        paymentMode: "UPI",
        notes: "Shop bill",
        expenseTimeMillis: nowMs,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sales")
      .doc("sale_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        invoiceNumber: "INV-1001",
        customerId: "cust_1",
        customerName: "Rahul Verma",
        productName: "Basmati Rice 25kg",
        quantity: 2,
        unitPrice: 1200,
        discount: 100,
        taxPercent: 5,
        totalAmount: 2415,
        paidAmount: 2000,
        remainingBalance: 415,
        paymentMode: "UPI",
        saleTimeMillis: nowMs,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_1")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        partyId: "cust_1",
        partyName: "Rahul Verma",
        partyPhone: "9876543210",
        amount: 1500,
        channel: "WHATSAPP",
        message: "Hello Rahul, your outstanding balance is ₹1,500.",
        scheduledAtMillis: nowMs,
        status: "SENT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notifications")
      .doc("notif_1")
      .set({
        userId: ALICE_UID,
        title: "Payment Received",
        body: "Received ₹500 from Rahul Verma",
        category: "PAYMENT",
        readFlag: false,
        createdAtMillis: nowMs,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sales").get()
  );
});

test("Support Tickets, Announcements & Admin Privacy Isolation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({ role: "super_admin" });
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("customers")
      .doc("cust_secret")
      .set({
        userId: ALICE_UID,
        businessId: "biz_1",
        name: "Private Customer",
        category: "Regular",
        openingBalance: 0,
        currentBalance: 5000,
        createdAt: new Date(Date.now() - 5000),
        updatedAt: new Date(Date.now() - 5000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "sahib7536@gmail.com",
      email_verified: true,
    })
    .firestore();
  const ts = new Date(Date.now() - 5000);

  // Alice creates a support ticket
  await assertSucceeds(
    aliceDb.collection("support_tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      subject: "Need PDF help",
      description: "How to share PDF statement?",
      category: "Ledger",
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice queries her own tickets cleanly
  await assertSucceeds(
    aliceDb.collection("support_tickets").where("userId", "==", ALICE_UID).get()
  );
  // Unfiltered query on support_tickets fails for non-admin
  await assertFails(aliceDb.collection("support_tickets").get());
  // Bob cannot read Alice's ticket
  await assertFails(bobDb.collection("support_tickets").doc("tkt_1").get());

  // Admin can list & update support tickets and announcements
  await assertSucceeds(adminDb.collection("support_tickets").get());
  await assertSucceeds(
    adminDb.collection("support_tickets").doc("tkt_1").update({
      status: "RESOLVED",
      adminReply: "Tap Share PDF on customer ledger.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_1").set({
      title: "GST Export Ready",
      message: "You can now export CSV & PDF reports.",
      activeFlag: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("announcements").get());

  // Spoofed unverified admin email is rejected
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "sahib7536@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofDb.collection("support_tickets").get());

  // CRITICAL: Admin CANNOT read private customer ledger entries!
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customers")
      .doc("cust_secret")
      .get()
  );
});
