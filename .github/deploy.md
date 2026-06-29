# deploy.yml: Line by Line

This explains every line of `.github/workflows/deploy.yml`.

---

```yaml
name: Deploy to Elastic Beanstalk
```
Display name shown in the Actions tab on GitHub.

---

```yaml
on:
  push:
    branches: [main]
```
Triggers only on push to `main`. Unlike build.yml which runs on every branch, we only deploy from main. Pushing to a feature branch runs tests only, never deploys.

---

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
```
Same as build.yml: fresh Ubuntu VM, all steps run from the `backend/` folder.

---

```yaml
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        ...
```
Same three setup steps as build.yml. See build.md for the detailed explanation of each.

---

```yaml
      - name: Run tests
        run: ./gradlew test
```
Tests run before every deploy. If any test fails, the workflow stops here and nothing gets deployed. This is the safety gate: broken code never reaches production.

---

```yaml
      - name: Build JAR
        run: ./gradlew bootJar
```
Builds the fat JAR containing the compiled Kotlin code and all dependencies. This is the file that gets uploaded to AWS. Only runs if tests passed.

---

```yaml
      - name: Deploy to Elastic Beanstalk
        uses: einaregilsson/beanstalk-deploy@v22
        with:
          aws_access_key: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws_secret_key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          application_name: stashbox
          environment_name: Stashbox
          region: ap-northeast-1
          version_label: ${{ github.sha }}
          deployment_package: backend/build/libs/stashbox-0.0.1-SNAPSHOT.jar
```
This is a third-party action published by `einaregilsson` on GitHub marketplace. It handles the Beanstalk deployment steps automatically.

What it does internally:
1. Uploads the JAR to an S3 bucket (Beanstalk uses S3 as a staging area)
2. Creates a new application version in Beanstalk using that S3 file
3. Tells the Beanstalk environment to deploy that version
4. Waits until the deployment finishes and checks the result

Breaking down the parameters:

`aws_access_key` and `aws_secret_key`: credentials for the `github-actions` IAM user. Pulled from GitHub secrets, never visible in logs (shown as `*****`).

`application_name: stashbox`: the Beanstalk application name, must match exactly what is in the AWS console.

`environment_name: Stashbox`: the Beanstalk environment name, must match exactly.

`region: ap-northeast-1`: AWS Tokyo region where the environment lives.

`version_label: ${{ github.sha }}`: uses the full git commit SHA as the version label in Beanstalk. This creates a direct traceable link between the running version and the exact commit it came from. In the Beanstalk console under Running version, you will see this SHA.

`deployment_package`: path to the JAR file built in the previous step. The path is relative to the repo root (not the working-directory), so it includes `backend/`.

---

## IAM setup (do this once per AWS account)

The `github-actions` IAM user needs to exist with these settings:

1. Go to IAM in AWS console
2. Create user named `github-actions`
3. Console access: disabled (this user never logs in, only uses access keys)
4. Attach policy: `AdministratorAccess-AWSElasticBeanstalk`
5. Create access key, use case: Third-party service
6. Copy the key ID and secret

Then add them to GitHub: repo Settings → Secrets and variables → Actions → New repository secret:
- `AWS_ACCESS_KEY_ID`: the AKIA... value
- `AWS_SECRET_ACCESS_KEY`: the long secret value

## Verifying a deployment

After a push to main, check:
- GitHub Actions tab: green check on the deploy job
- Beanstalk console: Running version matches the git commit SHA (`git log --oneline -1`)
- Beanstalk Events tab: "Environment update completed successfully"

## How deployment affects the running app

Beanstalk restarts the app with the new JAR. There is a short downtime (around 30 seconds) while it restarts. This is the "All at once" deployment strategy. For a learning project this is fine. In production you would use Rolling or Blue/Green to avoid downtime.
