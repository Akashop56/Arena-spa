# RONIN AI — ProGuard / R8 rules

# Gson: keep the model types it (de)serializes reflectively
-keep class com.ronin.ai.core.domain.model.** { *; }
-keep class com.ronin.ai.core.data.db.entity.** { *; }
-keep class com.ronin.ai.core.data.datastore.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
