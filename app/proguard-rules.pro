# Keep JNI bridge — native methods must not be renamed
-keep class com.lokai.app.data.inference.LlamaEngine {
    native <methods>;
}
-keep interface com.lokai.app.data.inference.TokenCallback {
    *;
}
