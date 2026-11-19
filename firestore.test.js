const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      preferredTheme: "DARK",
      gridColumns: 3,
      gridViewMode: "COMPACT",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("aiCreations").get()
  );
});

test("Owner can create, read, and partially update UserProfile; rejects ghost fields & invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create valid profile matching Kotlin toCreateMap() (no id, optional email/photoUrl present)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aklilu Tibebu",
      email: "aklilu@example.com",
      photoUrl: "https://example.com/avatar.jpg",
      preferredTheme: "DARK",
      gridColumns: 3,
      gridViewMode: "COMPACT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Read own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update preserving immutable userId & createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredTheme: "LIGHT",
      gridColumns: 4,
      gridViewMode: "DETAILED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field (shadow update attack)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid gridColumns out of bounds (2..5)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gridColumns: 99,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject tampering createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gridColumns: 2,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's profile, favorites, or AI creations", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredTheme: "DARK",
      gridColumns: 3,
      gridViewMode: "COMPACT",
      createdAt: past,
      updatedAt: past,
    });
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_42")
      .set({
        userId: ALICE_UID,
        mediaId: 42,
        displayName: "sunset.jpg",
        mediaType: "IMAGE",
        bucketName: "Camera",
        sizeBytes: 2048000,
        createdAt: past,
        updatedAt: past,
      });
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("aiCreations")
      .doc("ai_1")
      .set({
        userId: ALICE_UID,
        mode: "VISION_COACH",
        modelId: "gemini-3.8-flash",
        prompt: "Analyze lighting",
        outputSummary: "Warm golden hour exposure.",
        createdAt: past,
        updatedAt: past,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_42")
      .delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("aiCreations").get()
  );
});

test("Owner can list, create, update, and delete CloudFavorite & AiCreationRecord matching Kotlin queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Subcollection empty list must succeed
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .orderBy("updatedAt", "desc")
      .get()
  );

  // Create favorite without 'id' field (uses @DocumentId)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_101")
      .set({
        userId: ALICE_UID,
        mediaId: 101,
        displayName: "madda_walabu.mp4",
        mediaType: "VIDEO",
        bucketName: "Movies",
        sizeBytes: 8400123,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject creating favorite with forbidden 'id' field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_102")
      .set({
        id: "fav_102",
        userId: ALICE_UID,
        mediaId: 102,
        displayName: "bad.jpg",
        mediaType: "IMAGE",
        bucketName: "Camera",
        sizeBytes: 100,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Create AI Creation record
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("aiCreations")
      .doc("creation_001")
      .set({
        userId: ALICE_UID,
        mode: "IMAGE_STUDIO",
        modelId: "gemini-nano-banana-2.1",
        prompt: "Golden sunset over Bale Mountains",
        outputSummary: "Generated 1:1 image",
        createdAt: past,
        updatedAt: past,
      })
  );

  // List AI Creations ordered by createdAt desc (matches Kotlin repo query)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("aiCreations")
      .orderBy("createdAt", "desc")
      .limit(25)
      .get()
  );

  // Delete AI Creation record
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("aiCreations")
      .doc("creation_001")
      .delete()
  );
});
