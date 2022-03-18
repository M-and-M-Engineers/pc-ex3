plugins {
    java
    id("com.vaadin") version "23.0.1"
    id("org.gretty") version "3.0.6"
    war
}

repositories {
    mavenCentral()
}

vaadin {
    optimizeBundle = false
    productionMode = true
    pnpmEnable = true
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
    scanInterval = 100
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(enforcedPlatform("com.vaadin:vaadin-bom:23.0.1"))
    providedCompile("javax.servlet:javax.servlet-api:4.0.1")
    implementation("io.vertx:vertx-core:4.2.5")
    implementation("io.vertx:vertx-web:4.2.5")
    implementation("io.vertx:vertx-web-client:4.2.5")
    implementation("com.vaadin:vaadin-core:23.0.1")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    register<JavaExec>("scm"){
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("scm.SmartCoffeeMachineService")
    }

    register<JavaExec>("user"){
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("user.Application")
    }

}