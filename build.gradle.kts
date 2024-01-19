plugins {
  id("dev.clojurephant.clojure")
  id("java-library")
  id("org.openjfx.javafxplugin")
}

repositories {
  mavenCentral()
  maven {
    name = "Clojars"
    url = uri("https://repo.clojars.org")
  }
}

javafx {
  version = "21.0.2"
  modules = listOf("javafx.base", "javafx.controls", "javafx.graphics", "javafx.media", "javafx.web")
}

dependencies {
  implementation("org.clojure:clojure:1.11.1")
  implementation("org.clojure:core.async:1.6.681")
  implementation("org.clojure:core.cache:1.0.225")
  implementation("com.stuartsierra:component:1.1.0")
  implementation("cljfx:cljfx:1.8.0")
    implementation("cljfx:cljfx:1.8.0:jdk11")
  implementation("parinferish:parinferish:0.8.0")
  implementation("rewrite-clj:rewrite-clj:1.1.47")

  testImplementation("org.clojure:test.check:1.1.1")

  devImplementation("org.clojure:tools.namespace:1.4.5")
}

val main by sourceSets.getting

tasks.register<JavaExec>("run") {
  classpath(main.runtimeClasspath)
  mainClass = "clojure.main"
  args("-m", "org.ajoberstar.derl.ui")
}
