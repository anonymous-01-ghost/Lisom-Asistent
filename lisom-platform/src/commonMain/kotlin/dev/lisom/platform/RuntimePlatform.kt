package dev.lisom.platform

enum class RuntimePlatform { ANDROID, JVM, IOS }

/** Identifies the runtime; microphone and lifecycle adapters are implemented separately. */
expect fun runtimePlatform(): RuntimePlatform
