import java.util.concurrent.atomic.AtomicLong

plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.christian34.catchthebeacon"
version = "1.0.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    // party plugins (integrations/PartySupport)
    maven("https://repo.alessiodp.com/releases/") { content { includeGroup("com.alessiodp.parties") } }
    maven("https://simonsator.de/repo/") { content { includeGroup("de.simonsator") } }
    // NPC plugins (integrations/NpcSupport), Floodgate (integrations/BedrockForms)
    maven("https://maven.citizensnpcs.co/repo") { content { includeGroup("net.citizensnpcs") } }
    maven("https://repo.fancyinnovations.com/releases") { content { includeGroup("de.oliver") } }
    maven("https://repo.opencollab.dev/main/") { content { includeGroupByRegex("org\\.geysermc.*") } }
    // TAB, EssentialsX, Multiverse (integrations/TabSupport, EssentialsSupport, MultiverseSupport)
    maven("https://jitpack.io") { content { includeGroup("com.github.NEZNAMY") } }
    maven("https://repo.essentialsx.net/releases/") { content { includeGroup("net.essentialsx") } }
    maven("https://repo.onarandombox.com/multiverse-releases") { content { includeGroupByRegex("org\\.mvplugins.*") } }
    // VaultUnlocked API (integrations/EconomySupport)
    maven("https://repo.codemc.io/repository/creatorfromhell/") { content { includeGroup("net.milkbowl.vault") } }
}

dependencies {
    // Paper API already ships Adventure (incl. MiniMessage + legacy serializer)
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    compileOnly("org.jetbrains:annotations:26.1.0")
    // optional: placeholders for the stats (stats/StatsExpansion)
    compileOnly("me.clip:placeholderapi:2.12.3")
    // optional: parties play together (integrations/PartySupport)
    compileOnly("com.alessiodp.parties:parties-api:3.2.18")
    compileOnly("de.simonsator:spigot-party-api-for-party-and-friends:1.0.7-RELEASE") { isTransitive = false }
    compileOnly("de.simonsator:Party-and-Friends-MySQL-Edition-Spigot-API:1.6.2-RELEASE") { isTransitive = false }
    // optional: join NPCs (integrations/NpcSupport)
    compileOnly("net.citizensnpcs:citizens-main:2.0.44-SNAPSHOT") { isTransitive = false }
    compileOnly("de.oliver:FancyNpcs:2.11.0") { isTransitive = false }
    // optional: forms instead of chest menus for Bedrock players (integrations/BedrockForms)
    // not transitive: geyser-common contains an old Gson, which would hide the one of Paper
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT") { isTransitive = false }
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2") { isTransitive = false }
    // optional: placeholders for MiniMessage texts of other plugins (integrations/CtbMiniPlaceholders)
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
    // optional: TAB doesn't overwrite tab list, name tags and sidebar in games (integrations/TabSupport)
    compileOnly("com.github.NEZNAMY:TAB-API:6.1.2") { isTransitive = false }
    // optional: god mode, vanish, /back, ... (integrations/EssentialsSupport)
    compileOnly("net.essentialsx:EssentialsX:2.21.2") { isTransitive = false }
    // optional: the worlds of games aren't managed by Multiverse (integrations/MultiverseSupport)
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.8.1") { isTransitive = false }
    compileOnly("org.mvplugins.multiverse.inventories:multiverse-inventories:5.3.6") { isTransitive = false }
    // optional: money for kills, beacons and wins (integrations/EconomySupport); VaultUnlocked (the Vault fork) contains
    // the legacy Vault api (net.milkbowl.vault) and its new one (net.milkbowl.vault2), like in EasyPrefix
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.20") { isTransitive = false }

    implementation("org.incendo:cloud-annotations:2.1.0")
    implementation("org.incendo:cloud-paper:2.0.1")
    implementation("org.incendo:cloud-minecraft-extras:2.0.1")
    // anonymous statistics (bstats.org) and error reports (sentry.io), see Telemetry
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("io.sentry:sentry:8.58.0")

    // tests run the plugin against a mocked Paper server
    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains:annotations:26.1.0")
    testImplementation("me.clip:placeholderapi:2.12.3")
    testImplementation("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
    testImplementation("net.milkbowl.vault:VaultUnlockedAPI:2.20") { isTransitive = false }
    // the database drivers Paper ships (stats)
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    testRuntimeOnly("com.mysql:mysql-connector-j:9.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }
    compileTestJava {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        // MySqlTest needs a real database: mysqlTest
        useJUnitPlatform { excludeTags("mysql") }
        // no statistics or error reports from tests
        systemProperty("ctb.offline", "true")
        // sqlite-jdbc loads its native library
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        // MockBukkit reports server methods it doesn't implement as skipped tests - they must not pass silently
        val skipped = AtomicLong()
        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) skipped.set(result.skippedTestCount)
            }
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        })
        doLast {
            if (skipped.get() > 0) {
                throw GradleException("${skipped.get()} tests were skipped (MockBukkit: not implemented?) - see build/reports/tests")
            }
        }
    }

    // copies the plugin into the update folder of a local test server (swapped in on its next start, as Windows
    // locks the loaded jar); set serverPluginsDir in the (git-ignored) gradle.properties to enable
    providers.gradleProperty("serverPluginsDir").orNull?.let { pluginsDir ->
        val deployPlugin = register<Copy>("deployPlugin") {
            from(shadowJar)
            into(file(pluginsDir).resolve("update"))
        }
        build {
            finalizedBy(deployPlugin)
        }

        // builds, deploys, starts the test server until it is ready and stops it again;
        // console commands to run in between: -Pcommands="ctb debug;plugins"
        register<Exec>("testServer") {
            group = "verification"
            dependsOn(deployPlugin)
            val java = project.extensions.getByType<JavaToolchainService>()
                .launcherFor(java.toolchain).get().executablePath.asFile.absolutePath
            val commands = providers.gradleProperty("commands").orNull
            commandLine(buildList {
                addAll(
                    listOf(
                        "powershell",
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        file("scripts/test-server.ps1").absolutePath
                    )
                )
                addAll(listOf("-ServerDir", file(pluginsDir).parentFile.absolutePath, "-Java", java))
                if (commands != null) addAll(listOf("-Commands", commands))
            })
        }
    }
    // the stats against a real MySQL/MariaDB (mysql.host, .port, .database, .username, .password in gradle.properties)
    register<Test>("mysqlTest") {
        group = "verification"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform { includeTags("mysql") }
        systemProperty("ctb.offline", "true")
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        for (key in listOf("host", "port", "database", "username", "password")) {
            providers.gradleProperty("mysql.$key").orNull?.let { systemProperty("mysql.$key", it) }
        }
        outputs.upToDateWhen { false }
    }

    shadowJar {
        archiveClassifier = ""
        // relocates the ServiceLoader files of cloud together with the classes
        mergeServiceFiles()
        // the annotation processor of cloud-annotations is only needed at compile time
        exclude("META-INF/services/javax.annotation.processing.Processor", "META-INF/gradle/**", "META-INF/maven/**")
        // HotSwap replaces classes with unrelocated IDE-compiled ones, so local dev builds may skip relocation
        if (providers.gradleProperty("relocateLibraries").orNull == "false") {
            doFirst { logger.warn("relocateLibraries=false: libraries are not relocated, do not release this jar") }
            return@shadowJar
        }
        relocate("org.incendo.cloud", "com.christian34.catchthebeacon.libs.cloud")
        relocate("io.leangen.geantyref", "com.christian34.catchthebeacon.libs.geantyref")
        // bStats refuses to start unless relocated
        relocate("org.bstats", "com.christian34.catchthebeacon.libs.bstats")
        relocate("io.sentry", "com.christian34.catchthebeacon.libs.sentry")
    }
}

