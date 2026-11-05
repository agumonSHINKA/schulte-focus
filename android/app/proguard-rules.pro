# Keep JS bridge
-keepclassmembers class io.github.agumonshinka.schulte.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
