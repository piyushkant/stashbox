# GitHub Actions — Stashbox CI/CD

Workflow files live here because GitHub requires the `.github/workflows/` path. There is nothing to run locally; GitHub's runners execute these automatically.

## Workflows

### `build.yml` — Build & Test
Triggers on every push to any branch, and on pull requests to `main`.

What it does:
1. Checks out the code
2. Sets up Java 21 (Temurin)
3. Restores Gradle dependency cache (speeds up subsequent runs)
4. Runs `./gradlew test` in the `backend/` folder — compiles + runs all unit tests
5. Uploads the HTML test report as a downloadable artifact (even on failure, so you can see what broke)

Fails if: any test fails or the code doesn't compile.

### `deploy.yml` — Deploy to Elastic Beanstalk _(Phase 7b, not yet created)_
Will trigger on push to `main` only. Will build the JAR and deploy it to AWS Elastic Beanstalk.

## Secrets needed

| Secret name | Used by | Where to set |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | deploy.yml | GitHub → repo → Settings → Secrets and variables → Actions |
| `AWS_SECRET_ACCESS_KEY` | deploy.yml | same |

No secrets are needed for `build.yml` — it only compiles and tests.

## Reading the results

Go to the **Actions** tab on GitHub. Each push shows a workflow run — green check means all tests passed, red X means something failed. Click into a failed run to see which test failed and why. Download the test report artifact for the full HTML report.
