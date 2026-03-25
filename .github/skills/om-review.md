---
name: om-review
description: Review current code changes or PR for bugs, style issues, and potential problems specific to Organic Maps
argument-hint: "[pr-number|staged|all|branch]"
allowed-tools: [ Bash, Glob, Grep, Read, Task ]
---

# Code Review for Organic Maps

Review code changes for bugs, style issues, and Organic Maps-specific problems.

## Arguments

- No argument: review unstaged changes (`git diff`)
- `staged`: review staged changes (`git diff --cached`)
- `all`: review all uncommitted changes
- `branch`: review all commits in current branch vs master (`git diff origin/master...HEAD`)
- PR number (e.g., `123`): review PR changes via `gh pr diff`
- `pr`: review current branch's PR

### Flags

- `--post` or `post`: Post review comments directly to GitHub PR (requires PR number or `pr`)

### Examples

```
/om-review                  # Review unstaged changes
/om-review staged           # Review staged changes
/om-review branch           # Review current branch vs master
/om-review branch --post    # Review branch and post to PR (if PR exists)
/om-review 12427            # Review PR #12427, output to console
/om-review 12427 --post     # Review PR #12427 and post comments to GitHub
/om-review pr --post        # Review current branch's PR and post comments
```

## Execution Steps

### Step 1: Determine Scope

Based on the argument provided by the user (ARGUMENTS variable):

1. **No argument or empty**: Get diff with `git diff`
2. **`staged`**: Get diff with `git diff --cached`
3. **`all`**: Get diff with `git diff HEAD`
4. **`branch`**: Get diff with `git diff origin/master...HEAD`
5. **Number (PR)**: Get diff with `gh pr diff <number>`
6. **`pr`**: Get diff with `gh pr diff`

If the diff is empty, inform the user there are no changes to review.

### Step 2: Identify Changed Files

Parse the diff to identify:

- Which files were modified
- File types (.cpp, .hpp, .java, .swift, .mm, .m)
- Whether JNI bridge files are involved (both .cpp and .java changes)

### Step 3: Launch Review Agents

Launch appropriate review agents in parallel based on changed files:

#### Agent 1: Code Quality Review (always run)

Check for:

- Logic errors and potential bugs
- Null pointer dereferences
- Memory leaks (especially in C++)
- Resource cleanup issues
- Thread safety problems
- Exception handling gaps

#### Agent 2: Style Checker (always run)

Verify Organic Maps code style:

**C++ style:**

- `const` after type: `auto const & ref = f();`
- `m_` prefix for member variables
- `#pragma once` in headers
- `.hpp/.cpp` extensions (not `.h/.cc`)
- `constexpr` and `kCamelCase` for constants
- `using` instead of `typedef`
- 2-space indent, 120 char line width
- Namespaces in `lower_case` with underscores

**Java style:**

- Java 17 compatibility
- Proper `@NonNull`/`@Nullable` annotations
- 2-space indent, 120 char line width

#### Agent 3: JNI Reviewer (if .cpp AND .java files changed)

Check JNI-specific issues:

- Exception checking after JNI calls (`ExceptionCheck()`)
- Local reference management (`DeleteLocalRef`)
- Thread safety (JNI calls from correct thread)
- String encoding (UTF-8 with `GetStringUTFChars`)
- Method signature correctness
- Native method declarations match implementations

#### Agent 4: Android Reviewer (if Android code changed)

Check Android-specific issues:

- Context leaks (storing Activity context)
- Memory leaks (unregistered listeners, callbacks)
- Main thread blocking (I/O, network on main thread)
- Proper permission handling
- Lifecycle awareness
- Resource cleanup in `onDestroy`/`onStop`

#### Agent 5: iOS Reviewer (if iOS code changed: .swift, .m, .mm in iphone/)

Check iOS-specific issues:

**Swift:**

- Optional unwrapping safety (avoid force unwrap)
- `weak`/`unowned` in closures to prevent retain cycles
- `@MainActor` / `DispatchQueue.main.async` for UI operations
- swiftformat compliance (2-space indent)
- `@objc` attributes for Objective-C interop
- Proper use of `NS_SWIFT_NAME` mappings

**Objective-C/C++:**

- ARC compliance (no manual retain/release)
- `NSHashTable` for weak observer references
- `dispatch_async(dispatch_get_main_queue(), ...)` for UI updates
- Nullability annotations (`NS_ASSUME_NONNULL_BEGIN/END`)
- `#pragma mark -` for code sections
- Singleton pattern with `dispatch_once_t`
- C++ bridging: proper lambdas with `GetFramework()`

**CoreApi Bridge:**

- `NS_SWIFT_NAME` for Swift-friendly API
- Framework callbacks dispatched to main queue
- Proper type definitions in MWMTypes.h

#### Agent 6: Security Reviewer (always run)

Check for security vulnerabilities:

**Secrets and credentials:**

- Hardcoded API keys, tokens, passwords
- Private keys or certificates in code
- Suspicious base64-encoded strings
- URLs with embedded credentials

**Injection vulnerabilities:**

- SQL injection (raw string concatenation in queries)
- Command injection (shell commands with user input)
- Path traversal (unsanitized file paths)
- XSS in WebView content

**Privacy (Organic Maps specific):**

- No analytics or tracking code
- No unauthorized network requests
- No PII logging
- Location data handling

**Cryptography:**

- Weak hash algorithms (MD5, SHA1 for security)
- Hardcoded IVs or salts
- Insecure random number generation

#### Agent 7: Regression Reviewer (always run)

Compare changes against main branch to detect potential regressions:

**How to check:**

1. Get the base branch: `git merge-base HEAD origin/master`
2. Compare function signatures, return types, public APIs
3. Check for removed functionality without deprecation
4. Verify error handling is not weakened

**Flag as regression if:**

- Public API changed without backwards compatibility
- Error handling removed or weakened
- Performance-critical code made slower (added allocations, loops)
- Feature flags or conditions removed
- Default values changed

**Mark with severity:**

- 🟣 Pre-existing: Issue existed before this PR
- 🔴 Regression: Change introduces a problem that was not there before

#### Agent 8: Test Coverage Reviewer (always run)

Check that new/changed code has appropriate test coverage:

**For C++ code:**

1. Find test files: look for `*_tests.cpp` in same directory or `*_tests/` subdirectory
2. Check if new functions/classes have corresponding tests
3. Look for test patterns: `UNIT_TEST`, `TEST`, `TEST_F`

**For Java code:**

1. Find test files in `src/test/java/` or `src/androidTest/java/`
2. Map source file to test file: `Foo.java` -> `FooTest.java`
3. Check for JUnit annotations: `@Test`, `@Before`, `@After`

**For Swift code:**

1. Find test files in `*Tests/` directories
2. Check for `XCTestCase` subclasses
3. Look for `func test*()` methods

**Flag as issue if:**

- New public function/method has no tests
- Changed logic in existing function but tests not updated
- Test file exists but new code paths not covered
- Critical code (routing, payments, location) lacks tests

**Report format:**

- List untested new functions
- Suggest specific test cases needed
- Note if existing tests may need updates

### Step 4: Generate Report

Compile findings into a structured report with severity tags:

**Severity levels:**

- 🔴 **Critical** — Must fix before merge (security, crashes, data loss)
- 🟠 **Important** — Should fix (bugs, regressions, significant issues)
- 🟡 **Nit** — Minor improvements (style, readability, minor optimizations)
- 🟣 **Pre-existing** — Issue existed before this PR (optional to fix)

```markdown
## Review Summary

**Scope:** [description of what was reviewed]
**Files reviewed:** [count]
**Security scan:** [passed/issues found]
**Test coverage:** [covered/needs tests]

### 🔴 Critical Issues

- [file:line] Description of critical issue

### 🟠 Important Issues

- [file:line] Description of important issue

### 🟡 Nits

- [file:line] Description of minor issue

### 🟣 Pre-existing Issues

- [file:line] Issue that existed before this PR

### 🧪 Test Coverage

- [status] function_name in file.cpp — [has tests / needs tests]
- Suggested test cases: [list]

### ✅ Passed Checks

- Security scan
- Code style
- Memory safety
- Thread safety
- Test coverage
```

### Step 5: Post to GitHub (if --post flag)

If the user specified `--post` flag and reviewing a PR:

1. **Format comments for GitHub:**
    - Convert markdown report to GitHub review format
    - Group issues by file for inline comments

2. **Post review using gh CLI:**

   For overall review comment:
   ```bash
   gh pr review <PR_NUMBER> --comment --body "<review summary>"
   ```

   For requesting changes (if critical issues found):
   ```bash
   gh pr review <PR_NUMBER> --request-changes --body "<review summary>"
   ```

   For approval (if no critical/important issues):
   ```bash
   gh pr review <PR_NUMBER> --approve --body "<review summary>"
   ```

3. **Post inline comments for specific issues:**
   ```bash
   gh pr comment <PR_NUMBER> --body "**file:line** - description"
   ```

4. **Inform user of posted review:**
    - Show link to PR
    - Summarize what was posted

**Note:** If `--post` flag is not specified, only output the review to console.

## Organic Maps Specific Guidelines

When reviewing, consider:

1. **Cross-platform impact**: Changes may affect iOS, Android, and Desktop
2. **Offline-first**: App must work without network
3. **Performance**: Maps are performance-critical
4. **Privacy**: No tracking, no analytics leaks
5. **Memory**: Mobile devices have limited memory

## File Type Detection

| Extension      | Platform | Review Focus                           |
|----------------|----------|----------------------------------------|
| `.cpp`, `.hpp` | Core     | Memory, performance, const correctness |
| `.java`        | Android  | Lifecycle, null safety, threading      |
| `.swift`       | iOS      | Optionals, memory management           |
| `.mm`, `.m`    | iOS      | ARC, bridging                          |
| `.gradle`      | Android  | Dependencies, versions                 |
| `.xml`         | Android  | Resources, layouts                     |

## Key Files Reference

### JNI Bridge

- `android/sdk/src/main/java/app/organicmaps/sdk/Framework.java` - JNI Java side
- `android/sdk/src/main/cpp/app/organicmaps/sdk/` - JNI C++ implementations

### iOS Core

| Component        | Path                                                 | Focus                     |
|------------------|------------------------------------------------------|---------------------------|
| App Delegate     | `iphone/Maps/Classes/MapsAppDelegate.mm`             | Framework init, lifecycle |
| Map Controller   | `iphone/Maps/Classes/MapViewController.mm`           | Gestures, rendering       |
| Framework Bridge | `iphone/Maps/Core/Framework/MWMFrameworkListener.mm` | Observer dispatch         |
| Location         | `iphone/Maps/Core/Location/MWMLocationManager.mm`    | Singleton, threading      |
| CoreApi Types    | `iphone/CoreApi/CoreApi/Common/MWMTypes.h`           | Type definitions          |
| Swift Bridging   | `iphone/Maps/Bridging-Header.h`                      | ObjC→Swift imports        |
| Theme            | `iphone/Maps/Core/Theme/Core/ThemeManager.swift`     | SwiftUI patterns          |

### Android Core

- `android/app/src/main/java/app/organicmaps/MwmApplication.java` - App entry
- `android/app/src/main/java/app/organicmaps/MwmActivity.java` - Main activity

## Example Output

```markdown
## Review Summary

**Scope:** Staged changes
**Files reviewed:** 3 (2 C++, 1 Java)
**Security scan:** Passed
**Test coverage:** Needs attention

### 🔴 Critical Issues

- android/sdk/src/main/java/app/organicmaps/sdk/Framework.java:142
  Potential null pointer: `result` not checked before use

### 🟠 Important Issues

- map/routing_manager.cpp:89
  Missing `const` qualifier: should be `auto const & route`

### 🟡 Nits

- map/routing_manager.cpp:95
  Consider using `std::move` for the string parameter

### 🟣 Pre-existing Issues

- map/routing_manager.cpp:42
  Variable `m_data` shadows outer scope (existed before this PR)

### 🧪 Test Coverage

- ⚠️ `CalculateRoute()` in routing_manager.cpp — needs tests
- ✅ `ParseResponse()` in routing_manager.cpp — has tests in routing_manager_tests.cpp
- Suggested test cases:
    - Test CalculateRoute with empty waypoints
    - Test CalculateRoute with invalid coordinates

### ✅ Passed Checks

- Security scan (no secrets, no injection vulnerabilities)
- Code style (clang-format)
- No memory leaks detected
- JNI exception handling present
- Thread safety verified
- No regressions detected
```
