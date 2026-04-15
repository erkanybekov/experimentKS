import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.plugin.spring)
	alias(libs.plugins.kotlin.plugin.jpa)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.erkan"
version = "0.1.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.json)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.flyway.postgresql)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.kotlin.reflect)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.coroutines.reactor)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	runtimeOnly(libs.postgresql)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.security.test)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
		freeCompilerArgs.addAll(
			"-Xjsr305=strict",
			"-Xannotation-default-target=param-property",
		)
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
