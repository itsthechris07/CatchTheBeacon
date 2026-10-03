plugins {
    application
    id("com.gradleup.shadow")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    // Minecraft client protocol (GeyserMC) - the version has to match the server's Minecraft version
    implementation("org.geysermc.mcprotocollib:protocol:26.3-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-plain:5.2.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass = "com.christian34.catchthebeacon.bot.TestBots"
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }
    named<JavaExec>("run") {
        // ./gradlew :bot:run --args="2 --join" : console input goes to the bots (e.g. "say hi", "stop")
        standardInput = System.`in`
    }
}
