# CINCO CLIENT — Purple Nebula Edition

**Target:** Minecraft Java Edition 1.21.1 · Fabric · Java 21

A client-side Fabric mod with a custom animated purple-star ClickGUI, configurable module settings, local HUD widgets, crystal interaction helpers, an anchor click macro, and an incremental scanner for containers in the nearby client-loaded area.

## Quick build (Windows)
1. Install JDK 21: https://adoptium.net/temurin/releases/?version=21 (select Windows, x64, JDK, version 21, and the `.msi` installer).
2. Download this repository as ZIP and extract it.
3. Open the extracted folder and double-click `build-cinco.bat`. It uses Gradle 8.10.2 if installed, or downloads it into a local `.tools` folder automatically.
4. When it says `BUILD SUCCESSFUL`, find `build\libs\cinco-client-2.0.0.jar`.

Install that JAR and Fabric API for Minecraft 1.21.1 in `%appdata%\.minecraft\mods`. Launch the Fabric 1.21.1 profile.

## Controls
- **Right Shift**: open/close GUI
- **H**: toggle HUD
- **Mouse wheel**: scroll modules
- **Click a category**: change category
- **Click a module card**: toggle it
- **Drag the purple slider**: change that module's setting
- **Slash (/)**: focus search; type to filter module names/descriptions
- **Escape**: close GUI (or clear active search first)

Settings and toggle states persist in `.minecraft/config/cinco-client.json`.

## Included modules
- **Combat:** Auto Crystal, Anchor Macro, Crystal Breaker, Totem Reminder, Gapple Reminder, Crystal Practice Timer
- **Movement:** Sprint Assist, Safe Walk Guide, Elytra Reminder
- **Render:** Client HUD, FPS Counter, Coordinates, Direction, Armor HUD, Potion Status, Container Radar HUD, Purple Stars, Accent Glow, Compact HUD, Crosshair Guide
- **World:** Base Finder, Container Finder, Nearby Player List, Block Notes
- **Utility:** Config Auto-Save, Session Timer, Keystrokes, CPS Display, Safe Logout Reminder
- **Practice:** Anchor Practice Timer, Reach Trainer, Cooldown Coach, Inventory Checklist

## Accuracy notes
The current project implements the animated GUI, category navigation, module toggles, per-module sliders, persistent configuration, HUD widgets, sprint assist, basic Auto Crystal behavior, Anchor Macro, and incremental local scanning for nearby container-like blocks. Some modules are UI/config foundations rather than complete gameplay features.

Base Finder reads only block states available in the client-loaded scan volume; it does not reveal unseen chunks or bypass server protections. Auto Crystal and Anchor Macro can cause self-damage, consume items, or violate server rules. Test them in a private world/server where permitted. There is no anti-cheat bypass, stealth injector, or external-process injection.

The source project has not been compiled in this environment. If the build reports an error, keep the window open and share the full error output.

## Design direction
The interface uses general UX ideas from modular clients (category navigation, overlays, per-module controls) but uses Cinco's own name, colors, layout, and implementation.

References:
- Meteor Client FAQ: https://meteorclient.com/faq
- Vape feature overview: https://docs.vape.gg/features/overlays/
- Prestige Client: https://prestige-client.net/
