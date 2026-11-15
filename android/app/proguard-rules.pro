# Release shrinking rules. Libraries (Ktor, kotlinx.serialization, Room, Koin, Compose)
# ship their own consumer rules; only gaps are listed here.

# Glance pulls in WorkManager 2.7.1, whose InputMerger consumer rule keeps classes
# but not their constructors in R8 full mode. WorkManager creates these via
# Class.newInstance(); stripping the no-arg constructor leaves widgets loading forever.
-keep class * extends androidx.work.InputMerger {
    public <init>();
}

# Glance also instantiates widget action callbacks reflectively. Its consumer
# rule keeps callback classes, but their no-arg constructors must survive too.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Tink (used by androidx.security-crypto for the saved server passwords) references
# compile-only Error Prone annotations.
-dontwarn com.google.errorprone.annotations.**
