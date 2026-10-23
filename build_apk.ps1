# Orion's Journey - Production APK Build Pipeline
# Guarantees 100% POSIX forward-slash ZIP entry paths for Android AssetManager compatibility
$ErrorActionPreference = "Stop"
$base = "D:\CCTV\Projects\mobile\pufflenew"
$main = "$base\app\src\main"
$build = "$base\build_apk"
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$build\res_compiled", "$build\gen", "$build\classes", "$build\dex" | Out-Null

$sdk = "C:\Users\cktv\android-sdk"
$bt = "$sdk\build-tools\34.0.0"
$androidJar = "$sdk\platforms\android-34\android.jar"
$jdkBin = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$jdkBin;$env:PATH"

Write-Output "1. Compiling resources with aapt2..."
& "$bt\aapt2.exe" compile --dir "$main\res" -o "$build\res_compiled\resources.zip"

Write-Output "2. Linking resources with aapt2..."
& "$bt\aapt2.exe" link `
    -I $androidJar `
    --manifest "$main\AndroidManifest.xml" `
    --java "$build\gen" `
    --min-sdk-version 26 `
    --target-sdk-version 34 `
    --version-code 8 `
    --version-name "2.5.0-Orion" `
    -o "$build\unaligned.apk" `
    "$build\res_compiled\resources.zip"

Write-Output "3. Compiling Java source files with javac..."
$javaFiles = Get-ChildItem -Path "$main\java", "$build\gen" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& "$jdkBin\javac.exe" -source 1.8 -target 1.8 -bootclasspath $androidJar -encoding UTF-8 -d "$build\classes" $javaFiles

Write-Output "4. Converting classes to DEX with d8..."
& "$jdkBin\jar.exe" cf "$build\classes.jar" -C "$build\classes" .
& "$bt\d8.bat" --lib $androidJar --min-api 26 --output "$build\dex" "$build\classes.jar"

Write-Output "5. Packaging classes.dex and assets/ (POSIX forward-slashes) with jar..."
& "$jdkBin\jar.exe" uf "$build\unaligned.apk" -C "$build\dex" classes.dex
& "$jdkBin\jar.exe" uf0 "$build\unaligned.apk" -C "$main" assets

Write-Output "6. Aligning APK with zipalign (4-byte boundary)..."
$alignedApk = "$build\aligned.apk"
& "$bt\zipalign.exe" -p -f 4 "$build\unaligned.apk" $alignedApk

Write-Output "7. Signing APK (v1+v2+v3) with Orion keystore..."
$ks = "$base\orion_release.jks"
$finalApk = "$base\OrionsJourney_S25Ultra_Android17.apk"
& "$bt\apksigner.bat" sign `
    --ks $ks `
    --ks-key-alias orion_key `
    --ks-pass pass:orion2026 `
    --key-pass pass:orion2026 `
    --v1-signing-enabled true `
    --v2-signing-enabled true `
    --v3-signing-enabled true `
    --out $finalApk `
    $alignedApk

Write-Output "8. Verifying signed APK..."
& "$bt\apksigner.bat" verify --verbose $finalApk

$entries = & "$bt\aapt.exe" list $finalApk
$backslashCount = ($entries | Select-String "\\").Count
$assetCount = ($entries | Select-String "^assets/").Count
Write-Output "Verification -> Backslashes: $backslashCount, Assets: $assetCount"
Remove-Item $build -Recurse -Force -ErrorAction SilentlyContinue
Write-Output ("SUCCESS! APK is ready: {0:N2} MB at {1}" -f ((Get-Item $finalApk).Length / 1MB), $finalApk)
