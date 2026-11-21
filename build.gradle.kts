plugins {
    java
    alias(libs.plugins.openapi.generator)
}

group = "com.sternitc"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

repositories {
    mavenCentral()
}

val openApiOutputDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(layout.projectDirectory.file("src/api/task-api.yaml").asFile.absolutePath)
    outputDir.set(openApiOutputDir)
    apiPackage.set("com.sternitc.task.api")
    modelPackage.set("com.sternitc.task.api.model")
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "useSpringBoot4" to "true",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "useBeanValidation" to "true",
            "delegatePattern" to "false",
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "supportingFiles" to "",
        )
    )
}

val openapi = sourceSets.create("openapi") {
    java.srcDir(openApiOutputDir.map { it.dir("src/main/java") })
}

dependencies {
    "openapiImplementation"(platform(libs.spring.boot.dependencies))
    "openapiImplementation"(libs.spring.boot.starter.web)
    "openapiImplementation"(libs.spring.boot.starter.validation)

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.mongodb.driver.sync)
    implementation(openapi.output)

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.awaitility)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<JavaCompile>(openapi.compileJavaTaskName) {
    dependsOn(tasks.openApiGenerate)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters"))
}

tasks.named<JavaCompile>("compileTestJava") {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters"))
}

tasks.test {
    useJUnitPlatform()
}
