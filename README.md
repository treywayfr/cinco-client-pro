# CINCO CLIENT — Ember Edition

**Target:** Minecraft Java Edition 1.21.1 · Fabric · Java 21

Cinco Client has an animated purple-star ClickGUI, a global HSV color wheel, bold typography, configurable module settings, local HUD widgets, PvP helpers, and local loaded-area world scans.

## Quick build (Windows)
1. Install JDK 21: https://adoptium.net/temurin/releases/?version=21 (Windows x64, JDK, version 21, MSI installer).
2. Download this repository as ZIP and extract it.
3. Open the extracted folder and double-click `build-cinco.bat`. It uses Gradle 8.10.2 if installed, or downloads it into a local `.tools` folder.
4. After `BUILD SUCCESSFUL`, find `build\libs\cinco-client-2.0.0.jar`.

Install that JAR and Fabric API for Minecraft 1.21.1 in `%appdata%\.minecraft\mods`. Launch the Fabric 1.21.1 profile.

## Controls
- **Right Shift:** open/close GUI by default (remappable under SETTINGS → Open GUI)
- **H:** toggle HUD by default (remappable under SETTINGS → Toggle HUD)
- **Mouse wheel:** scroll modules
- **Left-click a module card:** toggle it (the Color Wheel card opens the picker)
- **Drag a card slider:** change its main setting
- **Right-click a module card:** open its full settings
- **Click MODULE KEYBIND, then press a keyboard key:** assign that module's hotkey
- **Delete/Backspace while capturing a key:** clear its binding
- **Escape while capturing:** cancel key capture
- **Slash (/):** focus search
- **Escape/Right Shift:** close the GUI

Each module has three sliders (primary and two extra settings), an independently saved hotkey, enable state, and reset-to-defaults action. Right-click the Color Wheel card under SETTINGS to open its detail page, or simply left-click that card to open the full hue/saturation/brightness wheel. Drag around the wheel to change hue and saturation; drag the brightness strip to change brightness. These colors update the ClickGUI, module editor, and in-game HUD immediately, and persist across restarts. Settings and keybinds persist in `.minecraft/config/cinco-client.json`.

Anchor Macro starts with **G**. When the module is enabled, its configured key runs its sequence; if it is disabled, the first press arms it and the next press runs it.

## Module categories
- **Combat:** Auto Crystal, Anchor Macro, Auto Safe Anchor, Auto Totem, Auto Eat, Offhand Manager, Auto Regear, Crystal Breaker, reminders, practice timer
- **Movement:** Sprint Assist, Safe Walk Guide, Elytra Reminder, Auto Swim, Fall Alert
- **Render:** Client HUD, FPS, coordinates, direction, armor, potion status, local scan radar, purple stars, accent glow, compact HUD, crosshair guide, target HUD, ping display
- **World:** Base Finder, Container Finder, Sus Chunk Finder, Portal Finder, Spawner Finder, Shulker Finder, Stash Radar, Chunk Density, Nearby Player List, Block Notes
- **Utility:** Config Auto-Save, Session Timer, Keystrokes, CPS, Safe Logout Reminder, Auto Refill, Inventory Checklist
- **Practice:** Anchor Practice Timer, Reach Trainer, Cooldown Coach, Surround Trainer, Pearl Reminder
- **Settings:** Color Wheel, Open GUI keybind, Toggle HUD keybind, Bold Typeface, GUI Animation, Panel Opacity

Some entries are configuration/visual foundations. Check each module in the detail screen for its current options.

## Accuracy and safety notes
World scanners inspect only block states already available in Minecraft's client-loaded area; they do not reveal unloaded chunks or bypass server protections. Anchor and combat automation can consume items, cause self-damage, or conflict with server rules. Test in a private world/server where permitted. No anti-cheat bypass or external-process injection is included.

The repository's GitHub Actions workflow builds the source and publishes the resulting mod JAR as a downloadable artifact when compilation succeeds.
