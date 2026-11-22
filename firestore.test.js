const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_donor_1";
const BOB_UID = "bob_requester_2";
const HOSPITAL_UID = "hospital_staff_3";
const ADMIN_UID = "admin_user_4";
const ADMIN_EMAIL = "nikethankmnikethan@gmail.com";

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

test("Unauthenticated users cannot read or write any ARSAM collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("donors").where("active", "==", true).get());
  await assertFails(unauth.collection("bloodRequests").where("status", "in", ["OPEN", "ESCALATED"]).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
});

test("Users: owner create/update works; role escalation to ADMIN or verified trustLevel fails; cross-user PII read fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Valid owner profile creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice K",
      role: "DONOR",
      district: "Bengaluru Urban",
      city: "Bengaluru",
      preferredLanguage: "en",
      trustLevel: "UNVERIFIED",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user PII read blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Self-escalation to ADMIN blocked
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      role: "ADMIN",
      district: "Mysuru",
      city: "Mysuru",
      preferredLanguage: "kn",
      trustLevel: "UNVERIFIED",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-verification trustLevel blocked on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trustLevel: "HOSPITAL_AUTHORIZED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid language switch succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "kn",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Donors: filtered active query succeeds; unfiltered query fails; ghost fields & fake verification fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("donors").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice K",
      bloodGroup: "O+",
      district: "Bengaluru Urban",
      city: "Bengaluru",
      approxLat: 12.97,
      approxLng: 77.59,
      availabilityStatus: "AVAILABLE_NOW",
      readinessConfirmed: true,
      trustLevel: "UNVERIFIED",
      qrToken: "ARSAM-DONOR-ALICE1",
      contactPreference: "IN_APP_CONSENT",
      donationCount: 3,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Client-aligned query succeeds
  await assertSucceeds(
    bobDb.collection("donors").where("active", "==", true).get()
  );

  // Unfiltered list fails
  await assertFails(bobDb.collection("donors").get());

  // Shadow/ghost field blocked
  await assertFails(
    bobDb.collection("donors").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      bloodGroup: "A+",
      district: "Mysuru",
      city: "Mysuru",
      approxLat: 12.3,
      approxLng: 76.65,
      availabilityStatus: "AVAILABLE_NOW",
      readinessConfirmed: true,
      trustLevel: "UNVERIFIED",
      qrToken: "ARSAM-DONOR-BOB",
      contactPreference: "IN_APP_CONSENT",
      donationCount: 0,
      active: true,
      phoneNumber: "+919999999999",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("BloodRequests & DonorResponses: Rescue Chain + Safe Contact consent + terminal lock enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // Bob creates emergency request
  await assertSucceeds(
    bobDb.collection("bloodRequests").doc("req_1").set({
      requesterId: BOB_UID,
      requesterName: "Bob R",
      bloodGroup: "O+",
      unitsNeeded: 2,
      hospitalName: "Victoria Hospital",
      district: "Bengaluru Urban",
      city: "Bengaluru",
      approxLat: 12.96,
      approxLng: 77.57,
      requiredBy: "Today 18:00",
      priority: "CRITICAL",
      description: "Urgent surgical need at Victoria Hospital.",
      status: "OPEN",
      rescueStage: "NEARBY_BATCH",
      trustLevel: "UNVERIFIED",
      duplicateFingerprint: "O+|Bengaluru Urban|Victoria Hospital",
      responseCount: 0,
      flaggedSuspicious: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered community list succeeds; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("bloodRequests").where("status", "in", ["OPEN", "ESCALATED"]).get()
  );
  await assertFails(aliceDb.collection("bloodRequests").get());

  // Bob requests Safe Contact from Alice (donorConsented must be false)
  await assertSucceeds(
    bobDb.collection("donorResponses").doc("resp_1").set({
      requestId: "req_1",
      requesterId: BOB_UID,
      donorId: ALICE_UID,
      donorDisplayName: "Alice K",
      donorBloodGroup: "O+",
      requesterName: "Bob R",
      status: "CONTACT_REQUESTED",
      donorConsented: false,
      message: "Need O+ at Victoria Hospital today.",
      participantUids: [BOB_UID, ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot forge Alice's consent
  await assertFails(
    bobDb.collection("donorResponses").doc("resp_1").update({
      donorConsented: true,
      status: "CONSENT_GRANTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice grants Safe Contact consent
  await assertSucceeds(
    aliceDb.collection("donorResponses").doc("resp_1").update({
      donorConsented: true,
      status: "CONSENT_GRANTED",
      sharedContactNote: "Available via hospital blood bank desk at 4 PM",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob marks request FULFILLED -> terminal lock prevents further requester edits
  await assertSucceeds(
    bobDb.collection("bloodRequests").doc("req_1").update({
      status: "FULFILLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("bloodRequests").doc("req_1").update({
      status: "OPEN",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Hospitals, BloodInventory & Admin Trust Gate: spoofing unverified email rejected, verified admin succeeds", async () => {
  const hospitalDb = testEnv.authenticatedContext(HOSPITAL_UID).firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    hospitalDb.collection("bloodBanks").doc("bb_1").set({
      ownerUid: HOSPITAL_UID,
      name: "Kaveri Red Cross Blood Bank",
      orgType: "BLOOD_BANK",
      district: "Bengaluru Urban",
      city: "Bengaluru",
      addressLine: "MG Road, Bengaluru",
      approxLat: 12.97,
      approxLng: 77.61,
      trustLevel: "UNVERIFIED",
      enquiryCount: 0,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    hospitalDb.collection("bloodInventory").doc("inv_1").set({
      orgId: "bb_1",
      orgName: "Kaveri Red Cross Blood Bank",
      orgCollection: "bloodBanks",
      ownerUid: HOSPITAL_UID,
      bloodGroup: "A+",
      componentType: "PRBC",
      unitsAvailable: 14,
      stockStatus: "ADEQUATE",
      expiryDateNote: "Valid 28 days",
      district: "Bengaluru Urban",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Spoofed unverified admin email MUST fail
  await assertFails(
    spoofAdminDb.collection("bloodBanks").doc("bb_1").update({
      trustLevel: "HOSPITAL_AUTHORIZED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    verifiedAdminDb.collection("bloodBanks").doc("bb_1").update({
      trustLevel: "HOSPITAL_AUTHORIZED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp tampering blocked
  await assertFails(
    hospitalDb.collection("bloodInventory").doc("inv_1").update({
      unitsAvailable: 12,
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});
