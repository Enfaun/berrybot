@file:Suppress("HardCodedStringLiteral")

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven("https://plugins.gradle.org/m2/")
    }

    // keep version here in sync when updating briar
    dependencies {
        classpath("ru.vyarus:gradle-animalsniffer-plugin:1.7.0")
        classpath(files("briar/libs/gradle-witness.jar"))
    }

    // keep version here in sync when updating briar
    extra.apply {
        set("kotlin_version", "1.8.20")
        set("dagger_version", "2.45")
        set("okhttp_version", "4.10.0")
        set("jackson_version", "2.13.4")
        set("tor_version", "0.4.7.13-2")
        set("obfs4proxy_version", "0.0.14-tor2")
        set("snowflake_version", "2.5.1")
        set("jsoup_version", "1.15.3")
        set("bouncy_castle_version", "1.71")
        set("junit_version", "4.13.2")
        set("jmock_version", "2.12.0")
        set("mockwebserver_version", "4.10.0")
        set("onionwrapper_version", "0.0.4")
    }
}


allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}

