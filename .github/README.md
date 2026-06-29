# GitHub Actions — Stashbox CI/CD

Workflow files live here because GitHub requires the `.github/workflows/` path. There is nothing to run locally; GitHub's runners execute these on their own Linux VMs.

## How it works

No installation or setup is needed. GitHub automatically scans every repo for `.github/workflows/*.yml` files. The moment you push one, it becomes active. No webhooks, no third-party services, no account linking.

When you push code, GitHub spins up a fresh Linux VM, runs the steps in the workflow file, and shows the result in the Actions tab. The VM is discarded after each run.

## Setting up on a new machine

Nothing to do. The workflow files live in the repo, so cloning the repo is all you need. GitHub Actions runs on GitHub's servers, not your machine.

If you are setting this up for a brand new repo from scratch:
1. Create the `.github/workflows/` folder in your repo
2. Add a `.yml` file (copy `build.yml` from here as a starting point)
3. Push to GitHub
4. Go to the Actions tab, it will already be running

## Workflows

### `build.yml` — Build & Test

Triggers on every push to any branch, and on pull requests to `main`.

Steps:
1. Clone the repo onto a fresh Ubuntu VM
2. Install Java 21 (Temurin)
3. Restore Gradle dependency cache to avoid re-downloading everything each run
4. Run `./gradlew test` inside `backend/` — compiles the code and runs all unit tests
5. Upload the HTML test report as a downloadable artifact (runs even on failure so you can see what broke)

Fails if any test fails or the code does not compile.

### `deploy.yml` — Deploy to Elastic Beanstalk (Phase 7b, not yet created)

Will trigger on push to `main` only. Will build the JAR and deploy it to AWS Elastic Beanstalk.

## Secrets needed

Secrets are encrypted values stored in GitHub, never in the code. Set them at: repo Settings → Secrets and variables → Actions → New repository secret.

| Secret name | Used by | What it is |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | deploy.yml | AWS IAM access key for deployments |
| `AWS_SECRET_ACCESS_KEY` | deploy.yml | AWS IAM secret key for deployments |

No secrets are needed for `build.yml` — it only compiles and tests.

## Reading the results

Go to the Actions tab on GitHub. Each push shows a workflow run. Green check means all tests passed, red X means something failed. Click into a failed run, then click the `build` job to see step-by-step logs. Download the `test-report` artifact for the full HTML report.
