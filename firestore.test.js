const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_1";
const BOB_UID = "bob_admin_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validVendorPayload(uid, created) {
  return {
    userId: uid,
    vendorCode: "VEN-1001",
    name: "Aarogya Medical Centre",
    city: "Guwahati",
    type: "Hospital",
    plan: "Enterprise",
    status: "Pending",
    adminEmail: "admin@aarogya.org",
    billingEmail: "billing@aarogya.org",
    phone: "+919876543210",
    address: "GS Road, Guwahati 781005",
    purchasedLicenses: 1,
    activeSeats: 1,
    autoRenew: true,
    subscriptionExpiresAtMillis: Date.now() + 86400000 * 30,
    assignedModulesCsv: "PharmaSuite,LabSuite",
    createdAt: created,
    updatedAt: created,
  };
}

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

test("Unauthenticated user: cannot read or write any workspace collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const created = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vendors").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vendors").doc("VEN-1001").set(
      validVendorPayload(ALICE_UID, created)
    )
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("modules").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("moduleGroups").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("offers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("licenses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notifications").get());
});

test("Owner vendor CRUD with email/phone/address/subscription succeeds; ghost keys & immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const vendorRef = aliceDb.collection("users").doc(ALICE_UID).collection("vendors").doc("VEN-1001");

  await assertSucceeds(vendorRef.set(validVendorPayload(ALICE_UID, created)));

  await assertSucceeds(
    vendorRef.update({
      status: "Active",
      phone: "+919999888877",
      address: "Updated Medical Hub, Guwahati",
      autoRenew: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable field mutation
  await assertFails(
    vendorRef.update({
      vendorCode: "VEN-9999",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vendors").doc("VEN-1002").set({
      id: "VEN-1002",
      ...validVendorPayload(ALICE_UID, created),
    })
  );
});

test("Modules pricing, moduleGroups, offers, licenses, payments terminal lock & append-only notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Module create & price edit
  const modRef = aliceDb.collection("users").doc(ALICE_UID).collection("modules").doc("MOD-PHARMA");
  await assertSucceeds(
    modRef.set({
      userId: ALICE_UID,
      moduleCode: "MOD-PHARMA",
      name: "PharmaSuite",
      description: "Pharmacy operations",
      stage: "Live",
      monthlyPrice: 1499.0,
      annualPrice: 14990.0,
      currency: "INR",
      discountPercent: 10,
      licenseAllEligible: true,
      isActive: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    modRef.update({
      monthlyPrice: 1999.0,
      discountPercent: 15,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Module group with All Modules licensing
  const grpRef = aliceDb.collection("users").doc(ALICE_UID).collection("moduleGroups").doc("GRP-ALL");
  await assertSucceeds(
    grpRef.set({
      userId: ALICE_UID,
      groupCode: "GRP-ALL",
      name: "All Modules Enterprise Suite",
      description: "Full HealthSuite 360 license",
      moduleCodesCsv: "ALL_MODULES",
      bundlePrice: 4999.0,
      discountPercent: 20,
      includesAllModules: true,
      maxSeatsDefault: 5,
      isActive: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Promo Offer
  const offerRef = aliceDb.collection("users").doc(ALICE_UID).collection("offers").doc("HEALTH20");
  await assertSucceeds(
    offerRef.set({
      userId: ALICE_UID,
      code: "HEALTH20",
      title: "20% Off All Modules",
      description: "Annual renewal & new clinic offer",
      discountPercent: 20,
      targetScope: "All Modules",
      validUntilMillis: Date.now() + 86400000 * 14,
      isActive: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // License sharing tracking
  const licRef = aliceDb.collection("users").doc(ALICE_UID).collection("licenses").doc("LIC-1003");
  await assertSucceeds(
    licRef.set({
      userId: ALICE_UID,
      licenseKey: "HS360-VEN1003-ABCD-1234",
      vendorId: "VEN-1003",
      vendorName: "CarePlus Pharmacy",
      vendorEmail: "careplus@pharmacy.in",
      scopeName: "PharmaSuite (1 Seat)",
      purchasedSeats: 1,
      activeDevicesCsv: "dev_a1b2,dev_c3d4,dev_e5f6",
      activeDeviceCount: 3,
      distinctIpCount: 3,
      violationStatus: "Unauthorized Sharing",
      lastHeartbeatMillis: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Payment transaction Pending -> Paid -> locked against further edits or deletion
  const payRef = aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("PAY-001");
  await assertSucceeds(
    payRef.set({
      userId: ALICE_UID,
      orderRef: "ORD-1001",
      vendorId: "VEN-1001",
      vendorName: "Aarogya Medical Centre",
      itemTitle: "All Modules Enterprise Suite",
      scopeType: "All Modules",
      seats: 1,
      billingCycle: "Annual",
      promoCode: "HEALTH20",
      subtotalAmount: 4999.0,
      discountAmount: 999.8,
      finalAmount: 3999.2,
      currency: "INR",
      gatewayMode: "UPI Intent",
      status: "Pending",
      gatewayRef: "",
      checkoutUrl: "",
      autoRenewEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    payRef.update({
      status: "Paid",
      gatewayRef: "UPI_REF_998877",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    payRef.update({
      status: "Failed",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(payRef.delete());

  // Append-only vendor notification
  const notifRef = aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("NOTIF-1");
  await assertSucceeds(
    notifRef.set({
      userId: ALICE_UID,
      vendorId: "VEN-1001",
      vendorName: "Aarogya Medical Centre",
      recipientEmail: "admin@aarogya.org",
      recipientPhone: "+919876543210",
      category: "Offer",
      title: "20% Off All Modules",
      message: "Use code HEALTH20 on renewal",
      deliveryChannel: "System Push + Log",
      status: "Sent",
      createdAt: created,
    })
  );
  await assertFails(notifRef.update({ title: "Tampered" }));
  await assertFails(notifRef.delete());
});

test("Cross-user isolation: Bob cannot read or mutate Alice's commerce or license collections", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("vendors").doc("VEN-1001").set(
      validVendorPayload(ALICE_UID, created)
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vendors").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("licenses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vendors").doc("VEN-1001").update({
      status: "Suspended",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
