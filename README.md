# Nexus Repository Report

**nexus-repository-report** is a lightweight reporting tool that connects to [Sonatype Nexus Repository Manager](https://www.sonatype.com/products/repository-oss) using its REST API to generate usage and storage statistics for all hosted repositories.

## ✨ Goals

- Connect to Nexus Repository Manager (OSS or Pro) via its REST API.
- Gather and summarize statistics for components (artifacts) stored in all repositories.
- Provide actionable insights such as:
    - Number of components per repository
    - Total size per repository
    - Breakdown by format (e.g., Maven, npm, Docker)

## 🚀 Features

- Easy integration with any Nexus instance via base URL and credentials
- Works with Nexus 3.x (REST API v1)
- Outputs human-readable reports (console, JSON, CSV)

## Sample Report output:

```
Repository      Format    Components    Total Size
------------    -------   -----------   ------------
maven-releases  maven            1234         2.1 GB
npm-repo        npm               456         800 MB

TOTAL           -               1690          2.9 GB
```

## 🛠️ Usage

1. Clone the repository:
   ```bash
   git clone https://github.com/skarzhevskyy/nexus-repository-report.git
   cd nexus-repository-report

2. Configure connection settings:

   set the following environment variables:
```
NEXUS_URL=https://nexus.example.com
NEXUS_USERNAME=admin
NEXUS_PASSWORD=yourpassword
```

3. Run the tool:
```
./gradlew run
```

### Proxy Support

The tool supports proxy configuration through multiple methods:

```bash
# Command line proxy argument (highest priority)
./gradlew run --args="--url https://nexus.example.com --proxy proxy.company.com:8080"
./gradlew run --args="--url https://nexus.example.com --proxy http://user:pass@proxy.company.com:8080"

# Environment variables
export HTTP_PROXY=http://proxy.company.com:8080
export HTTPS_PROXY=http://proxy.company.com:8080
./gradlew run

# Java system properties
./gradlew run -Dhttp.proxyHost=proxy.company.com -Dhttp.proxyPort=8080
```

## 🐳 Docker Usage

The application is also available as a Docker container from GitHub Container Registry.

### Running with Docker

```bash
# Basic usage with environment variables
docker run --rm \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_USERNAME=admin \
  -e NEXUS_PASSWORD=yourpassword \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest

# Using command line arguments
docker run --rm \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest \
  --url https://nexus.example.com \
  --username admin \
  --password yourpassword

# Using authentication token instead of username/password
docker run --rm \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_TOKEN=your-nexus-token \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest

# With proxy configuration
docker run --rm \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_USERNAME=admin \
  -e NEXUS_PASSWORD=yourpassword \
  -e HTTP_PROXY=http://proxy.company.com:8080 \
  -e HTTPS_PROXY=http://proxy.company.com:8080 \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest

# Using command line proxy option
docker run --rm \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest \
  --url https://nexus.example.com \
  --username admin \
  --password yourpassword \
  --proxy proxy.company.com:8080
```

### Available Environment Variables

- `NEXUS_URL` - Nexus Repository Manager URL (required)
- `NEXUS_USERNAME` - Username for authentication
- `NEXUS_PASSWORD` - Password for authentication
- `NEXUS_TOKEN` - Authentication token (alternative to username/password)
- `HTTP_PROXY` - HTTP proxy URL
- `HTTPS_PROXY` - HTTPS proxy URL
