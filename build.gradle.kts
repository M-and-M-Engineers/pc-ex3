plugins {
    application
    id("com.vaadin") version "23.0.1"
    id("org.gretty") version "3.0.6"
    war
}

repositories {
    mavenCentral()
//    maven { setUrl("https://maven.vaadin.com/vaadin-addons") }
}

vaadin {
    optimizeBundle = false
    productionMode = false
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
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

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("scm"){
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("scm.SmartCoffeeMachineService")
    args("10000", "scm")
}

tasks.register<JavaExec>("client"){
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("client.Application")
    args("10000")
}