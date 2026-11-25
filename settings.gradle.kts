pluginManagement {
    repositories {
        maven {
            url = uri("https://dl.google.com/dl/android/maven2")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        google()
        maven { url = uri("https://repo1.maven.org/maven2") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        maven { url = uri("https://repo1.maven.org/maven2") }
        mavenLocal {
            content {
                includeGroup("com.xposed")
            }
        }
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "miplayfix"

include(":app")
