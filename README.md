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

## System layout

```text
+============================================================================+
|                               SKI TOWN ER                                   |
|                                                                            |
|  +-----------------------------+      +-----------------------------+       |
|  |       MAIN WING LOBBY       |      |   AFTER-HOURS WING LOBBY   |       |
|  |                             |      |                             |       |
|  |  Local waiting patients     |      |  Local waiting patients     |       |
|  +--------------+--------------+      +--------------+--------------+       |
|                 \                                  /                         |
|                  \                                /                          |
|                   v                              v                           |
|              +------------------------------------------------+             |
|              |          SHARED TREATMENT BAY POOL             |             |
|              |              [ ] [ ] [ ] [ ] [ ]               |             |
|              +------------------------------------------------+             |
+============================================================================+
```

Each wing owns its lobby and patient list. Both wings draw from the same five
treatment bays. A decision made by one wing can therefore affect whether the
other wing can begin treatment.

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
pool and prints the ER diagram populated with live counts. `[X]` means a bay is
reported occupied and `[_]` means it is reported available. The final status
compares the bay count with actual patient states and calls out values that do
not reconcile.

## Submission

Submit the code, tests, and your initial design bullets, updated if your
understanding changed during implementation.
