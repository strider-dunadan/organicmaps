---
name: om-review
description: Review current code changes or PR for architecture, design, simplicity, bugs, style issues, and potential problems specific to Organic Maps
argument-hint: "[pr-number|staged|all|branch]"
allowed-tools: [ Bash, Glob, Grep, Read ]
---

# Code Review for Organic Maps

Review code changes for architecture, design, simplicity, bugs, style issues, and Organic Maps-specific problems.

## Arguments

Parse the review target from the ARGUMENTS variable if set, otherwise from the user's message
after the command. Parse `--post` or `post` as the posting flag from any position.

- No argument: review unstaged changes (`git diff`)
- `staged`: review staged changes (`git diff --cached`)
- `all`: review all uncommitted changes (`git diff HEAD`)
- `branch`: review all commits in current branch vs master (`git diff origin/master...HEAD`)
- PR number (e.g., `123`): review PR changes via `gh pr diff`
- `pr`: review current branch's PR

### Flags

- `--post` or `post`: Post review comments directly to GitHub PR (requires PR number or `pr`)

### Examples

```
/om-review                  # Review unstaged changes
/om-review staged           # Review staged changes
/om-review branch           # Review current branch vs master (should be rebased on master)
/om-review branch --post    # Review branch and post to PR (if PR exists)
/om-review 12427            # Review PR #12427, output to console
/om-review 12427 --post     # Review PR #12427 and post comments to GitHub
/om-review pr --post        # Review current branch's PR and post comments
```

## Execution Steps

### Step 1: Determine Scope

Based on the parsed argument:

1. **No argument or empty**: Get diff with `git diff --no-color`
2. **`staged`**: Get diff with `git diff --no-color --cached`
3. **`all`**: Get diff with `git diff --no-color HEAD`
4. **`branch`**: Get diff with `git diff --no-color origin/master...HEAD`
5. **Number (PR)**: Get diff with `gh pr diff <number>`
6. **`pr`**: Get diff with `gh pr diff`

If the diff is empty, inform the user there are no changes to review.

**Resolving `branch --post`:** If `--post` is specified with `branch` mode, run
`gh pr view --json number -q '.number'` to find the PR for the current branch.
If no PR exists, inform the user and skip posting.

### Step 2: Gather Context

#### 2a: Identify Changed Files

Parse the diff to identify:

- Which files were modified, added, or deleted
- File types (.cpp, .hpp, .java, .swift, .mm, .m, .gradle, .xml, .json, .txt, etc.)
- Whether JNI bridge files are involved (both .cpp and .java changes in android folder)

#### 2b: PR Metadata (for `pr` and numeric modes)

Fetch PR metadata to understand the stated scope:

```bash
gh pr view <N> --json title,body,labels,baseRefName,headRefName,commits
```

- Read the PR title and body to understand the stated purpose
- Note linked issues (`Fixes #NNN`) and labels
- Verify the base branch name (do not assume `master`)
- Check whether the implementation matches what the PR description claims

#### 2c: Read Source File Context

For each file with non-trivial changes, use `Read` to load surrounding context:

- Read at least 50 lines above and below each changed hunk
- For files under 500 lines, read the entire file
- For `.hpp` changes, also read the corresponding `.cpp` (and vice versa)
- For JNI `.java` changes, also read the matching native C++ file in
  `android/sdk/src/main/cpp/app/organicmaps/sdk/`

This is essential to avoid false positives. A diff alone cannot show whether a null check
already exists above, whether a variable is initialized earlier, or whether a pattern is
consistent with the rest of the file.

#### 2d: Large Diff Strategy

If the diff exceeds 500 changed lines:

1. Triage files by risk: core C++ and JNI bridge > platform UI > resources/configs
2. Read full context only for high-risk files; review only the diff for low-risk files
3. Focus the report on the highest-severity findings
4. State the scope limitation in the report summary
5. Recommend splitting if the PR mixes unrelated changes (per `docs/PR_GUIDE.md`)

### Step 3: Review Checklists

Work through the following checklists. Skip a checklist section if its trigger condition
is not met.

#### Checklist 1: Correctness & Safety (always)

**Bugs and logic errors:**

- Logic errors and potential bugs
- Null pointer dereferences (check surrounding context for existing guards)
- Memory leaks (especially in C++ — missing `delete`, unreleased resources)
- Resource cleanup issues (file handles, locks, JNI local refs)
- Thread safety problems (data races, missing locks, UI from background thread)
- Exception handling gaps
- Integer overflow / underflow in arithmetic

**Regression detection:**

Look for these concrete patterns in the diff (removed `-` vs added `+` lines):

- Removed `if` / `CHECK` / `ASSERT` guards without replacement
- Changed function return type (e.g., `bool` -> `void` loses error signaling)
- Removed `const` qualifier from parameter or method
- Added heap allocation (`new`, `make_shared`, `make_unique`) inside a loop that didn't have one
- Removed `override` keyword (may silently stop overriding a virtual)
- Changed default parameter values
- Removed `[[nodiscard]]` attribute
- Broadened a catch clause (e.g., specific exception -> `catch (...)`)
- Removed or weakened error handling

If a change looks like it might break callers, use `Grep` to find call sites and verify.

**Security:**

- Hardcoded API keys, tokens, passwords, or certificates
- Suspicious base64-encoded strings or URLs with embedded credentials
- SQL injection (raw string concatenation in queries)
- Command injection (shell commands with user input)
- Path traversal (unsanitized file paths)
- XSS in WebView content
- No analytics or tracking code (Organic Maps is privacy-first)
- No unauthorized network requests or PII logging
- Weak hash algorithms (MD5, SHA1 for security purposes)
- Hardcoded IVs, salts, or insecure random number generation

**Severity tags:**

- 🔴 Critical — security, crashes, data loss, clear regressions
- 🟠 Important — bugs, potential regressions, significant issues
- 🟣 Pre-existing — issue existed before this PR (visible in context, not in diff)

#### Checklist 2: Conventions & Hygiene (always)

**Semantic conventions** (formatters cannot check these):

Indentation, line width, and brace placement are enforced by the pre-commit hook
(`tools/hooks/pre-commit` runs clang-format + swiftformat). Do not comment on formatting
that these tools handle.

Focus on:

**C++:**

- `m_` prefix for member variables
- `kCamelCase` for `constexpr` constants
- `#pragma once` in headers (not include guards)
- `.hpp/.cpp` file extensions (not `.h/.cc`)
- `using` instead of `typedef`
- East const: `Type const &` (not `const Type &`)
- Meaningful, descriptive names

**Java:**

- `@NonNull` / `@Nullable` annotations on public API
- Java 17 compatibility

**PR & Commit Hygiene** (for `pr` and numeric modes):

Check against project requirements using `gh pr view <N> --json commits`:

- Every commit has a `Signed-off-by:` line (DCO requirement — `docs/CONTRIBUTING.md`)
- PR description mentions LLM tools if used (`docs/PR_GUIDE.md`)
- Commit subjects have `[subsystem]` prefix (`docs/COMMIT_MESSAGES.md`)
- Commit subjects <= 72 characters, imperative mood, no trailing period
- PR is appropriately sized (flag if > 1000 lines without justification)
- Auto-generated files (strings, styles) are in separate commits

**Severity tags:**

- 🟡 Nit — naming, style, minor readability improvements
- 🟠 Important — missing DCO, misleading names, convention violations in public API

#### Checklist 3: Platform-Specific (conditional)

##### JNI (if both .cpp AND .java files changed)

- Exception checking after JNI calls (`ExceptionCheck()`)
- Local reference management (`DeleteLocalRef`)
- Thread safety (JNI calls from correct thread)
- String encoding (UTF-8 with `GetStringUTFChars`)
- Method signature correctness
- **Cross-file verification:** For each `native` method in `.java`, use `Grep` to find the
  corresponding `JNIEXPORT` function (pattern: `Java_app_organicmaps_sdk_<ClassName>_<methodName>`)
  in `android/sdk/src/main/cpp/app/organicmaps/sdk/`. Verify parameter types match
  (Java `String` -> C++ `jstring`, Java `long` -> `jlong`, etc.)

##### Android (if `android/` files changed)

- Context leaks (storing Activity context in static or long-lived fields)
- Memory leaks (unregistered listeners, callbacks)
- Main thread blocking (I/O, network on main thread)
- Proper permission handling
- Lifecycle awareness (operations in correct lifecycle state)
- Resource cleanup in `onDestroy` / `onStop`

##### iOS (if `.swift`, `.m`, `.mm` in `iphone/` changed)

**Swift:**

- Optional unwrapping safety (avoid force unwrap `!`)
- `weak` / `unowned` in closures to prevent retain cycles
- `@MainActor` / `DispatchQueue.main.async` for UI operations
- `@objc` attributes for Objective-C interop
- Proper use of `NS_SWIFT_NAME` mappings

**Objective-C/C++:**

- ARC compliance (no manual `retain` / `release`)
- `NSHashTable` for weak observer references
- `dispatch_async(dispatch_get_main_queue(), ...)` for UI updates
- Nullability annotations (`NS_ASSUME_NONNULL_BEGIN/END`)
- C++ bridging: proper lambdas with `GetFramework()`

**CoreApi Bridge:**

- `NS_SWIFT_NAME` for Swift-friendly API
- Framework callbacks dispatched to main queue
- Proper type definitions in `MWMTypes.h`

#### Checklist 4: Test Coverage (always)

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
- Critical code (routing, search, location, storage) lacks tests

**Report format:**

- List untested new functions
- Suggest specific test cases needed
- Note if existing tests may need updates

### Review Tone

- Focus on the code, not the developer: "this function may leak" not "you forgot to free"
- When flagging an issue, suggest a concrete fix or alternative
- Acknowledge good design decisions in the review summary
- For nits, frame as suggestions: "Consider..." or "Minor: ..."
- Reference project docs when suggesting conventions (helps contributors learn)
- When posting to GitHub (`--post`), remember real contributors read these comments

### What NOT to Flag

- Formatting handled by clang-format or swiftformat (indentation, spacing, braces, line width)
- Missing comments on self-explanatory code
- Variable naming that follows existing patterns in the same file
- Import ordering (handled by tooling)
- Suggesting heavy external libraries (project prefers lightweight alternatives or std)
- Suggesting architectural rewrites in a bug-fix PR
- Pre-existing issues outside the diff scope (mention in summary body only, not as inline comments)

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
**Files reviewed:** [count and types]
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
- Naming and semantic conventions
- Memory safety
- Thread safety
- Test coverage
```

### Step 5: Post to GitHub (if --post flag)

If the user specified `--post` flag and reviewing a PR:

> **CRITICAL: Use the Reviews API**
>
> You MUST use `gh api repos/.../pulls/.../reviews` endpoint.
> This posts the review summary AND all inline comments in ONE request.
>
> **Correct:** `gh api repos/{owner}/{repo}/pulls/{pr}/reviews --input -`
> **Wrong:** `gh api repos/.../pulls/.../comments` (outdated, returns 422)
> **Wrong:** `gh pr comment` (creates general comment, not inline)

#### Step 5.1: Get PR metadata

```bash
PR_NUMBER=<number>
HEAD_SHA=$(gh pr view $PR_NUMBER --json headRefOid -q '.headRefOid')
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')
```

#### Step 5.2: Calculate line numbers for inline comments

The `line` field in the GitHub Reviews API refers to the line number in the **new version**
of the file (the `+` side of the diff). To calculate it:

1. Find the relevant `@@ -old_start,old_count +new_start,new_count @@` hunk header
2. The `new_start` value is the line number of the first line in that hunk
3. Count forward from `new_start`, skipping lines that start with `-` (deletions)
4. Lines starting with `+` and lines starting with ` ` (context) both increment the counter
5. The GitHub API rejects comments on lines not present in the diff

If unsure about a line number, place the comment in the review body instead of as an
inline comment.

#### Step 5.3: Build and post review payload

Use `jq` to safely construct the JSON payload (avoids markdown escaping issues in heredocs):

```bash
# Determine the event type
EVENT="COMMENT"  # default: nits, observations, or no issues
# Use REQUEST_CHANGES if critical or important issues found
# Do NOT use APPROVE — AI approvals mislead maintainers who expect human review

# Build the comments array incrementally
COMMENTS_JSON=$(jq -n '[]')
# For each finding with a file and line:
COMMENTS_JSON=$(echo "$COMMENTS_JSON" | jq \
  --arg path "path/to/file.cpp" \
  --argjson line 42 \
  --arg body "🔴 **Critical:** Description of issue" \
  '. + [{path: $path, line: $line, body: $body}]')

# Post the review
jq -n \
  --arg commit_id "$HEAD_SHA" \
  --arg event "$EVENT" \
  --arg body "$REVIEW_BODY" \
  --argjson comments "$COMMENTS_JSON" \
  '{commit_id: $commit_id, event: $event, body: $body, comments: $comments}' \
  | gh api repos/${REPO}/pulls/${PR_NUMBER}/reviews --input -
```

**Severity emoji prefixes for inline comments:**

- 🔴 Critical
- 🟠 Important
- 🟡 Nit
- 🟣 Pre-existing

#### Step 5.4: Review summary format (the "body" field)

The review body should be comprehensive:

```markdown
## Review Summary

**Scope:** [PR title and what it does]
**Files reviewed:** [count and types]
**Security scan:** [Passed / Issues found]
**Test coverage:** [Covered / Needs tests]

### General Observations

[Things that are not tied to specific lines:]

- Architecture/design considerations
- Cross-platform impact notes
- Performance implications
- Suggestions for future improvements

### Issues Not In Diff

[Any issues found in surrounding context — cannot be posted as inline comments:]

- file.cpp: pre-existing issue description

### Checks Passed

- ✅ Security scan (no secrets, no injection vulnerabilities)
- ✅ Naming and semantic conventions
- ✅ Memory safety
- ✅ Thread safety
- ⚠️ Test coverage needs attention

### Verdict

[Summary of what needs to be fixed before merge, or clean review message]
Found X critical and Y important issues. Please address inline comments.
```

**Important:** File-specific issues with line numbers go in the `comments` array as inline
comments, NOT in the body.

#### Step 5.5: Report to user

After posting, report:

- Count of inline comments posted (e.g., "Posted 4 inline comments")
- Link to PR review
- Review verdict (changes requested / commented)
- Any issues that could not be posted inline (line not in diff)

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
- Naming and semantic conventions
- No memory leaks detected
- JNI exception handling present
- Thread safety verified
- No regressions detected
```
