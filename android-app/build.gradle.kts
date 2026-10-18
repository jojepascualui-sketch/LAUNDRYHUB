plugins {
    id("com.android.application") version "9.4.1" apply false
}

// The repo lives under a non-ASCII path (OneDrive\<Korean folder>). On Windows, java.exe reads
// the @argfiles Gradle uses for test workers in the ANSI code page, so unit tests cannot load
// classes from such a path. In that case, build into an ASCII folder outside OneDrive.
if (rootDir.absolutePath.any { it.code > 127 }) {
    val outside = File(System.getProperty("user.home"), ".laundryhub-build/android-app")
    allprojects {
        layout.buildDirectory.set(File(outside, if (this == rootProject) "root" else name))
    }
}
