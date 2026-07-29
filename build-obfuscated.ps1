# ====================================================================
#    SolarTools Double Obfuscation Build (ProGuard + Skidfuscator)
# ====================================================================

$ErrorActionPreference = "Stop"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "    SolarTools Double Obfuscation Build" -ForegroundColor Cyan
Write-Host "    (ProGuard CLI + Skidfuscator)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 0: Find Java & Maven ────────────────────────────────────────
Write-Host "[Môi trường] Đang quét tìm Java và Maven..." -ForegroundColor Yellow

$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaExe) {
    Write-Error "Không tìm thấy Java Runtime. Hãy cài đặt JDK 17+."
}

$jdkHome = Split-Path -Parent (Split-Path -Parent $javaExe)
$jmodsDir = Join-Path $jdkHome "jmods"
Write-Host "-> Java Runtime:  $javaExe" -ForegroundColor Gray
Write-Host "-> JDK Home:      $jdkHome" -ForegroundColor Gray
if (Test-Path $jmodsDir) {
    Write-Host "-> JMods Path:    $jmodsDir" -ForegroundColor Gray
} else {
    Write-Warning "Không tìm thấy thư mục jmods. ProGuard có thể báo lỗi thiếu Class."
}

# Check if default Java supports Java 21 compilation
$oldEAP = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$javaVersionOutput = & $javaExe -version 2>&1 | Out-String
$ErrorActionPreference = $oldEAP

$mavenJavaHome = $null
if ($javaVersionOutput -match 'version "(\d+)') {
    $verNum = [int]$Matches[1]
    if ($verNum -ge 21) {
        $mavenJavaHome = $jdkHome
    }
}

if (-not $mavenJavaHome) {
    # Scan in Program Files first (prioritizing standard JDK 21+)
    $progJdk = Get-ChildItem -Path "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -ErrorAction SilentlyContinue | Where-Object {
        $_.Name -like "*21*" -or $_.Name -like "*jdk-21*"
    } | Select-Object -First 1
    
    if (-not $progJdk) {
        # Fallback to JDK 17 in Program Files
        $progJdk = Get-ChildItem -Path "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -like "*17*" -or $_.Name -like "*jdk-17*"
        } | Select-Object -First 1
    }
    
    if ($progJdk) {
        $mavenJavaHome = $progJdk.FullName
        Write-Host "-> Phát hiện JDK tương thích tại Program Files: $mavenJavaHome" -ForegroundColor Gray
    } else {
        # Fallback search in .jdks folder (IntelliJ downloads)
        $suitJdk = Get-ChildItem -Path "C:\Users\vuhao\.jdks" -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -like "*21*" -or $_.Name -like "*25*" -or $_.Name -like "*loom*"
        } | Select-Object -First 1
        if ($suitJdk) {
            $mavenJavaHome = $suitJdk.FullName
            Write-Host "-> Phát hiện JDK 21/25+ tương thích tại .jdks: $mavenJavaHome" -ForegroundColor Gray
        }
    }
}

if ($mavenJavaHome) {
    $env:JAVA_HOME = $mavenJavaHome
    Write-Host "-> Đã cấu hình JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Gray
    $jdkJava = Join-Path $mavenJavaHome "bin\java.exe"
    if (Test-Path $jdkJava) {
        $javaExe = $jdkJava
        Write-Host "-> Sử dụng Java Runtime từ JDK: $javaExe" -ForegroundColor Gray
    }
} else {
    Write-Warning "Không tìm thấy JDK 21+ tương thích. Maven build có thể gặp lỗi target 21."
}

# Find Maven
$mvnCmd = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvnCmd) {
    # Try IntelliJ standard bundled Maven path
    $ideaMvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    if (Test-Path $ideaMvn) {
        $mvnCmd = $ideaMvn
    } else {
        # Fallback to general IntelliJ folder search (in case minor version differs)
        $ideaMvnSearch = Get-ChildItem -Path "C:\Program Files\JetBrains" -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
        if ($ideaMvnSearch) {
            $mvnCmd = $ideaMvnSearch
        } else {
            Write-Error "Không tìm thấy Maven (mvn). Vui lòng cài đặt Maven hoặc cấu hình PATH."
        }
    }
}
Write-Host "-> Maven:         $mvnCmd" -ForegroundColor Gray
Write-Host ""

# ── Paths ────────────────────────────────────────────────────────────
$projectRoot = $PSScriptRoot
if (-not $projectRoot) {
    $projectRoot = Get-Location
}

# Parse version and artifactId from pom.xml
[xml]$pom = Get-Content (Join-Path $projectRoot "pom.xml")
$version = $pom.project.version
$artifactId = $pom.project.artifactId

$shadedJar = Join-Path $projectRoot "target\$artifactId-$version.jar"
$obfDir = Join-Path $projectRoot "obfuscation"
$libsDir = Join-Path $obfDir "libs"
$outputDir = Join-Path $obfDir "output"
$proguardJar = Join-Path $libsDir "proguard.jar"
$skidfuscatorJar = Join-Path $libsDir "skidfuscator.jar"

$proguardOut = Join-Path $outputDir "$artifactId-proguard.jar"
$mappingFile = Join-Path $outputDir "mapping.txt"
$finalOutput = Join-Path $outputDir "$artifactId-$version.jar"
$m2 = Join-Path $env:USERPROFILE ".m2\repository"

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
New-Item -ItemType Directory -Force -Path $libsDir | Out-Null
Remove-Item -Path $proguardOut -Force -ErrorAction SilentlyContinue
Remove-Item -Path $finalOutput -Force -ErrorAction SilentlyContinue

# ── Pre-flight: dependency gathering ──────────────────────────────────
Write-Host "[Pre-flight] Đang đồng bộ các thư viện classpath..." -ForegroundColor Yellow

function Get-MavenLib {
    param(
        [string]$GroupId,
        [string]$ArtifactId,
        [string]$Version,
        [string]$DestName,
        [string]$RepoUrl = $null
    )
    $groupPath = $GroupId -replace '\.', '\'
    $libDir = Join-Path $m2 (Join-Path $groupPath (Join-Path $ArtifactId $Version))
    $destPath = Join-Path $libsDir $DestName
    
    if (Test-Path $destPath) { return }
    
    # Try finding in m2 cache
    $found = Get-ChildItem -Path $libDir -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    
    if (-not $found) {
        Write-Host "  -> Tải thư viện ${GroupId}:${ArtifactId}:${Version} ..." -ForegroundColor DarkGray
        $artStr = "${GroupId}:${ArtifactId}:${Version}"
        $cmdArgs = @("dependency:get", "-Dartifact=$artStr")
        if ($RepoUrl) {
            $cmdArgs += "-DremoteRepositories=$RepoUrl"
        }
        $cmdArgs += "-q"
        Start-Process -FilePath $mvnCmd -ArgumentList $cmdArgs -NoNewWindow -Wait
        
        $found = Get-ChildItem -Path $libDir -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    }
    
    if ($found) {
        Write-Host "  -> Copy: $DestName" -ForegroundColor Gray
        Copy-Item -Path $found.FullName -Destination $destPath -Force
    } else {
        Write-Warning "  -> Không thể resolve thư viện: ${GroupId}:${ArtifactId}:${Version}"
    }
}

Get-MavenLib -GroupId "io.papermc.paper" -ArtifactId "paper-api" -Version "1.21-R0.1-SNAPSHOT" -DestName "paper-api.jar" -RepoUrl "https://repo.papermc.io/repository/maven-public/"
Get-MavenLib -GroupId "com.sk89q.worldguard" -ArtifactId "worldguard-bukkit" -Version "7.0.10" -DestName "worldguard-bukkit.jar" -RepoUrl "https://maven.enginehub.org/repo/"
Get-MavenLib -GroupId "com.sk89q.worldedit" -ArtifactId "worldedit-bukkit" -Version "7.3.6" -DestName "worldedit-bukkit.jar" -RepoUrl "https://maven.enginehub.org/repo/"

Get-MavenLib -GroupId "com.zaxxer" -ArtifactId "HikariCP" -Version "5.1.0" -DestName "HikariCP-5.1.0.jar"
Get-MavenLib -GroupId "org.xerial" -ArtifactId "sqlite-jdbc" -Version "3.45.1.0" -DestName "sqlite-jdbc-3.45.1.0.jar"
Get-MavenLib -GroupId "org.slf4j" -ArtifactId "slf4j-api" -Version "2.0.9" -DestName "slf4j-api-2.0.9.jar"

# Download spigot-api for Skidfuscator libs (if required)
Get-MavenLib -GroupId "org.spigotmc" -ArtifactId "spigot-api" -Version "1.21.4-R0.1-SNAPSHOT" -DestName "spigot-api.jar" -RepoUrl "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"

Write-Host ""

# ── Step 1: Maven Build ──────────────────────────────────────────────
Write-Host "[Step 1/3] Maven: compile & package..." -ForegroundColor Yellow
Write-Host "-------------------------------------------" -ForegroundColor Gray

# Run Maven Clean Package
$mvnArgs = @("clean", "package", "-DskipTests")
Start-Process -FilePath $mvnCmd -ArgumentList $mvnArgs -WorkingDirectory $projectRoot -NoNewWindow -Wait

if (-not (Test-Path $shadedJar)) {
    Write-Error "Maven build thất bại. Không tìm thấy Jar tại: $shadedJar"
}

$shadedSize = (Get-Item $shadedJar).Length / 1KB
Write-Host "-> Maven build thành công: $shadedJar ($([Math]::Round($shadedSize, 2)) KB)" -ForegroundColor Green
Write-Host ""

# ── Step 2: ProGuard Obfuscation ─────────────────────────────────────
Write-Host "[Step 2/3] ProGuard: class renaming..." -ForegroundColor Yellow
Write-Host "-------------------------------------------" -ForegroundColor Gray

$proguardPro = Join-Path $obfDir "proguard.pro"
$dictFile = Join-Path $obfDir "proguard-dict.txt"

if (-not (Test-Path $dictFile)) {
    Write-Error "Không tìm thấy proguard-dict.txt tại $dictFile"
}

# Update dictionary path in proguard.pro
$proContent = Get-Content $proguardPro -Raw
$proContent = $proContent -replace "-obfuscationdictionary\s+\S+", "-obfuscationdictionary `"$($dictFile -replace '\\', '/')`""
$proContent = $proContent -replace "-classobfuscationdictionary\s+\S+", "-classobfuscationdictionary `"$($dictFile -replace '\\', '/')`""
$proContent = $proContent -replace "-packageobfuscationdictionary\s+\S+", "-packageobfuscationdictionary `"$($dictFile -replace '\\', '/')`""
Set-Content $proguardPro $proContent -NoNewline

# Collect ProGuard libraries
$libArgs = @()
Get-ChildItem -Path $libsDir -Filter "*.jar" | ForEach-Object {
    if ($_.Name -ne "proguard.jar" -and $_.Name -ne "skidfuscator.jar" -and $_.Name -ne "spigot-api.jar") {
        $libArgs += "-libraryjars"
        $libArgs += "`"$($_.FullName)`""
    }
}

# Add standard JDK JMods
if (Test-Path $jmodsDir) {
    @("java.base.jmod", "java.logging.jmod", "java.sql.jmod", "java.desktop.jmod") | ForEach-Object {
        $jmodPath = Join-Path $jmodsDir $_
        if (Test-Path $jmodPath) {
            $libArgs += "-libraryjars"
            $libArgs += "`"$jmodPath`""
        }
    }
}

$proguardArgs = @(
    "-jar", "`"$proguardJar`"",
    "-injars", "`"$shadedJar`"",
    "-outjars", "`"$proguardOut`"",
    "-include", "`"$proguardPro`"",
    "-printmapping", "`"$mappingFile`""
)
$proguardArgs += $libArgs

# Run ProGuard
Start-Process -FilePath $javaExe -ArgumentList $proguardArgs -NoNewWindow -Wait

if (-not (Test-Path $proguardOut)) {
    Write-Error "ProGuard obfuscation thất bại."
}

$proguardSize = (Get-Item $proguardOut).Length / 1KB
Write-Host "-> ProGuard hoàn tất: $proguardOut ($([Math]::Round($proguardSize, 2)) KB)" -ForegroundColor Green

# Patch plugin.yml main class in obfuscated jar
Write-Host "-> Đang vá plugin.yml với class main đã mã hóa..." -ForegroundColor Gray
$originalMain = "com.omhvn.tools.SolarTool"
if (Test-Path $mappingFile) {
    # Find mappings line
    $mappingLines = Get-Content $mappingFile
    $matchedLine = $mappingLines | Where-Object { $_ -like "$originalMain -> *" }
    if ($matchedLine) {
        $obfuscatedMain = (($matchedLine -split ' -> ')[1] -replace ':$', '').Trim()
        
        Write-Host "   Original Main:   $originalMain" -ForegroundColor Gray
        Write-Host "   Obfuscated Main: $obfuscatedMain" -ForegroundColor Gray
        
        # Temp dir to unpack/repack plugin.yml
        $workDir = Join-Path $env:TEMP ([Guid]::NewGuid().ToString())
        New-Item -ItemType Directory -Path $workDir | Out-Null
        
        # Extract plugin.yml
        Start-Process -FilePath "jar" -ArgumentList "xf `"$proguardOut`" plugin.yml" -WorkingDirectory $workDir -NoNewWindow -Wait
        
        $pluginYml = Join-Path $workDir "plugin.yml"
        if (Test-Path $pluginYml) {
            $ymlContent = Get-Content $pluginYml -Raw
            $ymlContent = $ymlContent -replace "main:\s+\S+", "main: $obfuscatedMain"
            Set-Content $pluginYml $ymlContent -NoNewline
            
            # Repack plugin.yml
            Start-Process -FilePath "jar" -ArgumentList "uf `"$proguardOut`" plugin.yml" -WorkingDirectory $workDir -NoNewWindow -Wait
            Write-Host "   ✓ plugin.yml đã vá thành công!" -ForegroundColor Green
        } else {
            Write-Warning "   Không tìm thấy plugin.yml khi trích xuất!"
        }
        Remove-Item -Path $workDir -Recurse -Force
    } else {
        Write-Warning "   Không tìm thấy mapping cho main class '$originalMain'"
    }
} else {
    Write-Warning "   Không tìm thấy mapping.txt. Bỏ qua vá plugin.yml"
}
Write-Host ""

# ── Step 3: Skidfuscator ─────────────────────────────────────────────
Write-Host "[Step 3/3] Skidfuscator: flow & string obfuscation..." -ForegroundColor Yellow
Write-Host "-------------------------------------------" -ForegroundColor Gray

$configFile = Join-Path $obfDir "skidfuscator-config.conf"

$skidArgs = @(
    "-Xmx4G",
    "-Djava.version=21",
    "-jar", "`"$skidfuscatorJar`"",
    "obfuscate",
    "`"$proguardOut`"",
    "-o", "`"$finalOutput`"",
    "-cfg", "`"$configFile`"",
    "-li", "`"$libsDir`""
)

# Run Skidfuscator
Start-Process -FilePath $javaExe -ArgumentList $skidArgs -WorkingDirectory $obfDir -NoNewWindow -Wait

# Renaming fallback
if (-not (Test-Path $finalOutput)) {
    $skidOutput = Get-ChildItem -Path $outputDir -Filter "*.jar" | Where-Object { $_.LastWriteTime -gt (Get-Item $proguardOut).LastWriteTime -and $_.Name -notlike "*proguard*" } | Select-Object -First 1
    if ($skidOutput) {
        Move-Item -Path $skidOutput.FullName -Destination $finalOutput -Force
    } else {
        Write-Error "Skidfuscator build thất bại."
    }
}

$finalSize = (Get-Item $finalOutput).Length / 1KB
Write-Host "-> Skidfuscator hoàn tất: $finalOutput ($([Math]::Round($finalSize, 2)) KB)" -ForegroundColor Green
Write-Host ""

# ── Summary ──────────────────────────────────────────────────────────
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  Tóm tắt dung lượng Jar:" -ForegroundColor Cyan
Write-Host "  - Sau Maven shade:        $([Math]::Round($shadedSize, 2)) KB" -ForegroundColor Gray
Write-Host "  - Sau ProGuard:           $([Math]::Round($proguardSize, 2)) KB" -ForegroundColor Gray
Write-Host "  - Thành phẩm (Skid):      $([Math]::Round($finalSize, 2)) KB" -ForegroundColor Green
Write-Host ""
Write-Host "  📦 Đường dẫn đầu ra:      $finalOutput" -ForegroundColor Green
Write-Host "  ✅ ĐÃ HOÀN THÀNH BUILD MÃ HÓA!" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Cyan
