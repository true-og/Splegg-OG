rootProject.name = "Splegg-OG"

val requiredLibraries = listOf("DiamondBank-OG", "GxUI-OG", "Utilities-OG")
val missingLibraries = requiredLibraries.filter { libraryName -> !file("libs/$libraryName/build.gradle.kts").exists() }

if (missingLibraries.isNotEmpty()) {
    throw GradleException(
        "Missing initialized git submodules: ${missingLibraries.joinToString(", ")}. " +
            "Run ./bootstrap.sh or `git submodule update --init --recursive` before building."
    )
}

file("libs")
    .listFiles()
    ?.filter { directory ->
        directory.isDirectory && !directory.name.startsWith(".") && file("${directory.path}/build.gradle.kts").exists()
    }
    ?.forEach { directory ->
        include(":libs:${directory.name}")
        project(":libs:${directory.name}").projectDir = directory
    }
