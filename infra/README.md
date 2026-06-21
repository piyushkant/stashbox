# infra

Infrastructure and deployment for Stashbox: AWS setup, hosting, and related config.

This is where AWS-related files and notes live (kept separate from the app code). For now it's mostly notes; actual config/scripts get added when we reach the deploy phase.

See the root [PLAN.md](../PLAN.md), Phase 6 (Deploy backend to AWS) and Phase 7 (CI/CD). Nothing set up yet.

## Planned

- AWS account + IAM user (not root), AWS CLI configured. **Billing alarm first**, before creating any chargeable resource.
- PostgreSQL on RDS (free-tier `db.t3.micro`).
- Backend on Elastic Beanstalk (single-instance, no load balancer to stay in free tier).
- Web on S3 + CloudFront.
- Cost: stay on free tier for the first 12 months; tear down what's not in use.

Note: the local LLM (Ollama/Llama) is NOT deployed to AWS (cloud GPU is expensive), so AI stays a local-only capability.
