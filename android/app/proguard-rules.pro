-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, AnnotationDefault
-keep class java.lang.reflect.** { *; }

# 2. PROTECT RETROFIT GENERICS
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 3. PROTECT GSON & DATA MODELS
-keep class com.google.gson.** { *; }
-keep class com.soulmatch.app.data.models.** { *; }
-keep public class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# 4. PROTECT YOUR API INTERFACES
-keep interface com.soulmatch.app.data.api.** { *; }
-keep class com.soulmatch.app.data.api.** { *; }

# 5. HILT & VIEWMODELS
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# 6. GENERAL SUPPRESSION
-dontwarn proguard.annotation.Keep
-dontwarn proguard.annotation.KeepClassMembers
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontnote retrofit2.Platform
