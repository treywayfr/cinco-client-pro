# ★ STAR CLIENT 2.3.0

**Target:** Minecraft Java Edition 1.21.1 · Fabric · Java 21

Star Client is a performance-focused custom Minecraft client with a fully animated ClickGUI, selectable UI font profiles, configurable HSV theme, draggable Screen GUI widgets, PvP helpers, Free Cam, custom Player Changer models, player/storage/light ESP, DonutSMP-style loaded-chunk tools, and adaptive rendering controls.

## Build and install (Windows)
1. Install JDK 21 from Adoptium Temurin (Windows x64 JDK 21).
2. Download the repository ZIP and extract it.
3. Run `build-star.bat` (or `build-star.ps1`). The helper uses Gradle 8.10.2 if installed or downloads it into the local `.tools` folder.
4. After `BUILD SUCCESSFUL`, find `build\libs\star-client-2.3.0.jar`.
5. Put the JAR and Fabric API for Minecraft 1.21.1 in `%appdata%\.minecraft\mods`, then launch Fabric 1.21.1.

## Main controls
- **Right Shift:** open Star Client by default.
- **H:** toggle the Screen GUI/HUD master switch.
- **C:** Free Cam by default.
- **Right-click a module:** expanded settings.
- **Middle-click a module:** favorite/unfavorite it.
- **Search:** click the search box and type to filter modules.
- **Open Minecraft chat:** enabled Screen GUI widgets enter edit mode. Drag a widget with left click, hover it and use the mouse wheel to resize it, or click its `×` to disable that widget.
- Module settings and keybinds persist in the client config; HUD positions/scales persist in `star-hud.json`.

## Interface
Star Client uses a compact two-column module layout with smooth hover, click, toggle, category, search-focus and open/close animations. Performance Mode automatically shortens or removes decorative animation and reduces ESP/cosmetic rendering when FPS drops below your chosen target.

Available interface font profiles include **Minecraft Ten**, **Clean Sans**, **Classic Bold**, and **Compact UI** using bundled Minecraft font renderers so the client does not depend on external system fonts.

The global Color Wheel controls the client palette. Star Client also supports optional soft GUI click sounds, favorites, and quick profiles for **Custom**, **PvP**, **DonutSMP**, **Visual**, and **Performance** setups.

## PvP / utility highlights
- **Auto Crystal:** while enabled, hold an end crystal in the main hand, look directly at obsidian, and hold right-click. It only works on the exact targeted obsidian rather than acting like a crystal aura.
- **Anchor Macro:** one-key anchor placement/charge/detonation flow. It handles replaceable blocks such as fire and water and uses a totem from the hotbar for detonation rather than relying on the offhand.
- **Key Pearl:** selects and throws a hotbar pearl with configurable restore behavior.
- **Fast XP:** hold XP bottles and right-click for rapid client-side use attempts, up to one use attempt per client tick at the fastest setting.
- **Flight:** client-side directional flight; servers may still reject movement that their rules do not allow.
- **Free Cam:** moves only a detached local render camera through already loaded terrain while your real player position remains untouched by the Free Cam controller.

## Player Changer
Player Changer replaces the local blocky player render with custom low-poly characters. Current choices include:
- Franklin / GTA V-inspired streetwear character
- Spiky cartoon kid
- Anime heroine
- Tung Tung Sahur-inspired wooden character

Close-range detail uses automatic level-of-detail rendering to protect FPS. Player Changer also has **First-person hands**, which replaces the vanilla first-person arm with a hand/sleeve matching the selected character and palette. The settings screen includes a live character preview.

## ESP and world visuals
- **Player ESP:** through-wall player glow plus clean 3D box/corner-box geometry and optional compact names/distances.
- **Storage ESP:** Minecraft-selection-style through-wall outlines around storage blocks. Chests, barrels, shulkers, ender chests, hoppers, furnaces, dispensers/droppers and spawners each have their own highlight toggle, tracer toggle and color.
- **Storage tracers:** connect the storage block center to the real player's eye position, including while Free Cam is detached.
- **Light Finder:** highlights likely underground artificial-light pockets using loaded-world block-light data.
- **ESP Style:** shared **Clean**, **Neon**, **Minimal**, and **Bold** visual presets for player, storage and light ESP.
- **Fullbright:** strong lightmap override plus extended configurable gamma.
- **Sus Chunk Finder:** ranks already-loaded chunks using storage/utility density and underground light evidence. It does not request or reveal unloaded chunks.

## Performance
Star Client keeps the requested 8× supersampled rounded-edge quality, but caches corner coverage by radius and draws rounded rows as spans instead of rebuilding colored AA textures every frame. Performance Mode also reduces star particles, decorative animation, ESP limits and light-render load when FPS falls below the configured target.

## Notes
World scanners use only data Minecraft has already loaded locally; they do not load distant chunks or bypass server protections. Gameplay automation may consume items, cause self-damage, or violate server rules, so use it only where permitted.

GitHub Actions builds **Star Client** and publishes `star-client-2.3.0` when compilation succeeds.
