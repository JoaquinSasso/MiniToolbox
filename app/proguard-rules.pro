# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*

# Mantener metadata de genéricos y anotaciones
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, EnclosingMethod, InnerClasses

# protobuf-java (versión completa) resuelve los accesores de los mensajes
# generados por reflexión, así que el código generado no puede ofuscarse.
# El resto de data/ no usa reflexión: @Serializable trae sus propias reglas
# en kotlinx-serialization y lo demás se serializa a mano con org.json.
-keep class com.joasasso.minitoolbox.data.CountryOuterClass { *; }
-keep class com.joasasso.minitoolbox.data.CountryOuterClass$* { *; }

