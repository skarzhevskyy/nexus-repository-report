plugins {
	id("java")
	id("application")
	// Create Java API from OpenAPI specification
	id("org.openapi.generator") version "7.14.0"
	// Dependencies list and diff automation in command line and CI/CD
	id("org.cyclonedx.bom") version "2.3.1"
	// Docker image creation
	id("com.google.cloud.tools.jib") version "3.4.5"
	// Allow configuring IntelliJ IDEA project
	id("idea")
}

group = "com.example.nexus-repository-report"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

application {
	mainClass.set("com.pyx4j.nxrm.report.Application")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.3"))
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// Command line arguments
	implementation("info.picocli:picocli:4.7.7")
	implementation("com.opencsv:opencsv:5.11.2")

	implementation("com.google.guava:guava:33.4.8-jre")
	implementation("org.apache.commons:commons-lang3")
	// Runtime validation alternative...
	implementation("org.assertj:assertj-core:3.27.6")

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.xmlunit", module = "xmlunit-core")
		exclude(group = "com.jayway.jsonpath", module = "json-path")
	}
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val generatedSourcesPath = "$projectDir/generated/src/main/java"
val apiDescriptionFile = "$projectDir/src/main/resources/open-api-nexus.json"

// Validating a single specification
openApiValidate {
	inputSpec.set(apiDescriptionFile)
}

openApiGenerate {
	generatorName.set("java")
	inputSpec.set(apiDescriptionFile)
	outputDir.set(generatedSourcesPath)

	modelPackage.set("org.sonatype.nexus.model")
	apiPackage.set("org.sonatype.nexus.api")

	generateApiTests.set(false)
	generateModelTests.set(false)
	generateApiDocumentation.set(false)
	generateModelDocumentation.set(false)

	configOptions.put("library", "webclient")
	configOptions.put("useJakartaEe", "true")
	configOptions.put("openApiNullable", "false")
	configOptions.put("useBeanValidation", "false")
	configOptions.put("hideGenerationTimestamp", "true")
}

// Add the generated sources to the project
java.sourceSets["main"].java.srcDir(generatedSourcesPath)

idea {
	module {
		generatedSourceDirs.add(file(generatedSourcesPath))
	}
}

// Make sure the sources are validated and generated before compiled
tasks {
	val openApiValidate by getting

	val openApiGenerate by getting {
		dependsOn(openApiValidate)
	}

	val compileJava by getting {
		dependsOn(openApiGenerate)
	}
}

tasks.withType<JavaCompile> {
	options.isDeprecation = true
	// Enables http unit tests
	options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed", "standardOut", "standardError")
	}
}

// Disable the default cyclonedxBom task: https://github.com/CycloneDX/cyclonedx-gradle-plugin/issues/596
tasks.named("cyclonedxBom") {
	enabled = false
}

// Example: gradle sbom; vk-sbom-diff sbom-1.json sbom.json
tasks.register("sbom", org.cyclonedx.gradle.CycloneDxTask::class) {
	setIncludeConfigs(listOf("runtimeClasspath"))
	setProjectType("application")
	setSchemaVersion("1.6")
	setDestination(project.file("."))
	setOutputName("sbom")
	setOutputFormat("json")
	setIncludeBomSerialNumber(false)
	setIncludeLicenseText(false)
}

// Default to local Docker daemon for development
// When container.image.repository is not set, JIB builds to local Docker
if (!hasProperty("container.image.name")) {
	extra["container.image.name"] = project.name
}

// JIB configuration for Docker image creation
jib {
	from {
		image = "eclipse-temurin:17-jre-alpine"
	}
	to {
		// Use repository if provided, otherwise build locally with 'gradle jibDockerBuild'
		if (extra.has("container.image.registry")) {
			image = "${extra["container.image.registry"]}/${extra["container.image.name"]}"
		} else {
			image = extra["container.image.name"].toString()
		}
		tags = setOf("latest", version.toString())
	}
	container {
		mainClass = application.mainClass.get()
		ports = listOf()  // CLI application, no exposed ports
		environment = mapOf(
			"JAVA_TOOL_OPTIONS" to "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
		)
		labels = mapOf(
			"org.opencontainers.image.title" to "Nexus Repository Report",
			"org.opencontainers.image.description" to "CLI tool for generating reports from Nexus Repository Manager",
			"org.opencontainers.image.version" to version.toString(),
			"org.opencontainers.image.vendor" to "skarzhevskyy",
			"org.opencontainers.image.licenses" to "Apache-2.0"
		)
		creationTime = "USE_CURRENT_TIMESTAMP"
	}
}
