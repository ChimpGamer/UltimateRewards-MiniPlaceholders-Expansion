plugins {
    kotlin("jvm") version "2.1.10"
}

group = "nl.chimpgamer"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.3.0")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:2.3.0")

    compileOnly(fileTree("libs")) // Store UltimateRewards jar in libs folder.
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
