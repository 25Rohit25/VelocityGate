# CI/CD Pipeline & Deployment Strategy

VelocityGate uses a robust, automated pipeline to ensure code quality, security, and continuous delivery across environments.

## 1. Pipeline Overview (GitHub Actions)

The pipeline is triggered on every **Push to Main** and **Pull Request**. The workflow file (`.github/workflows/ci-cd.yml`) defines the following stages:

### Stage 1: Continuous Integration (CI)

- **Build & Test**: Compiles Java code, runs unit tests, and generates JaCoCo coverage reports.
- **Security Scanning**: Uses **Trivy** to scan the codebase for vulnerabilities (SCA). Fails the build on `CRITICAL` issues.
- **Docker Build**: Builds the OCI-compliant image and pushes to Docker Hub with semantic versioning tags (`v1.2.3`, `sha-xyz`).
- **Image Scan**: Scans the final Docker image for OS-level vulnerabilities before pushing.

### Stage 2: Continuous Deployment (CD)

- **Dev Environment**: Automatically deployed after successful merge to `main`.
- **Staging Environment**: Requires manual approval (GitHub Environment protection). Runs full integration tests.
- **Production Environment**: Requires manual approval + valid staging sign-off. Uses Blue-Green strategy.

## 2. Deployment Strategies

### A. Blue-Green Deployment (Production)

We achieve zero-downtime deployments by running two identical environments: `Blue` (Live) and `Green` (New Release).

1.  **Deploy**: New version (`v2`) is deployed to the `Green` environment.
2.  **Test**: Smoke tests run against `Green` (internal load balancer).
3.  **Cutover**: The main Service selector is updated to point to `Green`.
4.  **Rollback**: Instant revert by pointing Service back to `Blue` if issues arise.

### B. Canary Deployment (Optional)

For high-risk features, we route a small % of traffic (e.g., 5%) to the new version using Istio or Argo Rollouts.

- **Metric Analysis**: Compare error rates/latency between Canary and Baseline.
- **Promotion**: If metrics are healthy, gradually increase traffic to 100%.

## 3. Integration Testing Gates

Quality Gates are enforced between stages:

| Gate               | Description                           | tool/Method           |
| :----------------- | :------------------------------------ | :-------------------- |
| **Code Coverage**  | Ensure > 80% coverage.                | JaCoCo + Codecov      |
| **Contract Tests** | Validate API compatibility.           | Spring Cloud Contract |
| **Load Test**      | Ensure staging handles expected load. | k6                    |
| **Security Audit** | Check dependencies for CVEs.          | Snyk / Trivy          |

---

## 4. Configuration Managment

- **Secrets**: All sensitive data (DB passwords, Cloud Keys) are stored in GitHub Secrets and injected at runtime.
- **Helm Charts**: Infrastructure-as-Code definitions for Kubernetes resources are kept in `./k8s/helm-chart`.

## 5. Rollback Procedure

In case of critical failure in Prod:

1.  **Instant Switch**: Run the `rollback-prod` manual workflow (or `kubectl rollout undo`).
2.  **Verify**: Check Grafana dashboards for error rate recovery.
3.  **Post-Mortem**: Freeze deployments until root cause analysis is complete.
