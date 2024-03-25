import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.maven.publish)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

group = "dev.rlqd.libs"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Use the Kotlin JUnit 5 integration.
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}


/* Download and bundle ranges */

sourceSets.create("utils") {
    java.srcDir("src/utils/kotlin")
    compileClasspath += sourceSets.main.get().output
}

configurations {
    getByName("utilsImplementation") {
        extendsFrom(implementation.get())
    }
    getByName("utilsRuntimeOnly") {
        extendsFrom(runtimeOnly.get())
    }
}

tasks.register<JavaExec>("downloadRanges") {
    description = "Downloads ISBN ranges to include with the distribution"
    group = "build"

    // Configure the task's properties
    classpath = sourceSets.getByName("utils").runtimeClasspath
    mainClass.set("dev.rlqd.isbn.utils.DownloadRangesKt")
}

tasks.jar {
    archiveBaseName = rootProject.name

    dependsOn("downloadRanges")

    from("build/external/isbn-ranges.json") {
        into("dev/rlqd/isbn/ranges")
    }
}


/* Test output jar */

sourceSets.create("testJar") {
    java.srcDir("src/test-jar/kotlin")
    resources.srcDir("src/test-jar/resources")
    compileClasspath += sourceSets.main.get().output
}

configurations {
    getByName("testJarImplementation") {
        extendsFrom(testImplementation.get())
    }
    getByName("testJarRuntimeOnly") {
        extendsFrom(testRuntimeOnly.get())
    }
}

tasks.register<Test>("testJar") {
    description = "Runs tests against the output JAR"
    group = "verification"

    // Ensure the JAR is built first
    dependsOn(tasks.jar)

    useJUnitPlatform()

    val jarTestSrc = sourceSets.named("testJar").get()
    testClassesDirs = jarTestSrc.output.classesDirs
    classpath = jarTestSrc.runtimeClasspath + files(tasks.jar.get().archiveFile)
}

tasks.build {
    // Add jar test as a build step
    dependsOn("testJar")
}


/* Docs */

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(rootProject.name)
    moduleVersion.set(project.version.toString())
}


/* Publish */

mavenPublishing {
    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name.set("Rlqd's ISBN")
        description.set("A kotlin library to parse, validate and convert ISBNs")
        inceptionYear.set("2024")
        url.set("https://github.com/rlqd/isbn/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("rlqd")
                name.set("Rlqd")
                url.set("https://rlqd.dev")
            }
        }
        scm {
            url.set("https://github.com/rlqd/isbn/")
            connection.set("scm:git:git://github.com/rlqd/isbn.git")
            developerConnection.set("scm:git:ssh://git@github.com/rlqd/isbn.git")
        }
    }

    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true,
    ))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}

tasks.forEach {
    if (it.name.startsWith("publish")) {
        it.dependsOn("test", "testJar")
    }
}
