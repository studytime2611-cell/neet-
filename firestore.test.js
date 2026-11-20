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

const pastDate = () => new Date(Date.now() - 5000);

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
  const userRef = unauthDb.collection("users").doc(ALICE_UID);

  await assertFails(userRef.get());
  await assertFails(userRef.collection("tasks").get());
  await assertFails(userRef.collection("chapters").get());
  await assertFails(userRef.collection("sessions").get());
  await assertFails(userRef.collection("mistakes").get());
  await assertFails(userRef.collection("revisions").get());
  await assertFails(userRef.collection("tests").get());
  await assertFails(userRef.collection("upcomingTests").get());
  await assertFails(userRef.collection("mockLabs").get());
  await assertFails(userRef.collection("vault").get());
  await assertFails(userRef.collection("quizzes").get());
  await assertFails(userRef.collection("quizAttempts").get());

  await assertFails(
    userRef.collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Unauthorized mission",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's documents", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const bobRef = db.collection("users").doc(BOB_UID);
    const ts = pastDate();
    await bobRef.set({
      userId: BOB_UID,
      name: "Bob",
      currentRank: "4 Lakh",
      upcomingRank: "10k",
      examDate: "2027-05-02",
      examDateSet: false,
      mockTarget: 650,
      syllabusLoaded: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await bobRef.collection("tasks").doc("bob_t1").set({
      userId: BOB_UID,
      date: "2026-11-20",
      text: "Bob Physics",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRef = aliceDb.collection("users").doc(BOB_UID);

  await assertFails(bobRef.get());
  await assertFails(bobRef.collection("tasks").get());
  await assertFails(bobRef.collection("tasks").doc("bob_t1").get());
  await assertFails(bobRef.collection("tasks").doc("bob_t1").update({ done: true, updatedAt: pastDate() }));
  await assertFails(bobRef.collection("tasks").doc("bob_t1").delete());
  await assertFails(
    bobRef.collection("tasks").doc("inject_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Injected",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Owner can create, update, list, and delete across all IGRIS collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastDate();

  // UserProfile
  await assertSucceeds(
    userRef.set({
      userId: ALICE_UID,
      name: "IGRIS",
      currentRank: "4 Lakh",
      upcomingRank: "10k",
      examDate: "2027-05-02",
      examDateSet: false,
      mockTarget: 650,
      syllabusLoaded: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    userRef.update({
      currentRank: "50k",
      upcomingRank: "5k",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // MissionTask
  await assertSucceeds(
    userRef.collection("tasks").doc("t_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Kinematics 30 MCQs",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      focusMins: 45,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    userRef.collection("tasks").doc("t_1").update({
      done: true,
      doneAt: "2026-11-20",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(userRef.collection("tasks").get());

  // SubjectChapter
  await assertSucceeds(
    userRef.collection("chapters").doc("ch_1").set({
      userId: ALICE_UID,
      subject: "Physics",
      name: "Kinematics",
      done: false,
      strength: "Medium",
      weakTopics: "",
      ticks: { module: "2026-11-20" },
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    userRef.collection("chapters").doc("ch_1").update({
      done: true,
      strength: "Strong",
      ticks: { module: "2026-11-20", r1: "2026-11-20" },
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // FocusSession
  await assertSucceeds(
    userRef.collection("sessions").doc("s_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      subject: "Physics",
      minutes: 45,
      questions: 30,
      topic: "Kinematics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // MistakeEntry
  await assertSucceeds(
    userRef.collection("mistakes").doc("m_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      subject: "Physics",
      type: "Calculation",
      chapter: "Kinematics",
      error: "Sign error in relative velocity",
      fix: "Draw frame axis first",
      closed: false,
      fixStreak: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    userRef.collection("mistakes").doc("m_1").update({
      fixStreak: 2,
      closed: true,
      closedAt: "2026-11-20",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // RevisionEntry
  await assertSucceeds(
    userRef.collection("revisions").doc("r_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      r3: "2026-11-23",
      r7: "2026-11-27",
      r21: "2026-12-11",
      chapter: "Cell Cycle",
      subject: "Biology",
      kind: "NCERT",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // MockTestEntry
  await assertSucceeds(
    userRef.collection("tests").doc("mt_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      name: "Full Test 01",
      score: 620,
      accuracy: 85,
      physics: 150,
      chemistry: 150,
      biology: 320,
      analysis: "",
      analysisDone: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // UpcomingTestEntry
  await assertSucceeds(
    userRef.collection("upcomingTests").doc("up_1").set({
      userId: ALICE_UID,
      name: "Allen Part Test",
      date: "2026-12-01",
      time: "14:00",
      syllabus: "Electrostatics + Cell Cycle",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // MockLabPaper
  await assertSucceeds(
    userRef.collection("mockLabs").doc("ml_1").set({
      userId: ALICE_UID,
      name: "Mock Lab 1",
      date: "2026-11-20",
      physicsC: 35,
      physicsW: 5,
      chemistryC: 38,
      chemistryW: 4,
      botanyC: 40,
      botanyW: 2,
      zoologyC: 41,
      zoologyW: 3,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // VaultEntry
  await assertSucceeds(
    userRef.collection("vault").doc("v_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      title: "Organic Mindmap",
      subject: "Chemistry",
      chapter: "Hydrocarbons",
      category: "Short Notes PDF",
      link: "https://drive.google.com/file/123",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // QuizQuestion
  await assertSucceeds(
    userRef.collection("quizzes").doc("q_1").set({
      userId: ALICE_UID,
      subject: "Biology",
      chapter: "Cell Cycle",
      q: "Crossing over occurs in?",
      opts: ["Leptotene", "Pachytene", "Diplotene", "Diakinesis"],
      ans: 1,
      mark: "Imp",
      exp: "Recombination nodules appear in Pachytene.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // QuizAttempt
  await assertSucceeds(
    userRef.collection("quizAttempts").doc("qa_1").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      subject: "Biology",
      chapter: "Cell Cycle",
      score: 9,
      total: 10,
      accuracy: 90,
      elapsedSec: 240,
      avgSec: 24,
      wrongItemsJson: "[]",
      marksSummaryJson: "[]",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Security bounds reject ghost fields, @DocumentId id, future timestamps, and invalid values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastDate();

  // Reject `@DocumentId` `id` inside payload
  await assertFails(
    userRef.collection("tasks").doc("t_bad_id").set({
      id: "t_bad_id",
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Bad ID",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject ghost field
  await assertFails(
    userRef.collection("tasks").doc("t_ghost").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Ghost field",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    userRef.collection("tasks").doc("t_future").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Future",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: ts,
    })
  );

  // Reject MockLab > 45 questions
  await assertFails(
    userRef.collection("mockLabs").doc("ml_bad").set({
      userId: ALICE_UID,
      name: "Over 45",
      date: "2026-11-20",
      physicsC: 40,
      physicsW: 10,
      chemistryC: 30,
      chemistryW: 5,
      botanyC: 30,
      botanyW: 5,
      zoologyC: 30,
      zoologyW: 5,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject QuizQuestion with out-of-range answer index
  await assertFails(
    userRef.collection("quizzes").doc("q_bad").set({
      userId: ALICE_UID,
      subject: "Biology",
      chapter: "Cell Cycle",
      q: "Q?",
      opts: ["A", "B", "C", "D"],
      ans: 4,
      mark: "Imp",
      exp: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject mutating createdAt on update
  await assertSucceeds(
    userRef.collection("tasks").doc("t_ok").set({
      userId: ALICE_UID,
      date: "2026-11-20",
      text: "Valid",
      subject: "Physics",
      missionType: "Practice",
      done: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    userRef.collection("tasks").doc("t_ok").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
