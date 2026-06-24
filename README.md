# Java Code Review Practice

A set of 9 Java code-review exercises at three difficulty levels, built for senior-engineer interview preparation.

Each snippet contains intentional bugs modelling real production issues. The matching `_Answer` file contains the full review notes (What / Why / Fix / Comment template) and the corrected code.

---

## Structure

```
easy/
  Easy01_StringAndResources            — String ==, unclosed resource, O(n²) loop
  Easy02_NullAndMagicNumbers           — NPE chain, null return, magic numbers
  Easy03_ExceptionAndDuplication       — Swallowed exception, catch-all, DRY violation

medium/
  Medium01_EqualsHashCodeAndImmutability — equals/hashCode contract, exposed internal state
  Medium02_PolymorphismAndSRP            — instanceof chains, SRP violation, magic strings
  Medium03_ConcurrencyAndMemoryLeak      — Race condition, HashMap, ThreadLocal, TOCTOU

hard/
  Hard01_SecurityAndLogging              — SQL injection (×2), passwords in logs, session token leak
  Hard02_DeepConcurrencyAndDesign        — Unsynchronized rate-limiter/circuit-breaker, retry design
  Hard03_MemoryLeakAndPolymorphism       — Static collection leak, listener leak, NPE, polymorphism
```

---

## How to Practice

1. **Open the snippet file** (e.g. `Easy01_StringAndResources.java`).
2. **Review it** as if it were a real PR — write down every issue, the risk, and the fix.
3. **Use the comment format**: *What → Why → Suggestion → open question*.
4. **Check your answer** in the `_Answer` file. Compare severity ratings and fixes.
5. **Time yourself**: easy ~5 min, medium ~10 min, hard ~15–20 min.

---

## Topics Covered

| Topic | Files |
|---|---|
| String comparison (`==` vs `.equals()`) | Easy01 |
| Unclosed resources / try-with-resources | Easy01, Easy03 |
| Null pointer exceptions & Optional | Easy02, Medium01 |
| Magic numbers & named constants | Easy02 |
| Swallowed exceptions / catch-all | Easy03, Medium03 |
| Code duplication (DRY) | Easy03 |
| `equals()` / `hashCode()` contract | Medium01 |
| Immutability / defensive copies | Medium01, Hard03 |
| Polymorphism over conditionals | Medium02, Hard03 |
| Single Responsibility Principle | Medium02 |
| Race conditions / atomic operations | Medium03, Hard02 |
| Thread-safe collections | Medium03, Hard03 |
| ThreadLocal memory & security leak | Medium03 |
| TOCTOU race in Map operations | Medium03 |
| SQL injection | Hard01 |
| Sensitive data in logs | Hard01 |
| Password hashing | Hard01 |
| Unsynchronised rate-limiter / circuit-breaker | Hard02 |
| Retry non-retryable exceptions | Hard02 |
| Static collection memory leak | Hard03 |
| Listener registration leak | Hard03 |

---

## Code Review Comment Formula

> **"This [X] does [Y], which means [risk Z]. Suggest [fix]. Happy to discuss if there's a reason for the current approach."**

- **What** — the specific issue and line
- **Why** — the production risk or correctness impact
- **Suggestion** — the concrete fix
- **Open question** — collaborative, not gatekeeping

---

*Built as part of interview preparation for senior Java engineer roles.*
