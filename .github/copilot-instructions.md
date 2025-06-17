# Junie and Copilot Instructions, Project Guidelines, Coding Style & Conventions

## Purpose

Provide concise, enforceable guidelines so that GitHub Copilot (and humans!) generate code that is consistent, maintainable, and production‑ready for this Spring Boot project.

---

## 1  High‑Level Principles

1. **Readability over brevity** – prefer clear intent to clever tricks.
2. **Fail fast** – validate arguments early and throw meaningful exceptions.
3. **Composition over conditionals** – extract helper methods or classes instead of nesting complex `if/else` blocks.
4. **Immutable where sensible** – default to `final` fields and unmodifiable collections.
5. **Predictable side effects** – pure functions when possible; keep IO at the edges.

---

## 2  Library & API Usage

| Guideline               | Preferred                                                | Avoid                                          |
| ----------------------- | -------------------------------------------------------- | ---------------------------------------------- |
| Utility helpers         | **Google Guava**, **Apache Commons Lang/IO/Collections** | Re‑inventing common utilities                  |

---

## 3  Code Structure & Conditional Logic

* Break large methods into **small, single‑responsibility** helpers.
* Replace deeply nested `if` blocks with explanatory boolean methods.

```java
if (shouldRetry(response)) {
    retryRequest(request);
}

private boolean shouldRetry(HttpResponse<?> response) {
    return response.statusCode() >= 500 && response.statusCode() < 600;
}
```

---

## 4  Dependency Injection

* **Constructor injection first** – every required dependency must appear as a constructor parameter.
* **No field injection** (`@Autowired` on fields) and avoid optional injections; instead supply stubs/mocks in tests.

```java
@Service
public class InvoiceService {
    private final InvoiceRepository repo;
    private final PaymentGateway gateway;

    public InvoiceService(InvoiceRepository repo, PaymentGateway gateway) {
        this.repo = Objects.requireNonNull(repo);
        this.gateway = Objects.requireNonNull(gateway);
    }
}
```

---

## 5  Nullability via JSpecify

Annotate public APIs so that null‑safety can be checked by tools.

```java
import org.jspecify.annotations.*;

public interface UserService {
    List<@NonNull User> listUsers(@Nullable String filter);
}
```

* **@NonNull** – default for return values and parameters unless explicitly `@Nullable`.
* **@Nullable** – document optional values; avoid where possible.

---

## 6  Logging

* Use **SLF4J** with parameterized messages.
* **`log.trace`** at method entry/exit or where fine‑grained debugging helps.
* **`log.debug`** for high‑level flow; **`log.info`** for business events; **`log.warn` / `log.error`** for anomalies.

```java
log.trace("Processing purchase order {}", orderId);
```

---

## 7  Testing with AssertJ

* Use **AssertJ** for all test assertions – it provides a fluent, readable API.
* Prefer chained assertions over multiple separate assertion statements.
* Include descriptive messages using `.as()` to clarify test failures.

### Basic Assertions

```java
// String assertions
assertThat(result).isEqualTo("expected");
assertThat(result).contains("partial");
assertThat(result).startsWith("prefix").endsWith("suffix");

// Numeric assertions
assertThat(value).isPositive().isLessThan(100);
assertThat(value).isBetween(1, 10);

// Collection assertions
assertThat(list).hasSize(3).contains("element");
assertThat(list).containsExactly("a", "b", "c");
assertThat(map).containsEntry("key", "value");

// Boolean assertions
assertThat(condition).isTrue();
assertThat(condition).as("Should be disabled when in maintenance mode").isFalse();

// Exception assertions
assertThatThrownBy(() -> methodThatThrows())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("invalid input");
```

### JUnit vs AssertJ Comparison

```java
// JUnit style (avoid)
assertEquals("expected", actual);
assertTrue(list.contains("element"));
assertFalse(condition, "Should be disabled");

// AssertJ style (preferred)
assertThat(actual).isEqualTo("expected");
assertThat(list).contains("element");
assertThat(condition).as("Should be disabled").isFalse();
```

### Best Practices

* Import statically: `import static org.assertj.core.api.Assertions.*;`
* Use `.as()` to provide context for failures: `assertThat(user.isActive()).as("User %s should be active", user.getId()).isTrue();`
* For complex objects, use extracting: `assertThat(users).extracting("name").contains("Alice", "Bob");`
* Chain assertions for related checks: `assertThat(response).isNotNull().extracting(Response::getStatus).isEqualTo(200);`

---

## 8  Pull‑Request Checklist

* [ ] Constructor‑based injection only (no field `@Autowired`).
* [ ] Utility code leverages Guava / Apache Commons where applicable.
* [ ] Public interfaces carry JSpecify nullability annotations.
* [ ] Adequate `log.trace` statements for debugging.
* [ ] Unit and integration tests cover new code paths.
* [ ] Tests use AssertJ for assertions.

---

## 9  Formatting & Tooling

* **Google Java Format** (v1.18.1) enforced via Spotless.
* **Checkstyle** ruleset: Google + custom overrides (see `/config/checkstyle`).
* **Sonar Lint** issues must be addressed or explicitly justified.

---

## 10  Examples in Practice

### Utility Helper with Guava

```java
Iterable<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().split(payload);
```

### Apache Commons IO Convenience

```java
String fileHash = DigestUtils.sha256Hex(Files.newInputStream(path));
```

---

## 11  References

* Google Guava – [https://github.com/google/guava](https://github.com/google/guava)
* Apache Commons – [https://commons.apache.org/](https://commons.apache.org/)
* JSpecify – [https://jspecify.org/](https://jspecify.org/)
* AssertJ – [https://assertj.github.io/doc/](https://assertj.github.io/doc/)

> **Remember**: clarity, safety, and traceability are the North Stars of this codebase.
