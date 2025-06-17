plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	// Dependencies list and diff automation in command line and CI/CD
	id("org.cyclonedx.bom") version "2.3.1"
}

group = "com.example.template-java-gradle-spring"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

	implementation("org.apache.tomcat.embed:tomcat-embed-core:10.1.41") {
		exclude(group = "org.apache.tomcat", module = "tomcat-annotations-api")
	}

	implementation("com.google.guava:guava:33.4.8-jre")
	implementation("org.apache.commons:commons-lang3")
	// Runtime validation alternative...
	implementation("org.assertj:assertj-core:3.27.3")

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.xmlunit", module = "xmlunit-core")
		exclude(group = "com.jayway.jsonpath", module = "json-path")
	}
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
