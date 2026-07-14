import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Base64

plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    testImplementation("io.quarkus:quarkus-junit5")
}

group = "sisve.ec"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

val generateDevJwtKeys by tasks.registering {
    group = "development"
    description = "Genera las claves RSA JWT para desarrollo"

    doLast {
        val resourcesDirectory = layout.projectDirectory
            .dir("src/main/resources")
            .asFile
            .toPath()

        val privateKeyPath = resourcesDirectory.resolve("privateKey.pem")
        val publicKeyPath = resourcesDirectory.resolve("publicKey.pem")

        fun isValidPem(
            path: java.nio.file.Path,
            expectedHeader: String
        ): Boolean {
            if (!Files.isRegularFile(path)) {
                return false
            }

            if (Files.size(path) == 0L) {
                return false
            }

            return Files.readString(path)
                .contains(expectedHeader)
        }

        val privateKeyValid = isValidPem(
            privateKeyPath,
            "-----BEGIN PRIVATE KEY-----"
        )

        val publicKeyValid = isValidPem(
            publicKeyPath,
            "-----BEGIN PUBLIC KEY-----"
        )

        if (privateKeyValid && publicKeyValid) {
            println("Las claves JWT de desarrollo ya existen.")
            return@doLast
        }

        Files.createDirectories(resourcesDirectory)

        val keyPairGenerator = KeyPairGenerator
            .getInstance("RSA")
            .apply {
                initialize(2048, SecureRandom())
            }

        val keyPair = keyPairGenerator.generateKeyPair()

        fun toPem(
            type: String,
            encodedKey: ByteArray
        ): String {
            val lineSeparator = byteArrayOf(
                '\n'.code.toByte()
            )

            val base64 = Base64
                .getMimeEncoder(64, lineSeparator)
                .encodeToString(encodedKey)

            return buildString {
                appendLine("-----BEGIN $type-----")
                appendLine(base64)
                appendLine("-----END $type-----")
            }
        }

        Files.writeString(
            privateKeyPath,
            toPem(
                type = "PRIVATE KEY",
                encodedKey = keyPair.private.encoded
            ),
            StandardCharsets.US_ASCII
        )

        Files.writeString(
            publicKeyPath,
            toPem(
                type = "PUBLIC KEY",
                encodedKey = keyPair.public.encoded
            ),
            StandardCharsets.US_ASCII
        )

        println("Claves JWT de desarrollo generadas:")
        println(" - $privateKeyPath")
        println(" - $publicKeyPath")
    }
}

tasks.named("quarkusDev") {
    dependsOn(generateDevJwtKeys)
}
