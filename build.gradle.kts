/*
 * Jenkins Aliyun OSS Plugin - Build Configuration
 * 
 * Available tasks:
 * - ./gradlew build          : Build and test the plugin
 * - ./gradlew jar            : Build JAR file
 * - ./gradlew hpi            : Build HPI plugin archive
 * - ./gradlew test           : Run tests
 * - ./gradlew pluginInfo     : Display plugin information
 * - ./gradlew dist           : Build plugin distribution package
 */

plugins {
    `java-library`
    `maven-publish`
}

group = "org.jenkins-ci.plugins"
version = "1.0.0-M1"
description = "Aliyun OSS Plugin for Jenkins"

java.sourceCompatibility = JavaVersion.VERSION_21

// Jenkins repository configuration
repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.jenkins-ci.org/public/")
    }
    mavenCentral()
}

// Jenkins core version
val jenkinsVersion = "2.440.3"
val jenkinsTestHarnessVersion = "2211.v29b_b_b_2e79c5a_"

dependencies {
    // Jenkins core dependencies
    compileOnly("org.jenkins-ci.main:jenkins-core:${jenkinsVersion}")
    compileOnly("jakarta.servlet:jakarta.servlet-api:4.0.4")
    compileOnly("commons-logging:commons-logging:1.3.0")
    
    // Plugin dependencies
    api("com.aliyun.oss:aliyun-sdk-oss:3.17.4")
    api("commons-lang:commons-lang:2.6")
    api("org.jenkins-ci.plugins:jackson2-api:2.17.0-379.v02de8ec9f64c")
    api("org.jenkins-ci.plugins.workflow:workflow-cps:3894.3896.vca_2c931e7935")
    
    // Test dependencies
    testImplementation("org.jenkins-ci.main:jenkins-test-harness:${jenkinsTestHarnessVersion}")
    testImplementation("org.jenkins-ci.main:jenkins-war:${jenkinsVersion}")
    testImplementation("org.jenkins-ci:test-annotations:1.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Exclude conflicting dependencies
configurations.all {
    exclude(group = "javax.servlet", module = "javax.servlet-api")
    exclude(group = "javax.servlet", module = "servlet-api")
    exclude(group = "org.sonatype.sisu", module = "sisu-guice")
    exclude(group = "log4j", module = "log4j")
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "org.apache.logging.log4j")
    exclude(group = "org.jenkins-ci.main", module = "jenkins-test-harness")
    exclude(group = "org.powermock")
    exclude(group = "org.testng", module = "testng")
}

// Configure JUnit 5
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Create test JAR
val testsJar by tasks.registering(Jar::class) {
    archiveClassifier = "tests"
    from(sourceSets["test"].output)
}

// Publish configuration
publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        artifact(testsJar)
    }
}

// Compilation and Javadoc encoding
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// Create HPI (Jenkins Plugin Archive) task
tasks.register<Jar>("hpi") {
    group = "jenkins"
    description = "Build HPI plugin archive"
    archiveClassifier = "hpi"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(sourceSets["main"].output)
    from("src/main/resources")
    
    manifest {
        attributes(
            "Plugin-Class" to "io.jenkins.plugins.aliyunoss.AliyunOSSPublisher",
            "Short-Name" to "aliyun-oss",
            "Long-Name" to "Aliyun OSS Plugin",
            "Version" to project.version,
            "Jenkins-Version" to jenkinsVersion,
            "Plugin-Developers" to "Bruce.Wu"
        )
    }
    
    into("WEB-INF/lib") {
        from(configurations.runtimeClasspath)
    }
}

// Create distribution task
tasks.register<Copy>("dist") {
    group = "jenkins"
    description = "Build plugin distribution package"
    dependsOn("hpi")
    from(tasks.named<Jar>("hpi").map { it.archiveFile })
    into(layout.buildDirectory.dir("distributions"))
}

// Helper task to show plugin info
tasks.register("pluginInfo") {
    group = "jenkins"
    description = "Display plugin information"
    doLast {
        println("==========================================")
        println("Plugin: ${project.description}")
        println("==========================================")
        println("Group: ${project.group}")
        println("Version: ${project.version}")
        println("Jenkins Version: ${jenkinsVersion}")
        println("Java Version: ${java.sourceCompatibility}")
        println("==========================================")
    }
}
