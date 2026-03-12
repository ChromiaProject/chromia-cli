# Deployment & Hosting

This document explains how Chromia CLI code reaches production and the deployment mechanics across environments.

## Overview

Chromia CLI is a developer tool distributed through multiple package management systems. The deployment process is **fully automated** and triggered by Git tags. There is no traditional "hosting" environment—the CLI runs locally on developer machines or in CI/CD pipelines.

## Distribution Channels

Chromia CLI is released through the following channels:

| Channel | Target Audience | Registry Location                                                                                        |
|---------|-----------------|----------------------------------------------------------------------------------------------------------|
| GitLab Maven Registry | Java/Kotlin developers, CI pipelines | [chromia-cli package registry](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages)           |
| Docker Registry | Container-based workflows | [chromia-cli container registry](https://gitlab.com/chromaway/core-tools/chromia-cli/container_registry) |
| Homebrew | macOS users | [homebrew-chromia](https://gitlab.com/chromaway/core-tools/homebrew-chromia)                             |
| Scoop | Windows users | [scoop-chromia](https://gitlab.com/chromaway/core-tools/scoop-chromia)                                   |
| APT Repository | Debian/Ubuntu users | [apt-repository-chromia](https://gitlab.com/chromaway/core-tools/apt-repository-chromia)                 |

## Deployment Trigger

Deployments are triggered exclusively by **semantic version tags** pushed to the repository.

```
Pattern: ^[0-9]+\.[0-9]+\.[0-9]+$
Examples: 0.29.7, 1.0.0, 2.1.3
```

Any tag matching this pattern triggers the full release pipeline. Non-matching tags (e.g., `v1.0.0`, `1.0.0-beta`) do not trigger deployment.

### Pipeline Job
However, the suggested approach is to use the pipelines job `release-patch`, and `release-minor` which are available on dev branch in Gitlab. 
To release a new major version, create a new tag manually.

## CI/CD Pipeline

### Pipeline Definition

The CI/CD pipeline is defined in:
- **Primary:** `.gitlab-ci.yml` (root of repository)
- **Shared Templates:** [gitlab.com/chromaway/core-infra/gitlab-automation](https://gitlab.com/chromaway/core-infra/gitlab-automation) (version 1.3.3)
  - `templates/release.yml`
  - `templates/report-code-coverage.yml`
  - `templates/maven-dependency-scanning.yml`
  - `templates/container-dependency-scanning.yml`

### Pipeline Stages

| Stage | Purpose | Trigger |
|-------|---------|---------|
| `build` | Compile, test, generate coverage | All branches (except tags) |
| `release-patch` | Creates new tag to repo triggering release | Manual in UI (dev branch) |
| `release-minor` | Creates new tag to repo triggering release | Manual in UI (dev branch) |
| `code-coverage` | Report test coverage metrics | All branches (except tags) |
| `deploy` | Publish artifacts to registries | Semver tags only |
| `dependency-check` | Security vulnerability scanning | Manual (`RUN_DEPENDENCY_CHECK=true`) |


### Release Stage

After build stage is green on `dev` branch you trigger the job `release-patch` or `release-minor` to push a new tag to the repository. This new tag will trigger the deployment for these gitlab jobs:

| Job | Target Repository | Purpose |
|-----|-------------------|---------|
| `trigger-homebrew-release` | [gitlab.com/chromaway/core-tools/homebrew-chromia](https://gitlab.com/chromaway/core-tools/homebrew-chromia) | Update Homebrew formula |
| `trigger-scoop-release` | [gitlab.com/chromaway/core-tools/scoop-chromia](https://gitlab.com/chromaway/core-tools/scoop-chromia) | Update Scoop manifest |
| `trigger-apt-repo-release` | [gitlab.com/chromaway/core-tools/apt-repository-chromia](https://gitlab.com/chromaway/core-tools/apt-repository-chromia) | Update APT repository |
| `update-docs` | `bitbucket.org/chromawallet/chromia-docs` | Update CLI documentation |
| `gitlab-release` | Same repository | Create GitLab release with notes |

### Documentation Update Flow

When a release is triggered, the `update-docs` job:
1. Clones the `chromia-docs` repository
2. Runs `generate-chromia-cli-docs.sh` to regenerate CLI command documentation
3. Runs `generate-rell-docs.sh` to regenerate Rell documentation
4. Runs `chromia-cli-release-notes.sh` to extract release notes from `CHANGELOG.md`
5. Creates a pull request to `chromia-docs` with the updates

> **Note:** The official chromia docs repository is private, ask system admin for access if needed
 
> **Note:** When a new command is created, manual steps are required. When changing an existing command, documentation
> is updated automatically.

## Pipeline Infrastructure


### Environment Variables

| Variable | Purpose |
|----------|---------|
| `MAVEN_CLI_OPTS` | Maven batch mode, error reporting, custom settings |
| `DOCKER_HOST` | Docker daemon connection (`tcp://docker:2375`) |
| `POSTGRES_*` | Database connection for integration tests |
| `CHR_DB_URL` | CLI database connection for tests |
| `CI_JOB_TOKEN` | GitLab authentication (automatic) |
| `CI_REGISTRY_USER` / `CI_REGISTRY_PASSWORD` | Container registry authentication |

## Known Deployment Risks

### 1. Downstream Pipeline Failures

**Risk:** Homebrew, Scoop, or APT release pipelines may fail independently.

**Impact:** Users on specific platforms won't receive updates.

### 2. Jenkins APT Repository Job

**Risk:** The APT release relies on a Jenkins job (`apt-repository-chromia`) that requires:
- `JENKINS_USER` / `JENKINS_USER_TOKEN` credentials
- `JENKINS_APT_JOB_TOKEN` for job triggering

**Impact:** If Jenkins is unavailable or credentials expire, Linux package updates fail.

**Mitigation:** Monitor Jenkins job status; credentials are managed separately.

### 3. Documentation PR Bottleneck

**Risk:** The `update-docs` job creates PRs that require manual approval.

**Impact:** Documentation may lag behind releases until PR is merged.

**Mitigation:** Prioritize reviewing auto-generated documentation PRs.

## Release Checklist

When preparing a release:

1. **Update CHANGELOG.md** with release notes following the established format
2. **Verify CI passes** on the target commit
3. **Trigger Release** Using gitlabs UI manualy trigger `release-patch` or `release-minor` to push a new tag to repository.
4. **Monitor pipeline stages** in GitLab CI
5. **Verify downstream releases:**
   - Check Homebrew formula update PR
   - Check Scoop manifest update PR
   - Check Jenkins APT job completion
6. **Merge documentation PR** in chromia-docs repository
7. **Verify GitLab release** is created with correct assets

## Rollback Procedure

If a release contains critical bugs:

1. **Do NOT delete the tag** - this causes issues with package managers
2. **Create a new patch release** with the fix (e.g., `0.29.7` → `0.29.8`)
3. For urgent rollback:
   - Manually update Homebrew/Scoop/Apt to point to previous version
   - Docker users can pin to specific version tag