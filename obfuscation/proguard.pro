-dontshrink
-dontoptimize
-useuniqueclassmembernames
-allowaccessmodification
-repackageclasses decodenodecode.solartools.nothingtoseehere.stoptryingtofigurethisout.youarewastingyourtime.seriouslythereisnothinghidden.keepgoingforward.dontlookback.justignorethissegment.anothermeaninglesspartadded

-keeppackagenames org.bukkit.**
-keeppackagenames io.papermc.**
-keeppackagenames net.md_5.**
-keeppackagenames com.zaxxer.**
-keeppackagenames com.mysql.**
-keeppackagenames org.xerial.**
-keeppackagenames org.slf4j.**
-keeppackagenames com.sk89q.worldguard.**
-keeppackagenames com.sk89q.worldedit.**
-keeppackagenames com.omhvn.tools.bstats.**

-keep class com.omhvn.tools.bstats.** { *; }
-dontwarn com.omhvn.tools.bstats.**


-obfuscationdictionary "C:/Users/vuhao/OneDrive/OmhVN/SolarTools/obfuscation/proguard-dict.txt"
-classobfuscationdictionary "C:/Users/vuhao/OneDrive/OmhVN/SolarTools/obfuscation/proguard-dict.txt"
-packageobfuscationdictionary "C:/Users/vuhao/OneDrive/OmhVN/SolarTools/obfuscation/proguard-dict.txt"

-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,StackMapTable

-dontwarn **
-dontnote **

# Keep the constructor of JavaPlugin subclasses but allow the class name to be obfuscated
-keepclassmembers public class * extends org.bukkit.plugin.java.JavaPlugin {
    public <init>();
}

# Keep CommandExecutors (except the main class)
-keep class !com.omhvn.tools.SolarTool,* implements org.bukkit.command.CommandExecutor {
    public boolean onCommand(...);
}

# Keep TabCompleters (except the main class)
-keep class !com.omhvn.tools.SolarTool,* implements org.bukkit.command.TabCompleter {
    public java.util.List onTabComplete(...);
}

# Keep Event Listeners
-keep class * implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler public void *(...);
}

# Keep serializable class members
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}