const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated users are completely blocked from reading or writing any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("appSettings").doc("main").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customLogos").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc-1").set({
      id: "acc-1",
      name: "BRI",
      type: "Bank",
      initialBalance: 0,
      currentBalance: 100000,
      icon: "Bank",
      color: 0xFF3B82F6,
      updatedAt: Date.now(),
      isDeleted: false,
    })
  );
});

test("Owner can create, update, soft-delete, and list core financial collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const nowTs = Date.now();
  const pastServerDate = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      photoUrl: "https://example.com/alice.png",
      lastSyncedAt: nowTs,
      createdAt: pastServerDate,
      updatedAt: nowTs,
      serverUpdatedAt: pastServerDate,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc-uuid-1").set({
      id: "acc-uuid-1",
      name: "BRI Utama",
      type: "Bank · IDR",
      initialBalance: 500000,
      currentBalance: 500000,
      icon: "custom:/data/user/0/com.example/files/custom_icons/wallet_123.png",
      color: 0xFF3B82F6,
      currency: "IDR",
      excludeFromNetWorth: false,
      excludeFromBudget: false,
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat-uuid-1").set({
      id: "cat-uuid-1",
      name: "Food & Dining",
      type: "EXPENSE",
      icon: "Restaurant",
      color: 0xFFF59E0B,
      isVisible: true,
      subcategoriesJson: "[]",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx-uuid-1");
  await assertSucceeds(
    txRef.set({
      id: "tx-uuid-1",
      title: "Sarapan",
      type: "EXPENSE",
      amount: 25000,
      categoryId: "Food & Dining",
      subcategory: "",
      accountId: "acc-uuid-1",
      accountName: "BRI Utama",
      toAccountId: "",
      toAccountName: "",
      note: "Nasi uduk",
      label: "",
      recurrence: "Tidak pernah",
      date: nowTs,
      dayOfMonth: 16,
      monthIndex: 11,
      year: 2026,
      dayOfWeekName: "Sen",
      time: "08:00",
      transferFee: 0,
      targetAssetId: "",
      targetAssetName: "",
      targetDebtId: "",
      targetDebtName: "",
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  await assertSucceeds(
    txRef.update({
      amount: 30000,
      note: "Nasi uduk + teh",
      updatedAt: nowTs + 1000,
      serverUpdatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    txRef.update({
      isDeleted: true,
      updatedAt: nowTs + 2000,
      serverUpdatedAt: new Date(Date.now() - 200),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("debts").doc("debt-uuid-1").set({
      id: "debt-uuid-1",
      personName: "Budi",
      amount: 1000000,
      paidAmount: 250000,
      type: "BORROWED",
      dueDate: "1 Des 2026",
      isPaid: false,
      recordsJson: "[]",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assets").doc("asset-uuid-1").set({
      id: "asset-uuid-1",
      name: "Saham BBCA",
      groupName: "Investasi",
      assetType: "Saham",
      value: 5000000,
      platform: "Stockbit",
      performancePercent: 5.5,
      currency: "IDR",
      recordsJson: "[]",
      iconUri: "custom:/data/user/0/com.example/files/custom_icons/asset_123.png",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );
});

test("Owner can sync labels, budgets, recurring, financialPlans, events, savingsGoals, timelineScenarios, appSettings, and customLogos", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const nowTs = Date.now();
  const pastServerDate = new Date(Date.now() - 1000);

  // Label
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("labels").doc("lbl_investasi").set({
      id: "lbl_investasi",
      name: "Investasi",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Budget
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("budget-1").set({
      id: "budget-1",
      category: "Food & Drink",
      allocated: 1500000,
      spent: 250000,
      name: "Makan Bulanan",
      categoriesJson: "[\"Food & Drink\"]",
      accountNamesJson: "[]",
      frequency: "Bulanan",
      customStartDate: "",
      customEndDate: "",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Recurring Transaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recurringTransactions").doc("rec-1").set({
      id: "rec-1",
      title: "Internet Fiber",
      amount: 350000,
      type: "EXPENSE",
      category: "Bills & Fees",
      subcategory: "",
      accountName: "BRI Utama",
      dayOfMonth: 20,
      frequency: "Setiap bulan",
      isActive: true,
      autoDeduct: true,
      reminderOptionsJson: "[\"Pada hari jatuh tempo\"]",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Financial Plan
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("financialPlans").doc("plan_default").set({
      id: "plan_default",
      title: "Pembagian Gaji",
      monthIndex: 11,
      year: 2026,
      itemsJson: "[{\"id\":\"p1\",\"name\":\"Gaji\",\"amount\":5000000,\"section\":\"INCOME\"}]",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Event
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("ev-1").set({
      id: "ev-1",
      name: "Liburan Bali",
      eventType: "Perjalanan",
      targetBudget: 5000000,
      monthIndex: 12,
      year: 2026,
      colorHex: 0xFF3B82F6,
      plannedExpensesJson: "[]",
      isCompleted: false,
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Savings Goal
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals").doc("sg-1").set({
      id: "sg-1",
      name: "Dana Darurat",
      targetAmount: 20000000,
      currentAmount: 5000000,
      colorHex: 0xFF10B981,
      isCompleted: false,
      recordsJson: "[]",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // Timeline Scenario
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("timelineScenarios").doc("ts-1").set({
      id: "ts-1",
      amount: 1000000,
      isIncome: true,
      note: "Bonus Akhir Tahun",
      date: "25/12/2026",
      recurrence: "Hanya sekali",
      sortOrder: 0,
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // App Settings (create & update)
  const settingsRef = aliceDb.collection("users").doc(ALICE_UID).collection("appSettings").doc("main");
  await assertSucceeds(
    settingsRef.set({
      id: "main",
      settingsJson: "{\"language\":\"Indonesia\",\"isDarkMode\":true}",
      activePlanId: "plan_default",
      planTitle: "Pembagian Gaji",
      selectedWalletFilter: "Semua",
      upcomingItemsJson: "[]",
      updatedAt: nowTs,
      serverUpdatedAt: pastServerDate,
    })
  );
  await assertSucceeds(
    settingsRef.update({
      settingsJson: "{\"language\":\"Indonesia\",\"isDarkMode\":false}",
      selectedWalletFilter: "BRI Utama",
      updatedAt: nowTs + 1000,
      serverUpdatedAt: new Date(Date.now() - 200),
    })
  );

  // Custom Logo
  const logoRef = aliceDb.collection("users").doc(ALICE_UID).collection("customLogos").doc("logo_wallet_123_png");
  await assertSucceeds(
    logoRef.set({
      id: "logo_wallet_123_png",
      fileName: "wallet_123.png",
      base64Data: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
      updatedAt: nowTs,
      isDeleted: false,
      serverUpdatedAt: pastServerDate,
    })
  );

  // List all new collections
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("labels").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("recurringTransactions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("financialPlans").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("events").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("timelineScenarios").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("appSettings").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("customLogos").get());
});

test("Cross-user access, shadow fields, ID mismatch, oversized custom logo, and tampered future timestamps are rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).collection("budgets").doc("bob-budget-1").set({
      id: "bob-budget-1",
      category: "Shopping",
      allocated: 500000,
      updatedAt: Date.now(),
      isDeleted: false,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cross-user read/list/write blocked
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("budgets").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("customLogos").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("appSettings").doc("main").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("budgets").doc("bob-budget-1").update({
      allocated: 999999,
      updatedAt: Date.now(),
    })
  );

  // Ghost / shadow field rejected on new collection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("appSettings").doc("main").set({
      id: "main",
      settingsJson: "{}",
      updatedAt: Date.now(),
      isAdmin: true,
    })
  );

  // ID mismatch rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals").doc("sg-a").set({
      id: "sg-b",
      name: "Dana",
      targetAmount: 1000,
      currentAmount: 0,
      updatedAt: Date.now(),
      isDeleted: false,
    })
  );

  // Oversized customLogo base64 (> 700KB) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customLogos").doc("logo_big").set({
      id: "logo_big",
      fileName: "big.png",
      base64Data: "A".repeat(700001),
      updatedAt: Date.now(),
      isDeleted: false,
    })
  );

  // Tampered future serverTimestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("ev-future").set({
      id: "ev-future",
      name: "Event Future",
      targetBudget: 1000,
      updatedAt: Date.now(),
      isDeleted: false,
      serverUpdatedAt: new Date(Date.now() + 60000),
    })
  );
});
