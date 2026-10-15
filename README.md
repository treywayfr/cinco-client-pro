# CINCO CLIENT — Ember Edition 2.3.0

**Target:** Minecraft Java Edition 1.21.1 · Fabric · Java 21

Cinco Client combines an animated purple-star ClickGUI, the global HSV color wheel, bold themeable typography, expandable per-module settings, saved keyboard bindings, PvP quick-actions, local HUDs, FOV control, animated sky presets, and a generated local battle-royale-inspired skin.

## Build and install (Windows)
1. Install JDK 21: https://adoptium.net/temurin/releases/?version=21 (Windows x64, JDK, version 21, MSI installer).
2. Download the repository ZIP and extract it.
3. Open the extracted folder and double-click `build-cinco.bat`. It uses Gradle 8.10.2 if installed, or downloads it into the local `.tools` folder.
4. After `BUILD SUCCESSFUL`, find `build\libs\cinco-client-2.3.0.jar`.
5. Put the JAR and Fabric API for Minecraft 1.21.1 in `%appdata%\.minecraft\mods` and launch the Fabric 1.21.1 profile.

## Controls
- **Right Shift:** open the GUI by default (remappable via SETTINGS → Open GUI).
- **H:** toggle the HUD by default (remappable via SETTINGS → Toggle HUD).
- **Right-click a module card:** open expanded settings.
- **Click its keybind field, then press a key:** assign a module key; Delete clears it and Escape cancels.
- **Click the Color Wheel card:** open the global hue/saturation/brightness picker.
- **Mouse wheel:** scroll modules; **/** focuses module search; **Escape** closes screens.
- **Left-click a module card:** toggle it; drag sliders to change its main setting.

Module toggles, all three settings per module, and keyboard bindings are saved to `.minecraft/config/cinco-client.json`.

## PvP modules
- **Anchor Macro (G by default):** enable it, aim at a placeable block (or an existing respawn anchor), then press its chosen key. The macro places the anchor when needed, switches to glowstone to charge it, waits for the charge to register, then selects a totem and activates it. It refuses to run in the Nether because respawn anchors behave differently there. You need an anchor, glowstone and totem in the hotbar.
- **Auto Crystal:** enable it, hold end crystals, aim at an obsidian/bedrock base, and hold right-click. It attempts repeated placement while the button is held and rapidly attacks nearby crystals during that action.
- **Crystal Breaker:** attacks nearby crystals while enabled.
- **Key Pearl (V by default):** enable it and press its assigned key. It selects a hotbar pearl and throws it, optionally restoring your previous slot after the configurable delay.
- **Auto Totem, Auto Eat, Auto Safe Anchor, reminders and practice helpers:** adjustable module settings; test gameplay automation in a private world first.

## Visual modules
- **FOV Changer:** sets FOV while enabled, with an optional smooth transition.
- **Sky Changer:** animated presets: **Sunset, Aurora, Cyberpunk, Cosmic, Ember**. Speed and intensity are configurable; sky/cloud colors are client-side.
- **Fortnite Style:** swaps only your local player's rendered skin texture to a generated battle-royale-inspired tactical outfit. Outfit style, suit palette and visor are configurable. It does not alter your actual Mojang account skin or other players' skins.
- **Hover Glow:** animated accent glow on hovered module cards, detail sliders and color controls. Intensity, pulse speed and width are adjustable.
- **Color Wheel:** changes hue, saturation and brightness across the ClickGUI, module editors, HUD panels and accents.
- **HUD and render modules:** FPS, coordinates, direction, armor, potion count, click rate, keystrokes, crosshair guide and nearby-block readouts.

## World scanning notes
Base Finder, Container Finder, Sus Chunk Finder and related scanners inspect only block states already present in the client-loaded area. They don't load distant chunks, reveal unloaded bases, or bypass server protections.

## Important notes
Gameplay automation may consume items, cause self-damage, or violate server rules. Use it only where your server rules permit it. The generated skin is a client-side visual override, not an account-skin upload. Some less-used modules remain HUD/config foundations rather than full gameplay implementations.

GitHub Actions builds the mod and publishes the JAR as an artifact when compilation succeeds.
