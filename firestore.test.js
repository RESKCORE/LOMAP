const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_driver_1";
const BOB_UID = "bob_driver_2";

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

test("Unauthenticated user: cannot read or write driver profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read own driver profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rajesh Kumar",
      email: "rajesh@example.com",
      city: "Bengaluru, KA",
      autoAcceptMillisTarget: 5,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another driver's profile or ride_events", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Driver",
      city: "Mumbai, MH",
      autoAcceptMillisTarget: 5,
      createdAt: now,
      updatedAt: now,
    });
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("ride_events")
      .doc("event_1")
      .set({
        userId: BOB_UID,
        platformId: "UBER",
        fareInr: 240.0,
        isEligible: true,
        acceptanceOutcome: "ACCEPTED_BY_AUTHORIZED_INTEGRATION",
        responseLatencyMs: 1.5,
        pickupLocation: "Andheri",
        destinationLocation: "Bandra",
        createdAt: now,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("ride_events").get()
  );
});

test("Authenticated user: can create and list own ride_events", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ride_events")
      .doc("evt_101")
      .set({
        userId: ALICE_UID,
        platformId: "UBER",
        fareInr: 310.0,
        isEligible: true,
        acceptanceOutcome: "ACCEPTED_BY_AUTHORIZED_INTEGRATION",
        responseLatencyMs: 1.2,
        pickupLocation: "Indiranagar",
        destinationLocation: "Koramangala",
        createdAt: now,
      })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ride_events").get()
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ride_events")
      .doc("evt_ghost")
      .set({
        userId: ALICE_UID,
        platformId: "UBER",
        fareInr: 200.0,
        isEligible: true,
        acceptanceOutcome: "ACCEPTED_BY_AUTHORIZED_INTEGRATION",
        responseLatencyMs: 1.0,
        pickupLocation: "MG Road",
        destinationLocation: "HSR Layout",
        createdAt: now,
        isAdminBypass: true,
      })
  );
});
