pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}
rootProject.name = "Horizon"
include(":app")
