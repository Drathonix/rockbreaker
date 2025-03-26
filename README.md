# Rock Breaker
Rock Breaker is the development core of all Drathonix/Vicious mods. You are welcome to use it yourself.

Rock Breaker uses [Stonecutter](https://stonecutter.kikugie.dev/stonecutter/introduction) for multiversioning and Architectury Loom for multi-modloader support. Architectury API is not required but is enabled by default. Rock breaker by default provides support for:

Fabric on 1.16.5, 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3.

Forge on 1.16.5, 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4.

Neoforge on 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3.

Expanding support is simple so don't worry if a version isn't listed.

I'm assuming you are using Intellij and understand how to use [Stonecutter](https://stonecutter.kikugie.dev/stonecutter/introduction)

# Project Setup
1. Create a new project, use the Java template, not the Minecraft template. You will be practically deleting everything the MC template generates.
   1. Set the project name to your modid. Keep it lowercase for your own sanity's sake.
   2. Ensure the build system is set to gradle and the Java version is 21.
   3. Set the gradle DSL to kotlin. All Rock Breaker scripts are kotlin and will not work with groovy.
   4. Make sure Add Sample Code is not checked.
   5. Open the advanced settings menu. 
      1. Set the gradle wrapper version to 8.9
      2. Set group ID to your group, and archives base name to your modid.
   6. Create the project and wait for initial setup.
# Rock Breaker Setup
Do not sync gradle until told to. You will just waste time if you do.

1. Copy all the files in this Repository into your project directory. Allow overwrites!
2. In settings.gradle.kts disable any versions you don't want active to start. (I have all except 1.20.4 turned off for convenience) Enabling more versions will make script loading take longer.
3. CTRL + SHIFT + R. Replace all instances of "rockbreaker" with your planned modid. Replace all instances of "Rock Breaker" with your mod display name. 3
4. Rename the Mixin files from mixins.<loader>.rockbreaker.json to mixins.<loader>.<your modid>.json. Rename the source root from your.group.rockbreaker to <your group>.<your modid>
6. CTRL + SHIFT + F. Search "TODO" Go through all the TODOs in gradle.properties, build.gradle.kts, stonecutter.gradle.kts, LICENSE, and settings.gradle.kts.
6. Sync gradle! Wait. This may take longer depending on how many versions you have enabled.
7. In the gradle menu, expand Tasks > stonecutter. Use the "Set active project to VERSION-MODLOADER" to swap between modloaders ONLY for now.
   1. CTRL + SHIFT + F "TODO", modify relevant modloader specific code for the current loader.
8. Once all the code is rid of Rock Breaker defaults set your active project to your preferred development version.

Finally, replace this README.md with your own and begin developing!