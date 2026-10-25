$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$buildTools = Join-Path $sdk 'build-tools\36.0.0'
$androidJar = Join-Path $sdk 'platforms\android-34\android.jar'
$out = Join-Path $root 'build'
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Path $out | Out-Null

& (Join-Path $buildTools 'aapt2.exe') compile --dir (Join-Path $root 'res') -o (Join-Path $out 'compiled.zip')
& (Join-Path $buildTools 'aapt2.exe') link `
    -o (Join-Path $out 'unaligned.apk') `
    -I $androidJar `
    --manifest (Join-Path $root 'AndroidManifest.xml') `
    --min-sdk-version 21 `
    --target-sdk-version 28 `
    --java (Join-Path $out 'gen') `
    (Join-Path $out 'compiled.zip')

$classes = Join-Path $out 'classes'
New-Item -ItemType Directory -Path $classes | Out-Null
$sources = @(Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem (Join-Path $out 'gen') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 8 -encoding UTF-8 -classpath $androidJar -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$classFiles = @(Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object FullName)
& (Join-Path $buildTools 'd8.bat') --min-api 21 --lib $androidJar --output $out $classFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$unsigned = Join-Path $out 'unsigned.apk'
Copy-Item (Join-Path $out 'unaligned.apk') $unsigned
Push-Location $out
try {
    & jar uf unsigned.apk classes.dex
} finally {
    Pop-Location
}

$aligned = Join-Path $out 'aligned.apk'
& (Join-Path $buildTools 'zipalign.exe') -f 4 $unsigned $aligned

$keystore = Join-Path $root 'debug.keystore'
if (-not (Test-Path $keystore)) {
    & keytool -genkeypair -keystore $keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'
}
& (Join-Path $buildTools 'apksigner.bat') sign --ks $keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --v1-signing-enabled true --v2-signing-enabled true --out (Join-Path $out 'LightHome.apk') $aligned
& (Join-Path $buildTools 'apksigner.bat') verify (Join-Path $out 'LightHome.apk')
Write-Host "APK $(Join-Path $out 'LightHome.apk')"
