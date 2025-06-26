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

### Repository Summary Report:

```
Repository      Format    Components    Total Size
------------    -------   -----------   ------------
maven-releases  maven            1234         2.1 GB
npm-repo        npm               456         800 MB

TOTAL           -               1690          2.9 GB
```

### Top Consuming Groups Report:

```
Top Consuming Groups (by Components)

Group                        Components    Total Size
---------------------------  -----------   ------------
org.springframework          1100          1.8 GB
com.example                   150          6.2 MB
org.apache                      8          7.0 MB
```

### Component Age Distribution Report:

```
Component Age Distribution

Age Range         Components    Total Size
---------------   ----------    ----------
   0  -   7 days         120         234 MB
   8  -  30 days         270         421 MB
  31  -  90 days         471         578 MB
  91  - 365 days         760         789 MB
>365 days                 69         878 MB

TOTAL                   1690         2.9 GB
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

This will run all reports by default (both repositories summary and top consuming groups).

To generate specific reports:
```bash
# Generate all reports (default)
./gradlew run --args="all --url https://nexus.example.com --username admin --password yourpassword"

# Generate only repositories summary
./gradlew run --args="repositories-summary --url https://nexus.example.com --username admin --password yourpassword"

# Generate only top consuming groups report
./gradlew run --args="top-groups --url https://nexus.example.com --username admin --password yourpassword"

# Generate age distribution report
./gradlew run --args="age-report --url https://nexus.example.com --username admin --password yourpassword"

# Generate age distribution report with custom age buckets
./gradlew run --args="age-report --url https://nexus.example.com --username admin --password yourpassword --age-buckets '0-14,15-30,31-180,>180'"
```

### Report Types

The tool supports three report types:

- **`repositories-summary`**: Shows storage consumption per repository with component counts
- **`top-groups`**: Shows top consuming groups (e.g., Maven groupId, npm scope) with configurable sorting and limits
- **`age-report`**: Shows component age distribution categorized by time since creation

> The `all` option (default) generates all three reports above.

### Filtering Options

The tool supports filtering components based on their creation, update, and download timestamps:

```bash
# Filter components created in the last 30 days (generates all reports)
./gradlew run --args="--url https://nexus.example.com --created-after 30d"

# Filter components created before a specific date (generates all reports)
./gradlew run --args="--url https://nexus.example.com --created-before 2024-06-01"

# Filter components updated within a date range
./gradlew run --args="--url https://nexus.example.com --updated-after 2024-06-01 --updated-before 2024-06-30"

# Filter components downloaded in the last 7 days
./gradlew run --args="--url https://nexus.example.com --downloaded-after 7d"

# Only include components that have never been downloaded
./gradlew run --args="--url https://nexus.example.com --never-downloaded"

# Combine multiple filters (AND logic)
./gradlew run --args="--url https://nexus.example.com --created-after 30d --never-downloaded"
```

#### Available Filter Options

- `--created-before <date|days-ago>` - Filter components created before this date
- `--created-after <date|days-ago>` - Filter components created after this date  
- `--updated-before <date|days-ago>` - Filter components updated before this date
- `--updated-after <date|days-ago>` - Filter components updated after this date
- `--downloaded-before <date|days-ago>` - Filter components downloaded before this date
- `--downloaded-after <date|days-ago>` - Filter components downloaded after this date
- `--never-downloaded` - Only include components that have never been downloaded

**Date Formats:**
- ISO-8601 format: `2024-06-01T00:00:00Z` or just date `2024-06-01`
- Days ago: `30d` (30 days ago), `7d` (7 days ago), `1d` (1 day ago)

**Note:** Filters are composable using AND logic. Multiple filters can be combined except `--never-downloaded` cannot be used with `--downloaded-before` or `--downloaded-after`.

#### Component Filtering

The tool also supports filtering components by repository, group, and name with wildcard pattern support:

```bash
# Filter by specific repository
../gradlew run --args="--url https://nexus.example.com --repository maven-central"

# Multiple repositories with wildcards (OR logic)
../gradlew run --args="--url https://nexus.example.com --repository 'maven-*' --repository npm-repo"

# Group filtering with wildcards and multiple groups
../gradlew run --args="--url https://nexus.example.com --group 'org.spring*' --group '*acme*' --repository 'maven-*'"

# Name filtering with wildcards
../gradlew run --args="--url https://nexus.example.com --name 'spring-*' --name 'junit?'"

# Combined component and date filtering (AND logic between different filter types)
../gradlew run --args="--url https://nexus.example.com --repository 'maven-*' --group 'com.example.*' --name 'spring-*' --created-after 30d"
```

**Component Filter Options:**
- `--repository <pattern>` - Filter components by repository name (can be specified multiple times)
- `--group <pattern>` - Filter components by group (can be specified multiple times)
- `--name <pattern>` - Filter components by name (can be specified multiple times)

**Wildcard Support:**
- `*` - Matches any number of characters
- `?` - Matches a single character

**Filter Logic:**
- Multiple values for the same filter type use OR logic (e.g., multiple `--group` arguments)
- Different filter types use AND logic (e.g., `--repository` AND `--group` AND `--name`)

### Top Groups Report Options

The tool can generate a report showing the top consuming groups (e.g., Maven groupId, npm scope):

```bash
# Generate only top 10 groups report sorted by components
./gradlew run --args="top-groups --url https://nexus.example.com"

# Show top 5 groups sorted by total size
./gradlew run --args="top-groups --url https://nexus.example.com --top-groups 5 --group-sort size"

# Combine with filtering options
./gradlew run --args="top-groups --url https://nexus.example.com --repository 'maven-*' --created-after 30d"
```

**Top Groups Options:**
- `--top-groups <N>` - Show only the top N groups (default: 10)
- `--group-sort <components|size>` - Sort groups by number of components or total size (default: components)

### Age Report Options

The tool can generate a report showing component age distribution categorized by time since creation:

```bash
# Generate age distribution report with default buckets (0-7, 8-30, 31-90, 91-365, >365 days)
./gradlew run --args="age-report --url https://nexus.example.com"

# Generate age distribution report with custom age buckets
./gradlew run --args="age-report --url https://nexus.example.com --age-buckets '0-14,15-30,31-180,>180'"

# Combine with filtering options
./gradlew run --args="age-report --url https://nexus.example.com --repository 'maven-*' --created-after 30d"
```

**Age Report Options:**
- `--age-buckets <buckets>` - Define custom age ranges (default: "0-7,8-30,31-90,91-365,>365")
  - Format: Comma-separated list of ranges like "0-7", "8-30", ">365"
  - Examples: "0-14,15-30,31-180,>180" or "0-7,8-30,31-90,91-365,>365"

**Age Calculation:**
- Component age is calculated from the earliest asset creation date (`blobCreated`)
- Age is measured in days from current UTC time
- Components are categorized into the first matching age bucket

### File Export Options

The tool can export reports and filtered component lists to JSON or CSV files.

**Report File Generation:**

- `--output-file <path>` - Save the report to a file. The format is determined by the file extension (`.json` or `.csv`).

```bash
# Save the top-groups report to a CSV file
./gradlew run --args="top-groups --output-file top_groups.csv"
```

**Filtered Component Export:**

- `--output-component <path>` - Save the list of filtered components to a file. The format is determined by the file extension (`.json` or `.csv`). This option must be used with a report-generating command.

```bash
# Run the age-report and export the underlying component data to a JSON file
./gradlew run --args="age-report --created-after 30d --output-component new_components.json"
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

# Using authentication token instead of username/password and with proxy configuration
docker run --rm \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_TOKEN=your-nexus-token \
  -e HTTPS_PROXY=http://proxy.company.com:8080 \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest

# Exporting components to a file on the host system
docker run --rm \
  -v "$(pwd)/reports:/app/reports" \
  ghcr.io/skarzhevskyy/nexus-repository-report:latest \
  --url https://nexus.example.com \
  --username admin \
  --password yourpassword \
  --output-component /app/reports/components.csv
```

### Available Environment Variables

- `NEXUS_URL` - Nexus Repository Manager URL (required)
- `NEXUS_USERNAME` - Username for authentication
- `NEXUS_PASSWORD` - Password for authentication
- `NEXUS_TOKEN` - Authentication token (alternative to username/password)
- `HTTP_PROXY` - HTTP proxy URL
- `HTTPS_PROXY` - HTTPS proxy URL
