# ER Service Reliability Exercise

Ski Town Health Clinic is opening a second, smaller after-hours wing. Each
wing triages and treats its own patients, but the two wings share one fixed
pool of treatment bays, and patient identity is hospital-wide: the same
patient ID must mean the same person no matter which wing admitted them.

Since the after-hours wing opened, operations has reported:

- Some patients wait far longer than others of comparable or lesser urgency,
  even when bays are sitting empty.
- Bay counts have occasionally drifted from the number of patients actually
  being treated.
- A patient appears to have been admitted at both wings at once.

Investigate the service and improve patient allocation across both wings.
Treat this as production code: preserve the public API where practical, keep
the change focused, and provide deterministic evidence for the behavior you
implement.

## Design choices you'll need to make and defend

These expectations intentionally leave policy open. State reasonable
assumptions, identify conflicting goals, and be ready to defend the tradeoffs
you choose, including:

- what "urgency matters" and "no one starves" mean precisely enough to test;
- whether one wing may sit on spare bays while the other has patients
  waiting, and if not, how much coordination between wings that requires.
  Implementation is required only if your policy depends on it;
- whether treatment that has already started may be preempted. Document
  your choice and its safety implications; implementation is required only
  if your policy depends on preemption;
- how a single patient's identity is protected across both wings.

Results must be deterministic when patients are otherwise equivalent. The
recorded medical assessment is historical data and must remain intact.

## First ten minutes

Do not use an AI assistant during the first ten minutes. Inspect the repository
and write 3-5 bullets covering:

1. the important invariants you found;
2. ambiguities or assumptions that affect the design;
3. your proposed allocation policy, including how the two wings coordinate,
   and its tradeoffs;
4. how you plan to verify it.

Discuss these bullets with the interviewer before changing code.

## AI-assisted work

After the initial investigation, AI coding tools are allowed. You remain
responsible for every submitted change and should be prepared to explain:

- what you asked an AI tool to do;
- which suggestions you accepted or rejected;
- how you checked generated code;
- which risks or follow-up work remain.

Do not add production dependencies solely to solve the exercise.

## Running the tests

Prerequisites: JDK 11+ and Maven 3.9+.

```bash
mvn test
```

The public tests are examples, not a complete specification. Add focused,
deterministic tests for your chosen policy and the service invariants. Tests
must not use `Thread.sleep`, random data, or the system clock.

## Reproducing the load

```bash
mvn -q compile
java -cp target/classes com.skiclinic.er.Main
```

This runs both wings under sustained concurrent load against the shared bay
pool and prints a summary. Numbers that don't reconcile with each other are
the discrepancy operations has been reporting.

## Submission

Submit the code, tests, and your initial design bullets, updated if your
understanding changed during implementation.
