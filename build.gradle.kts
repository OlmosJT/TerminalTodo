plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "io.olmosjt"
version = "1.0"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("io.olmosjt.terminaltodo")
    // CHANGED: Point to the wrapper Launcher class
    mainClass.set("io.olmosjt.terminaltodo.Launcher")

    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.h2database:h2:2.2.224")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.shadowJar {
    archiveBaseName.set("TerminalTodo")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}