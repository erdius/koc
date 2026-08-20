import org.gradle.api.tasks.Delete

plugins {
    id("com.android.application") version "8.2.0" apply false
    kotlin("android") version "1.9.10" apply false
    id("com.github.triplet.play") version "3.9.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
