import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.gradleup.shadow") version "9.6.1"
    kotlin("jvm") version "2.3.20"
}

group = "org.winlogon.infohub"

fun getTime(): String {
    val sdf = SimpleDateFormat("yyMMdd-HHmm")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date()).toString()
}

val shortVersion: String? = if (project.hasProperty("ver")) {
    val ver = project.property("ver").toString()
    if (ver.startsWith("v")) {
        ver.substring(1).uppercase()
    } else {
        ver.uppercase()
    }
} else {
    null
}

val version: String = when {
    shortVersion.isNullOrEmpty() -> "${getTime()}-SNAPSHOT"
    shortVersion.contains("-RC-") -> shortVersion.substringBefore("-RC-") + "-SNAPSHOT"
    else -> shortVersion
}

val pluginName = rootProject.name
val pluginVersion = version
val pluginPackage = project.group.toString()
val projectName = rootProject.name

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeModule("net.md-5", "bungeecord-chat")
        }
    }
    maven {
        name = "minecraft"
        url = uri("https://libraries.minecraft.net")
        content {
            includeModule("com.mojang", "brigadier")
        }
    }
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }

    maven {
        name = "winlogon-libs"
        url = uri("https://maven.winlogon.org/releases/")
    }

    mavenCentral()
}

val minecraftVersion = "26.1.2"

dependencies {
    compileOnly("io.papermc.paper:paper-api:${minecraftVersion}.build.+")
    compileOnly("dev.jorel:commandapi-paper-core:12.1.0")
    compileOnly("com.github.oshi:oshi-core:7.6.1")

    compileOnly("com.zaxxer:HikariCP:7.1.0")
    compileOnly("org.postgresql:postgresql:42.7.13")
    compileOnly("com.mysql:mysql-connector-j:26.7.0")
    compileOnly("io.lettuce:lettuce-core:7.8.0.RELEASE")

    compileOnly("org.winlogon:asynccraftr:0.2.0")
    
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
    testImplementation("io.papermc.paper:paper-api:${minecraftVersion}.build.+")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching("**/paper-plugin.yml") {
        expand(
            "NAME" to pluginName,
            "VERSION" to pluginVersion,
            "PACKAGE" to pluginPackage
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // relocate("io.papermc.lib", "shadow.io.papermc.paperlib")
    minimize()
}

// Disable jar and replace with shadowJar
tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Utility tasks
tasks.register("printProjectName") {
    doLast {
        println(projectName)
    }
}

var shadowJarTask = tasks.shadowJar.get()
tasks.register("release") {
    dependsOn(tasks.build)
    doLast {
        if (!version.endsWith("-SNAPSHOT")) {
            shadowJarTask.archiveFile.get().asFile.renameTo(
                file("${layout.buildDirectory.get()}/libs/${rootProject.name}.jar")
            )
        }
    }
}
