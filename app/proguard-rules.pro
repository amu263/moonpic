# Add project specific ProGuard rules here.

# Keep Compose runtime metadata
-keep class kotlin.Metadata { *; }

# Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class **_HiltModules$* { *; }
-keep class hilt_aggregated_deps.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep our @HiltAndroidApp / @AndroidEntryPoint application/activity classes
-keep class dev.moonpic.app.MoonPicApp { *; }
-keep class dev.moonpic.app.MainActivity { *; }
