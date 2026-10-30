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

test("Unauthenticated user: cannot read products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Authenticated owner: can create and read their own products", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1");
  
  await assertSucceeds(prodRef.set({
    name: "Milk 1L",
    barcode: "8901234567890",
    category: "Dairy",
    costPrice: 1.20,
    sellingPrice: 1.80,
    stockQuantity: 50,
    minStockAlert: 10,
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  await assertSucceeds(prodRef.get());
});

test("Cross-user isolation: Bob cannot read or modify Alice's products", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      name: "Milk 1L",
      barcode: "8901234567890",
      costPrice: 1.20,
      sellingPrice: 1.80,
      stockQuantity: 50,
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").delete());
});

test("Schema validation: Invalid product schema is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("bad_prod");
  
  // Missing required costPrice, negative stock, invalid type
  await assertFails(prodRef.set({
    name: "Bad Item",
    barcode: "123",
    stockQuantity: -10,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Transaction immutability: Sales cannot be modified once created", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const saleRef = aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale1");
  
  await assertSucceeds(saleRef.set({
    transactionId: "txn_12345",
    itemsJson: JSON.stringify([{ id: "prod1", name: "Milk", qty: 2, price: 1.80 }]),
    subtotal: 3.60,
    totalAmount: 3.60,
    paymentMethod: "CASH",
    timestamp: new Date()
  }));

  // Updating the sale must be rejected
  await assertFails(saleRef.update({
    totalAmount: 1.00
  }));
});
