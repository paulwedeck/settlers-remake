# JSettlers

This project intends to create a remake of the famous strategy game "The Settlers 3" published by Blue Byte in 1998. The project is developed in Java and runs on PC (Windows/Linux/macOS) and Android.

On macOS (both Apple Silicon and Intel) the game renders through Vulkan via [MoltenVK](https://github.com/KhronosGroup/MoltenVK); see the [macOS](#macos) section below for installation details.

### Warning: Alpha Status
The game is currently in an **alpha** status! Therefore bugs, frequent changes making saved games invalid and server abortions need to be expected. Nevertheless we will try to minimize trouble.

### Found a Bug? Report it!
If you experience troubles / find a bug, help us fix it.
Please either report your bug as an issue or post it on the JSettlers Discord in the #support channel.

The JSettlers game creates log files of your games that are essential for debugging.
Therefore, always include the following information in a bug report:
1. What OS and JRE are you using? If it is Android, please state your Android version.
2. Revision of your build:
  1. In the PC version this can be found in the head of the window as "JSettlers - commit: XXXXXXX".
  2. In the Android version, the info is displayed on the start screen in the lower right corner as "build: XXXXXXX".
3. In the game's folder, there is a subfolder ```resources/logs/``` containing a folder of log files for every game you played. Please package the folder belonging to your game where you experienced the bug (identifiable by date and map name). This folder contains the following files:
  1. *_out.log: This is the console / debugging output of the game.
  2. *_replay.log: This file contains all game relevant actions you and other players did in the game (e.g. place a building, send soldiers somewhere). With this game, we can recalculate your game and debug it to find the exact source of your trouble.
4. Specify the game time when you first experienced the bug (the game time can be found in the upper right corner while playing). 
5. If you were playing a custom map, please attach the map. Without it, replaying the game won't be possible.
6. Describe what problem you experienced so that we can easily understand it.

Many thanks in advance for helping to improve this game!


## Playing JSettlers

In order to play the game, you need to have the folders "GFX", "SND" and "MAP" (optional) of the original version of "The Settlers 3" as obtained by installing the original "The Settlers 3" game (DEMO version also works).

Furthermore, you need an up-to-date installation of [Java 11 or newer](https://adoptium.net/). A Java Installation is needed to run the desktop version of JSettlers, as it is written in the programming language Java.

Java is preinstalled on Android.

After that, follow the detailed installation instructions for you platform.

### Windows and Linux
1. Install "The Settlers III" or a demo version ([Settlers III Amazons Demo](http://www.siedler-maps.de/downloads.php?action=download&downloadid=41)) of it. Don't worry, if it is not running on your OS, we only need the graphics and sound files. In order to get them, you can also unzip the Amazons Demo exe file (yes: unzip the .exe) and copy the folders `Gfx`, `Snd` and `Map` (optional) into an empty folder on your computer.
2. Download the newest stable [release of JSettlers*.zip](https://github.com/paulwedeck/settlers-remake/releases) (this also includes the MapEditor).
3. Unpack the downloaded archive to wherever you want JSettlers' installation to be.
5. Run the "JSettlers.jar" file.
  1. On the first start, the game will ask you for the folder where you've installed / unziped (see step 1) the original Settlers III. Please select the respective folder and continue.
  2. Have fun and enjoy the game!
6. Please have a look at the [manual](https://github.com/jsettlers/settlers-remake/wiki/JSettlers-Manual). The current state of the game lacks some controls known from the original, but also contains new ways to do things, which you shouldn't miss. 

#### Arch Linux
1. Install [jsettlers-git](https://aur.archlinux.org/packages/jsettlers-git/) from the AUR.
2. Optionally: Install [settlers3-demo-data](https://aur.archlinux.org/packages/settlers3-demo-data/) if you don't own an original The Settlers III and select the following folder when the game asks you: /usr/share/jsettlers/s3
3. You can start the game from the system-menu or with the commands "jsettlers" and "jsettlers-mapcreator".
4. See instructions above

### macOS
JSettlers runs natively on Apple Silicon (M1/M2/M3) and Intel Macs. Rendering goes through Vulkan via MoltenVK (Apple does not ship a Vulkan loader, so MoltenVK is required at runtime).

1. Install [MoltenVK](https://github.com/KhronosGroup/MoltenVK) (~15 MB):
    ```
    brew install molten-vk
    ```
   The launcher auto-detects MoltenVK at the standard Homebrew prefixes (`/opt/homebrew/lib/libMoltenVK.dylib` on Apple Silicon, `/usr/local/lib/libMoltenVK.dylib` on Intel). If you installed MoltenVK elsewhere, point the loader at it explicitly with `VK_ICD_FILENAMES=/path/to/MoltenVK_icd.json` or `-Dorg.lwjgl.vulkan.libname=/path/to/libMoltenVK.dylib`.
2. Install Java 17 or newer (e.g. [Adoptium Temurin](https://adoptium.net/)).
3. Install or unzip "The Settlers III" demo (see step 1 of [Windows and Linux](#windows-and-linux)).
4. Download / unpack JSettlers and run `JSettlers.jar` (or `bin/jsettlers.main.swing` from a Gradle install).
5. The first launch prompts for the original-game folder, just like on the other platforms.

There is also a convenience launcher at `scripts/run-jsettlers-macos-vulkan.sh` that probes the Homebrew install of MoltenVK, sets the relevant `MVK_CONFIG_*` env vars known to misbehave on Apple Silicon, and starts the game via Gradle. It is the easiest way to run from a fresh checkout.

##### macOS troubleshooting
- **Solid black gameplay screen** on Apple Silicon usually means MoltenVK's Metal-argument-buffers path is biting you. JSettlers requests `MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=0` via `VK_EXT_layer_settings` during `vkCreateInstance`, which is the supported in-process channel for MoltenVK 1.2.7+. If you're on an older MoltenVK or the extension isn't advertised, set the same value via env var (`MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=0`) before launching; the launcher script does this for you.
- **`vkCreateInstance failed`** at startup means MoltenVK could not be loaded. Verify `brew list molten-vk` and confirm `libMoltenVK.dylib` is present in `/opt/homebrew/lib` (Apple Silicon) or `/usr/local/lib` (Intel).
- For more verbose diagnostics, set `MVK_CONFIG_LOG_LEVEL=3` (info) or `4` (verbose) before launching.
- Other backends (`-Dgo.graphics.backend=lwjglx-gl` or `=jogl`) compile and start, but currently render to a black canvas on macOS 14+; treat them as developer-only.

#### Configuration Flags
As described before, the game's UI is still lacking a lot of features. That's why we have to offer some configurations via an options file. You can find a default `options.prp` file aside the `JSettlers.jar` file after you unpacked the archive. 

When opening the file, you will see several options that can be enabled by uncommenting them (remove the # at the beginning of the respective line). This is also described in the file. 

**Possible configurations include:**
- all-ai: Let all players be played by the AI. You will be able to watch all AI players and to "assist" them during the game.
- fixed-ai-type=YYYYY: Option to specify an AI type that shall be used for all AI players. The default behavior is to use a the weakest AI type for the first player and increase the difficulty for every player. Possible values: ROMAN_VERY_EASY, ROMAN_EASY, ROMAN_HARD, ROMAN_VERY_HARD
- disable-ai: If this flag is enabled, no AI players will be present in single player games. 
- locale: If you want to test a different localization than your systems default, it can be specify with this option. The value should look like: en_en.

**Command line flags**
All the options above can also be specified as command line options. For this, you need to prepend every one of them with a double dash. Therefore the option `fixed-ai-type=YYYYY` should be specified as `--fixed-ai-type=YYYYY`.


### Android
1. Enable installation of Apps from "Unknown Sources".
2. Copy the folders "GFX", "SND" and "MAP" (optional) of your original "The Settlers 3" installation into a folder called "JSettlers" on your device. The "JSettlers" folder must be located in the root directory of your internal storage (alongside folders like "Download" or "DCIM") or your external storage (e.g. memory card).
3. Download the newest stable JSettlers-Android_v*.apk onto your Android device.
4. Install JSettlers by running the downloaded file.

## Build instructions and developer's guide
The [build instructions](https://github.com/paulwedeck/settlers-remake/wiki/Compiling-using-gradle) and the [developer's guide](https://github.com/paulwedeck/settlers-remake/wiki/Developer's%20Guide) can be found in our wiki.

### Textures
JSettlers is meant to be used with the original textures from Settlers 3.
However we are working on replacing these with new textures that have a more permissive license.
There is a [list](https://github.com/paulwedeck/settlers-remake/blob/master/TEXTURES.md) of textures that we didn't create ourselves but instead took from image sites on the internet



## Getting in Touch
Besides the possibility to report bugs on Github you can also join our [JSettlers Discord](https://discord.gg/2hVV4u6). Here you can discuss on development questions and find other players to meet with. 
