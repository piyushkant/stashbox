# build.yml — Line by Line

This explains every line of `.github/workflows/build.yml`.

---

```yaml
name: Build & Test
```
Display name shown in the Actions tab on GitHub. Just a label, no functional effect.

---

```yaml
on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]
```
When to trigger the workflow. `**` matches every branch, so any push anywhere runs this. It also runs when a pull request targeting `main` is opened or updated. Think of it like a Bitrise trigger condition.

---

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
```
A workflow has one or more jobs. This one is named `build`. It runs on a fresh Ubuntu Linux VM that GitHub spins up automatically. The machine is clean every time — nothing from previous runs carries over.

---

```yaml
    defaults:
      run:
        working-directory: backend
```
Every `run` step below will execute from inside the `backend/` folder. Without this you would need to write `cd backend &&` before every command.

---

```yaml
      - name: Checkout code
        uses: actions/checkout@v4
```
The VM starts with no code on it. This step clones your repo onto the VM. `uses` means it is a pre-built action from GitHub's marketplace, not a shell command you wrote yourself. `@v4` is the version of that action.

---

```yaml
      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
```
Installs Java 21 on the VM. The default Ubuntu runner does not have the right Java version pre-installed. Temurin is the free open-source JDK from Eclipse, same one you install locally via Homebrew.

---

```yaml
      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('backend/**/*.gradle*', 'backend/**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
```
Every run starts on a fresh VM, so without this step Gradle would re-download all dependencies from the internet on every run (adds 30 to 60 seconds). This step saves the Gradle folders between runs.

`path` tells GitHub which folders to cache.

`key` is a fingerprint made from the OS name and a hash of your Gradle files. If `build.gradle.kts` changes, the hash changes, the old cache is thrown away, and a fresh one is created. If nothing changed, the previous cache is restored instantly.

`restore-keys` is a fallback. If no exact key match is found, use any cache that at least starts with `linux-gradle-`. Better than starting from scratch.

`${{ }}` is GitHub Actions template syntax, similar to string interpolation in Kotlin.

---

```yaml
      - name: Run tests
        run: ./gradlew test
```
The actual work. Compiles the Kotlin code and runs all JUnit tests. If any test fails, the workflow stops here and the run is marked as failed with a red X.

---

```yaml
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: backend/build/reports/tests/test/
```
Uploads the HTML test report as a downloadable file on the run summary page.

`if: always()` means this step runs even when the previous step (tests) failed. Without it, a test failure would skip this step and you would have no report to inspect. Always uploading it means you can open the HTML report and see exactly which test broke and why.
