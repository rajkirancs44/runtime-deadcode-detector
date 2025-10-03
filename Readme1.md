# 10–15 min Demo Flow (high-impact)

1) **Open with a promise (30s)**  
   “Today I’ll show how I use Copilot Agent mode to: (a) fix code smells fast, (b) raise coverage to ≥80% without breaking builds, (c) optimize DB queries, (d) upgrade deps safely, and (e) ship Angular perf wins — all reproducibly.”

2) **Part A — SonarQube autofix (2–3 min)**
- Show a class with a handful of Sonar issues (null checks, resource leaks, logging, equals/hashCode, magic numbers).
- Run **Prompt A1** (below).
- Re-scan locally (SonarLint or `sonar-scanner`) and show reduced issues.

3) **Part B — Generate tests to ≥80% coverage (3–4 min)**
- Pick 1–2 packages with services/controllers.
- Run **Prompt B1** to generate tests in batches; run tests; open JaCoCo report; briefly show branch lines toggled.

4) **Part C — Performance uplift (3–4 min)**
- **Java service hot path:** micro-optimizations + algorithmic fix + Micrometer timers.
- **Mongo query:** aggregation/index advice; measure with `explain()`.
- **Oracle query:** show plan + rewrite + bind variables.

5) **Part D — Safe dependency upgrades (1–2 min)**
- Run **Prompt D1** to upgrade Gradle/Maven + libs with changelog checks and smoke tests.

6) **Part E — Angular perf wins (1–2 min)**
- Run **Prompt E1** to add `OnPush`, `trackBy`, and lazy routes.
- Show `ng build --configuration production` bundle diff or perf hints.

7) **Close with metrics (30s)**
- Before/after: Sonar issue count ↓, coverage ↑, p95 latency ↓, library CVEs ↓.

---

# Setup Checklist (do beforehand)

- Project builds green.
- JaCoCo configured (Maven or Gradle).
- SonarLint in IDE (optional) or access to a SonarQube project.
- Mongo shell access with a demo collection, and Oracle access to a demo schema (or a recorded plan screenshot).
- Angular app with 1 list view + 1 detail route.

---

# Copy-Paste Prompts (Agentic Mode)

## A) SonarQube “Easy Wins” Autofix

**Prompt A1 — Sonar fixes (safe batch)**
```
Scan current module for common Sonar issues (null checks, logging level misuse, resource leaks, magic numbers, equals/hashCode/compareTo contracts, unused private methods, non-final fields in constants classes). 
For up to 30 files:
- Apply minimal safe refactors that don’t change external behavior.
- Add unit tests when a fix changes logic.
- Keep commits small: 10 files max per commit with messages like “chore(sonar): fix resource leaks in io package”.
- After each commit: run mvn/gradle test and show a short summary of fixed rules by key.
Do not introduce new dependencies.
```

**Prompt A2 — Logging & exceptions clean-up**
```
Normalize logging and exception handling:
- Replace printStackTrace with proper logger.
- Use parameterized logging with placeholders.
- Convert broad catch blocks to specific exceptions where possible.
- Preserve messages and add context IDs to logs.
Run tests after changes. No functional behavior changes.
```

---

## B) Coverage ≥80% with Passing Tests

**Prompt B1 — Test generation with batching**
```
Generate JUnit 5 + Mockito tests for all classes under src/main/java with target ≥80% line AND branch coverage (JaCoCo).
Rules:
- Mirror packages, name <ClassName>Test.
- Cover happy paths, edge cases, exceptions, and nulls.
- Mock external IO (DB, HTTP, clock, randomness with seeds).
- Process in batches of ~30 classes. After each batch: run “mvn test” or “./gradlew test”; fix failing tests; run JaCoCo report; add tests until ≥80% on touched code.
Stop when global coverage ≥80% with all tests green.
```

**Prompt B2 — Fill branch gaps**
```
Open the latest JaCoCo HTML report. Identify the 10 lowest-covered classes with uncovered branches. 
For each: add targeted tests to execute missing decision paths (if/else, switch, try/catch, ternaries). 
Re-run tests and update the report until all listed classes exceed 80% branch coverage. No flaky tests.
```

---

## C) Performance (Java, MongoDB, Oracle)

**Prompt C1 — Java hot-path optimization**
```
Profile the service method X (class Y). Add Micrometer timers and log elapsed time at DEBUG. 
Refactor for performance only if it keeps behavior identical:
- Prefer immutable precomputed lookups over repeated stream pipelines.
- Replace O(n^2) loops with a map-based O(n) approach where possible.
- Avoid unnecessary object creation in tight loops.
- For concurrency, use bounded thread pools and avoid blocking calls in reactive flows.
Add microbench-style unit tests validating results and performance assertions (upper-bound timing using a generous threshold). Run tests.
```

**Prompt C2 — Mongo aggregation + index tuning**
```
Given this Mongo aggregation pipeline (paste below), analyze with explain() and propose a better index or pipeline rewrite to reduce COLLSCAN and stage time. 
Output:
1) Proposed compound index(es) with field order rationale using cardinality/selectivity.
2) Revised pipeline steps with $match/$project early and $sort/$limit later.
3) A small dataset generator for tests.
4) Instructions to validate with explain() and timing before/after.
Do not change business semantics.
```

**Prompt C3 — Oracle SQL rewrite with plan check**
```
For the given Oracle SQL (paste below), produce:
1) A version using bind variables and SARGable predicates (avoid functions on indexed columns).
2) Alternative join order or hints only if necessary.
3) A step-by-step plan comparison (OLD vs NEW) and expected reduction in logical reads.
4) A minimal test harness (JUnit) that runs the SQL via a DataSource with a mock or test container; assert same results.
No schema changes unless an index addition is recommended; if so, justify the composite index column order.
```

---

## D) Safe Dependency Upgrades

**Prompt D1 — Upgrade plan & execution**
```
Detect build tool and list outdated dependencies and plugins. 
Plan:
- Group upgrades by risk (patch, minor, major).
- For each group: review CHANGELOG/RELEASE NOTES and known breaking changes.
- Propose a batch upgrade order starting with low-risk.
Execute:
- Apply patch/minor upgrades first.
- Run tests and publish a summary of results.
- For major upgrades: create a short migration checklist and update code/config accordingly.
- Ensure JaCoCo, JUnit 5, Mockito and Spring Boot (if present) remain compatible.
- Commit in small batches with detailed messages.
```

**Prompt D2 — Security & license check**
```
Run OWASP dependency check or equivalent. Identify CVEs with fix versions. 
Propose minimal upgrades to remediate critical/high issues, and generate a markdown report summarizing CVEs fixed, versions moved, and any transitive exclusions added, with links to advisories.
```

---

## E) Angular Performance & DX

**Prompt E1 — Angular perf sweep**
```
For the Angular app:
- Convert list-heavy components to ChangeDetectionStrategy.OnPush.
- Add trackBy functions to *ngFor.
- Split routes into lazy-loaded feature modules; ensure shared components are not eagerly bundled.
- Replace RxJS nested subscriptions with pipe + switchMap/mergeMap and async pipe in templates.
- Audit third-party UI libs; remove unused modules; enable build optimizer and budgets.
- Create a before/after bundle size and LCP comparison note.
No behavior changes.
```

**Prompt E2 — Unit tests and harness**
```
Add Jest or keep Karma/Jasmine (project default) and generate unit tests for components and services, focusing on:
- Input/output bindings
- Async observables via TestScheduler or marble tests
- trackBy correctness
- Route guards and resolvers
Run tests and ensure coverage ≥80% for affected components.
```

---

# Live “Wow” Examples You Can Paste

### 1) Java micro-optimization snippet (before/after)
**Before**
```java
public List<Foo> findActive(List<Foo> foos) {
  return foos.stream()
      .filter(f -> f.getStatus() != null && f.getStatus().equals("ACTIVE"))
      .sorted(Comparator.comparing(Foo::getUpdatedAt))
      .collect(Collectors.toList());
}
```
**After (clearer + faster, less GC)**
```java
public List<Foo> findActive(List<Foo> foos) {
  List<Foo> out = new ArrayList<>();
  for (Foo f : foos) {
    if ("ACTIVE".equals(f.getStatus())) out.add(f);
  }
  out.sort(Comparator.comparing(Foo::getUpdatedAt));
  return out;
}
```
**Narrate:** simple changes remove null checks via constant-equals, cut lambda churn, and keep semantics.

### 2) Mongo index recommendation (talk track)
- “The query filters on `{ tenantId, status }` and sorts by `updatedAt desc`. A compound index `{ tenantId: 1, status: 1, updatedAt: -1 }` covers match + sort; we move `$match` first and `$project` early; `$sort` happens on index; `$limit` last.”

### 3) Oracle SARGability example
**Anti-pattern**
```sql
WHERE TRUNC(order_date) = :d
```
**Fix**
```sql
WHERE order_date >= :d AND order_date < :d + 1
```
**Narrate:** preserves index on `order_date` and avoids function on column.

### 4) Angular `trackBy`
```ts
trackById(_: number, item: Product) { return item.id; }
```
**Narrate:** DOM reuse cuts re-render cost on large lists.

---

# “Narration Notes” (what to say while it runs)

- *Autonomy & safety:* “I run in batches; after each batch I compile, test, and check coverage to avoid breaking main.”
- *Quality gates:* “I enforce ≥80% line & branch coverage (not just line) and target the lowest covered classes first.”
- *Perf mindset:* “We instrument before optimizing; if p95 improves but correctness holds, we keep the change; otherwise revert.”
- *Reproducibility:* “Every change is a small commit with a human-readable message and a markdown report.”
- *DB tuning principle:* “Index for your predicate and your sort; push `$match` early, defer `$sort/$limit`; avoid functions on indexed columns.”
- *Angular perf:* “OnPush + trackBy + lazy routes — the holy trinity for list-heavy dashboards.”

---

# Quick Commands You Can Run On-Stage

- **Maven:** `mvn -q test` → `mvn -q -Djacoco.skip=false test jacoco:report`
- **Gradle:** `./gradlew test jacocoTestReport`
- **Sonar (if set):** `sonar-scanner`
- **Angular:** `ng build --configuration production`
- **Mongo explain:** in shell `db.orders.aggregate(pipeline).explain("executionStats")`
- **Oracle plan:** `SELECT * FROM table(DBMS_XPLAN.DISPLAY_CURSOR(NULL,NULL,'BASIC +PREDICATE +NOTE'));`

---

# Slide/Doc One-Pager (optional handout)

- **Problem:** Code smells, low coverage, slow endpoints, aging deps.
- **Approach:** Copilot Agentic Mode → batch changes → test → measure → commit → repeat.
- **Outcomes:**
    - Sonar: issue count ↓ 70% in 10 min
    - Coverage: 52% → 82%
    - p95 latency: 480ms → 220ms on hot path
    - CVEs: 3 critical fixed via minor upgrades
    - Angular LCP: 3.8s → 2.4s (OnPush + lazy routes)
- **Principles:** small steps, measurable impact, no regressions.

---

# Backup Prompts (if something stalls)

**“Recover & resume”**
```
Resume from last successful batch. List remaining files. Start the next batch of 20 files, generate tests, and run the build. Report only failures and uncovered branches.
```

**“Rollback a noisy change”**
```
Revert the last commit that modified production code in package X; keep only the test additions. Re-run tests and coverage.
```
