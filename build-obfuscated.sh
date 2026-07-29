#!/bin/bash
set -e

echo "=============================================="
echo "    SolarTools Double Obfuscation Build"
echo "    (ProGuard CLI + Skidfuscator)"
echo "=============================================="
echo ""

# ── Step 0: Find Java Compiler & Maven ─────────────────────────────────────
echo "[Môi trường] Đang quét tìm Java compiler và Maven..."

REAL_JAVAC=""
SKID_JAVA=""

# Prioritize JDK 17/21 for Skidfuscator
for jdk in \
    /usr/lib/jvm/java-21-openjdk-amd64/bin \
    /usr/lib/jvm/java-17-openjdk-amd64/bin \
    /usr/lib/jvm/java-21-openjdk/bin \
    /usr/lib/jvm/java-17-openjdk/bin \
    /usr/local/lib/jvm/java-21/bin; do
    if [ -x "$jdk/java" ]; then
        SKID_JAVA="$jdk/java"
        break
    fi
done

# Find javac to compile (any version)
if command -v javac >/dev/null 2>&1; then
    REAL_JAVAC=$(command -v javac)
fi

if [ -z "$REAL_JAVAC" ]; then
    for cmd in /usr/bin/javac /usr/local/bin/javac /home/runner/.nix-profile/bin/javac /run/current-system/sw/bin/javac; do
        if [ -x "$cmd" ]; then
            REAL_JAVAC="$cmd"
            break
        fi
    done
fi

if [ -z "$REAL_JAVAC" ]; then
    echo "ERROR: Không tìm thấy Java compiler (javac) trên hệ thống!"
    exit 1
fi

REAL_JAVA="$(dirname "$REAL_JAVAC")/java"

if [ -z "$SKID_JAVA" ]; then
    SKID_JAVA="$REAL_JAVA"
fi

echo "-> Compiler (Maven):       $REAL_JAVAC"
echo "-> Runtime  (ProGuard):    $REAL_JAVA"
echo "-> Runtime  (Skidfuscator): $SKID_JAVA"

# Resolve JDK Home and JMods
REAL_JAVAC_RESOLVED=$(readlink -f "$REAL_JAVAC" 2>/dev/null || realpath "$REAL_JAVAC" 2>/dev/null || echo "$REAL_JAVAC")
JDK_HOME="$(dirname "$(dirname "$REAL_JAVAC_RESOLVED")")"
NIX_JMOD_DIR=""

if [ -d "$JDK_HOME/jmods" ]; then
    NIX_JMOD_DIR="$JDK_HOME/jmods"
elif [ -d "$JDK_HOME/lib/modules" ]; then
    NIX_JMOD_DIR="$JDK_HOME/lib/modules"
elif [ -d "/mnt/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot/jmods" ]; then
    NIX_JMOD_DIR="/mnt/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot/jmods"
fi

if [ -n "$NIX_JMOD_DIR" ]; then
    echo "-> Thư mục JMods: $NIX_JMOD_DIR"
fi

unset JAVA_HOME

# Paths
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION=$(grep -m 1 "<version>" "$PROJECT_ROOT/pom.xml" | sed -E 's/<\/?version>//g' | xargs)
ARTIFACT_ID=$(grep -m 1 "<artifactId>" "$PROJECT_ROOT/pom.xml" | sed -E 's/<\/?artifactId>//g' | xargs)

SHADED_JAR="$PROJECT_ROOT/target/$ARTIFACT_ID-$VERSION.jar"
PROGUARD_JAR="$PROJECT_ROOT/obfuscation/libs/proguard.jar"
SKIDFUSCATOR_JAR="$PROJECT_ROOT/obfuscation/libs/skidfuscator.jar"
LIBS_DIR="$PROJECT_ROOT/obfuscation/libs"
OUTPUT_DIR="$PROJECT_ROOT/obfuscation/output"
PROGUARD_OUT="$OUTPUT_DIR/$ARTIFACT_ID-proguard.jar"
MAPPING_FILE="$OUTPUT_DIR/mapping.txt"
FINAL_OUTPUT="$OUTPUT_DIR/$ARTIFACT_ID-$VERSION.jar"
M2="$HOME/.m2/repository"

mkdir -p "$OUTPUT_DIR"
mkdir -p "$LIBS_DIR"
rm -f "$PROGUARD_OUT"
rm -f "$FINAL_OUTPUT"

# ── Pre-flight: copy/download libs for obfuscation ───────────────────────────
add_skid_lib() {
    local src="$1"
    local dest="$2"
    local label="$3"
    if [ -f "$LIBS_DIR/$dest" ]; then
        return 0
    fi
    if [ -f "$src" ]; then
        echo "[Pre-flight] Copying $label -> obfuscation/libs/$dest"
        cp "$src" "$LIBS_DIR/$dest"
    else
        echo "WARN: $label not found at $src. Downloading..."
        # Try downloading via maven
        mvn dependency:get -Dartifact="$4" -q || true
        # Search in local repo again
        local local_file=$(find "$M2" -name "$dest" | head -1)
        if [ -n "$local_file" ] && [ -f "$local_file" ]; then
            cp "$local_file" "$LIBS_DIR/$dest"
        else
            echo "WARN: Could not resolve $label automatically."
        fi
    fi
}

add_skid_lib "$M2/io/papermc/paper/paper-api/1.21-R0.1-SNAPSHOT/paper-api-1.21-R0.1-SNAPSHOT.jar" \
             "paper-api.jar" "paper-api 1.21" "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"

add_skid_lib "$M2/com/sk89q/worldguard/worldguard-bukkit/7.0.10/worldguard-bukkit-7.0.10.jar" \
             "worldguard-bukkit.jar" "WorldGuard 7.0.10" "com.sk89q.worldguard:worldguard-bukkit:7.0.10"

add_skid_lib "$M2/com/sk89q/worldedit/worldedit-bukkit/7.3.6/worldedit-bukkit-7.3.6.jar" \
             "worldedit-bukkit.jar" "WorldEdit 7.3.6" "com.sk89q.worldedit:worldedit-bukkit:7.3.6"



add_skid_lib "$M2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar" \
             "HikariCP-5.1.0.jar" "HikariCP 5.1.0" "com.zaxxer:HikariCP:5.1.0"

add_skid_lib "$M2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar" \
             "sqlite-jdbc-3.45.1.0.jar" "sqlite-jdbc 3.45.1.0" "org.xerial:sqlite-jdbc:3.45.1.0"

add_skid_lib "$M2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar" \
             "slf4j-api-2.0.9.jar" "slf4j-api 2.0.9" "org.slf4j:slf4j-api:2.0.9"

add_skid_lib "$M2/org/spigotmc/spigot-api/1.21.4-R0.1-SNAPSHOT/spigot-api-1.21.4-R0.1-SNAPSHOT.jar" \
             "spigot-api.jar" "spigot-api 1.21.4" "org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT"

echo ""

# ── Step 1: Maven compilation ──────────────────────────────────────────
echo "[Step 1/3] Maven: compile + package..."
echo "-------------------------------------------"

mvn clean package -DskipTests -q

if [ ! -f "$SHADED_JAR" ]; then
    echo "ERROR: Maven build failed. JAR not found: $SHADED_JAR"
    exit 1
fi
SHADED_SIZE=$(ls -lh "$SHADED_JAR" | awk '{print $5}')
echo "Maven complete: $SHADED_JAR ($SHADED_SIZE)"
echo ""

# ── Step 2: ProGuard CLI ──────────────────────────────────────────────────────
echo "[Step 2/3] ProGuard: rename obfuscation..."
echo "-------------------------------------------"

DICT_FILE="$PROJECT_ROOT/obfuscation/proguard-dict.txt"
PROGUARD_PRO="$PROJECT_ROOT/obfuscation/proguard.pro"

if [ ! -f "$DICT_FILE" ]; then
    echo "ERROR: proguard-dict.txt không tìm thấy tại $DICT_FILE"
    exit 1
fi

# Patch dictionary paths inside proguard.pro
sed -i "s|-obfuscationdictionary .*|-obfuscationdictionary \"$DICT_FILE\"|g"               "$PROGUARD_PRO"
sed -i "s|-classobfuscationdictionary .*|-classobfuscationdictionary \"$DICT_FILE\"|g"     "$PROGUARD_PRO"
sed -i "s|-packageobfuscationdictionary .*|-packageobfuscationdictionary \"$DICT_FILE\"|g" "$PROGUARD_PRO"

LIB_ARGS=""
add_lib() {
    local jar="$1"
    if [ -f "$jar" ]; then
        LIB_ARGS="$LIB_ARGS -libraryjars '$jar'"
    fi
}

# Add all jars from libs/ (except proguard and skidfuscator themselves)
for jar in "$LIBS_DIR"/*.jar; do
    name=$(basename "$jar")
    if [ "$name" != "proguard.jar" ] && [ "$name" != "skidfuscator.jar" ] && [ "$name" != "spigot-api.jar" ]; then
        add_lib "$jar"
    fi
done

if [ -n "$NIX_JMOD_DIR" ] && [ -d "$NIX_JMOD_DIR" ]; then
    add_lib "$NIX_JMOD_DIR/java.base.jmod"
    add_lib "$NIX_JMOD_DIR/java.logging.jmod"
    add_lib "$NIX_JMOD_DIR/java.sql.jmod"
    add_lib "$NIX_JMOD_DIR/java.desktop.jmod"
fi

eval "$REAL_JAVA" -jar "'$PROGUARD_JAR'" \
    -injars  "'$SHADED_JAR'" \
    -outjars "'$PROGUARD_OUT'" \
    -include "'$PROGUARD_PRO'" \
    -printmapping "'$MAPPING_FILE'" \
    $LIB_ARGS

if [ ! -f "$PROGUARD_OUT" ]; then
    echo "ERROR: ProGuard failed."
    exit 1
fi
PROGUARD_SIZE=$(ls -lh "$PROGUARD_OUT" | awk '{print $5}')
echo "ProGuard complete: $PROGUARD_OUT ($PROGUARD_SIZE)"

# Patch plugin.yml main class path
echo "Patching plugin.yml main class..."
ORIGINAL_MAIN="com.omhvn.tools.SolarTool"

if [ -f "$MAPPING_FILE" ]; then
    OBFUSCATED_MAIN=$(grep "^${ORIGINAL_MAIN} -> " "$MAPPING_FILE" | awk '{print $3}' | tr -d ':')
    if [ -n "$OBFUSCATED_MAIN" ]; then
        echo "  Original  : $ORIGINAL_MAIN"
        echo "  Obfuscated: $OBFUSCATED_MAIN"
        WORK_DIR="$(mktemp -d)"
        cd "$WORK_DIR"
        jar xf "$PROGUARD_OUT" plugin.yml
        sed -i "s|^main: .*|main: $OBFUSCATED_MAIN|" plugin.yml
        jar uf "$PROGUARD_OUT" plugin.yml
        cd "$PROJECT_ROOT"
        rm -rf "$WORK_DIR"
        echo "  plugin.yml patched OK"
    else
        echo "  WARN: No mapping found for '$ORIGINAL_MAIN'"
    fi
else
    echo "  WARN: mapping.txt không tồn tại"
fi
echo ""

# ── Step 3: Skidfuscator ───────────────────────────────────
echo "[Step 3/3] Skidfuscator: string/flow obfuscation..."
echo "-------------------------------------------"

CONFIG_FILE="$PROJECT_ROOT/obfuscation/skidfuscator-config.conf"
SKID_WORK_DIR="$PROJECT_ROOT/obfuscation"

cd "$SKID_WORK_DIR"
"$SKID_JAVA" -Xmx4G -jar "$SKIDFUSCATOR_JAR" obfuscate \
    "$PROGUARD_OUT" \
    -o "$FINAL_OUTPUT" \
    -cfg "$CONFIG_FILE" \
    -li "$LIBS_DIR"
cd "$PROJECT_ROOT"

if [ ! -f "$FINAL_OUTPUT" ]; then
    SKID_OUTPUT=$(find "$OUTPUT_DIR" -name "*.jar" -newer "$PROGUARD_OUT" 2>/dev/null | grep -v "proguard" | head -1)
    if [ -n "$SKID_OUTPUT" ]; then
        mv "$SKID_OUTPUT" "$FINAL_OUTPUT"
        echo "Renamed: $SKID_OUTPUT -> $FINAL_OUTPUT"
    else
        echo "ERROR: Skidfuscator failed."
        exit 1
    fi
fi

FINAL_SIZE=$(ls -lh "$FINAL_OUTPUT" | awk '{print $5}')

# Summary
echo ""
echo "=============================================="
printf "  %-28s %s\n" "After Maven shade:"     "$SHADED_SIZE"
printf "  %-28s %s\n" "After ProGuard:"        "$PROGUARD_SIZE"
printf "  %-28s %s\n" "Final (Skidfuscator):"  "$FINAL_SIZE"
echo ""
echo "  Output: $FINAL_OUTPUT"
echo ""
echo "  Build complete!"
