plugins {
    id("com.gradleup.shadow") version "8.3.6" // Import shadow API.
    java // Tell gradle this is a java project.
    eclipse // Import eclipse plugin for IDE integration.
    kotlin("jvm") version "2.1.21" // Import kotlin jvm plugin for kotlin/java integration.
}

java {
    // Declare java version.
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "net.trueog.splegg-og" // Declare bundle identifier.
version = "0.7" // Declare plugin version (will be in .jar).
val apiVersion = "1.19" // Declare minecraft server target version.

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion
    )

    inputs.properties(props) // Indicates to rerun if version changes.

    filesMatching("plugin.yml") {
        expand(props)
    }
    from("LICENSE") { // Bundle license into .jars.
        into("/")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://repo.purpurmc.org/snapshots")
    }
    maven {
    	url = uri("https://jitpack.io") // Jitpack for Vault API.
    }
    maven {
    	url = uri("https://papermc.io/repo/repository/maven-public/") // Paper repository for EssentialsX API.
    }
    maven {
    	url = uri("https://repo.essentialsx.net/releases/") // EssentialsX API repository.
    }
    maven {
    	url = uri("https://maven.enginehub.org/repo") // WorldEdit API repository.
    }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") // Declare purpur API version to be packaged.
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3") // Import MiniPlaceholders API.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") // Import Vault API.
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT") // Import WorldEdit API.
    implementation("net.essentialsx:EssentialsX:2.20.1") // Import EssentialsX API.
    compileOnly(project(":libs:Utilities-OG"))
    compileOnly(project(":libs:GxUI-OG"))
    compileOnly(project(":libs:DiamondBank-OG"))
}

tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    archiveClassifier.set("") // Use empty string instead of null.
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("part")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:deprecation") // Triggers deprecation warning messages.
    options.encoding = "UTF-8"
    options.isFork = true
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}
