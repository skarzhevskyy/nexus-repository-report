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

4. Sample output:
```
Repository      Format    Components    Total Size
------------    -------   -----------   ------------
maven-releases  maven            1234         2.1 GB
npm-repo        npm               456         800 MB

TOTAL           -               1690          2.9 GB
```