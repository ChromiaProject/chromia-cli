# Setup & Local Development

This guide enables a new engineer to run the Chromia CLI project locally from a fresh clone.

## Required Tools and Versions

| Tool | Version | Notes |
|------|---------|-------|
| Java (JDK) | 21+ | Required for compilation and runtime |
| Maven | 3.6+ | Build tool (3.3.0+ recommended for CI-friendly versioning) |
| Docker | 20+ | Required for containerization and some integration tests |
| PostgreSQL | 14+ | Required for integration tests (14.9 used in CI) |
| Git | 2.x | For source control and dependency checkout |

### Optional Tools

| Tool | Purpose |
|------|---------|
| [direnv](https://direnv.net) | Automatically adds build artifact to PATH for manual testing |

## OS / Platform

Windows is supported for running the CLI (via `.bat` script) but the development environment is primarily Unix-based.
If you are on a windows machine, extra setup might be required for development.


## Environment Variables

### Runtime Environment

| Variable | Purpose | Default |
|----------|---------|---------|
| `RELL_JAVA` | Path to custom Java installation | Auto-detected |
| `CHR_LOG_LEVEL` | Logging verbosity (trace, debug, info, warn, error) | `info` |
| `CHR_LOG_FOLDER` | Directory for log files | `/tmp/chromia` (or `$TMPDIR/chromia`) |

### CI-Only Variables (not needed locally)

| Variable | Purpose |
|----------|---------|
| `CI_JOB_TOKEN` | GitLab Maven registry authentication |
| `CI_REGISTRY_USER` | Docker registry username |
| `CI_REGISTRY_PASSWORD` | Docker registry password |

## Step-by-Step Setup Instructions

### 1. Install Java 21

**macOS (Homebrew):**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install openjdk-21-jdk
```

**Verify installation:**
```bash
java -version
# Should show: openjdk version "21.x.x"
```

### 2. Install Maven

**macOS (Homebrew):**
```bash
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install maven
```

**Verify installation:**
```bash
mvn -version
# Should show Maven 3.x with Java 21
```

### 3. Install Docker

**macOS:**
```bash
brew install --cask docker
# Then open Docker Desktop from Applications
```

**Linux:**
Follow the official Docker installation guide for your distribution: https://docs.docker.com/engine/install/

**Verify installation:**
```bash
docker --version
docker run hello-world
```

### 4. Setup PostgreSQL

Follow the setup guide at: https://docs.chromia.com/get-started/installation#set-up-postgresql-database.
Recommended to use Docker.

### 5. Clone the Repository

```bash
git clone https://gitlab.com/chromaway/core-tools/chromia-cli.git
cd chromia-cli
```

### 7. Build the Project

```bash
mvn install
```

This will:
- Download all dependencies
- Compile Kotlin source code
- Run unit tests
- Package the CLI distribution

The build artifact will be available at:
```
chromia-cli/target/chromia-cli-dev-dist/bin/chr
```

### 8. (Optional) Setup direnv for PATH

If you have direnv installed:

```bash
# Allow the .envrc file
direnv allow
```

This automatically adds the build artifact to your PATH, allowing you to run `chr` directly in the repository.

Without direnv, you can manually add to PATH:
```bash
export PATH="$PATH:$(pwd)/chromia-cli/target/chromia-cli-dev-dist/bin"
```

## How to Run Locally

### Running the CLI

After building, run the CLI using:

```bash
# With direnv or PATH configured:
chr --help

# Or directly:
./chromia-cli/target/chromia-cli-dev-dist/bin/chr --help
```

## How to Run Tests

### Run All Tests (Unit + Integration)

```bash
mvn verify
```

### Run Specific Test Class

```bash
mvn test -Dtest=TestClassName
mvn test -Dtest=TestClassName#testMethodName
```

## Building Docker Image

To build a Docker image locally:

```bash
mvn install -Pdocker
```

This creates a Docker image named `chr` in your local Docker registry.

## Common Setup Pitfalls and Fixes

### 1. Java Version Mismatch

**Error:**
```
Error: Failed to find a valid java executable
This application requires a minimum of Java 21.
```

**Fix:** Ensure Java 21+ is installed and either:
- Set `RELL_JAVA` to point to Java 21 executable
- Ensure Java 21 is in your PATH
- Install Java 21 in standard locations (`/usr/lib/jvm/java-21-openjdk-amd64` on Linux or `/opt/homebrew/opt/openjdk@21` on macOS)


### 2. Database Connection Refused

**Error:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Fix:**
- Verify PostgreSQL is running: `pg_isready` or `docker ps`
- Check the connection URL in `CHR_DB_URL`
- Ensure the database port (5432) is accessible

### 3. Maven Dependency Download Failures

**Error:**
```
Could not resolve dependencies for project...
```

**Fix:**
- The project uses private GitLab Maven registries
- For CI builds, `CI_JOB_TOKEN` is used automatically
- For local builds, dependencies should resolve from cached or public repositories
- If issues persist, check your network/proxy settings


## Development Workflow

1. Make code changes in `chromia-cli/src/main/kotlin/`
2. Add tests that cover your code changes
3. Run `mvn compile` for quick compilation check
4. Run `mvn test` for unit tests
5. Run `mvn install` to rebuild the distribution
6. Test manually using `chr` command

## Shell Auto-Completion

After building, enable shell auto-completion:

**Bash:**
```bash
chr --generate-completion=bash > ~/chr-completion.sh
source ~/chr-completion.sh
```

**Zsh:**
```bash
chr --generate-completion=zsh > ~/chr-completion.zsh
source ~/chr-completion.zsh
```

Note: Auto-completion scripts need to be regenerated after each CLI version update.
