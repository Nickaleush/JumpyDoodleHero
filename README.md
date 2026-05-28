# Jumper - Android XML/Kotlin Prototype

This project is the first playable milestone for **Jumper**, based on the requested mini-game skeleton.

## Current milestone

Implemented:

- Main menu with coins, Play, Shop, Leaders and Settings.
- Levels screen with 45 level buttons.
- First five gameplay patterns:
  - Level 1: 200 m, static platforms.
  - Level 2: 500 m, static + moving platforms.
  - Level 3: 1000 m, disappearing platforms.
  - Level 4: 1800 m, fragile platforms.
  - Level 5: 3000 m, all platform types plus rare platforms.
- Levels 6-45 currently reuse the first five patterns by cycle.
- Playable game screen implemented with a custom Canvas view.
- Automatic jumping after touching platforms.
- Gravity and horizontal control.
- Controls:
  - device tilt through accelerometer;
  - fallback swipe control: horizontal swipes/drags move the hero left or right.
- Platforms:
  - static;
  - moving;
  - vanishing;
  - fragile;
  - rare bonus jump platforms.
- Hazards:
  - monsters;
  - spikes.
- Boosters:
  - Spring;
  - Jetpack;
  - Shield;
  - Magnet;
  - Star.
- HUD:
  - level;
  - height;
  - goal;
  - score;
  - shield lives;
  - pause button.
- Pause overlay with Continue, Restart and Home.
- Win condition: reach target height.
- Lose condition: fall below the screen or hit a monster/spike without shield.
- Result screen:
  - Level passed / Game Over;
  - score;
  - height;
  - reward coins;
  - next / replay / home buttons.
- Best score saved locally through Room.

## Important files

```text
app/src/main/java/com/businessdoomguy/minigamestarter/games/jumper/JumperGameView.kt
app/src/main/java/com/businessdoomguy/minigamestarter/games/jumper/JumperLevelConfig.kt
app/src/main/java/com/businessdoomguy/minigamestarter/feature/game/GameFragment.kt
app/src/main/res/layout/fragment_game.xml
app/src/main/res/navigation/nav_graph.xml
```

## Next milestones

Recommended next steps:

1. Add real art assets for hero, platforms, boosters and background.
2. Add sound and music settings.
3. Persist coins through a WalletRepository.
4. Add skin/background shop content.
5. Add level unlock logic.
6. Add local top-10 leaderboard view.
7. Balance platform spacing, hazard chance and jump physics after testing on device.

## Notes

The current game is intentionally implemented without a third-party engine. It uses a custom Android `View`, Canvas drawing and `Choreographer` for the game loop, which keeps the project small and easy to evolve for mini-games.

## Milestone: Balance, Shop, Leaders and Achievements

Implemented according to the Jumper technical notes:

- Level rewards: 50 / 100 / 200 / 200 / 300 coins for level patterns 1-5.
- In-level score balance:
  - +1 score for each passed meter.
  - +20 score for jumping on a monster.
  - +30 score for ordinary booster activation.
  - +100 score for Star bonus.
- Persistent wallet stored in Room.
- Shop with two content groups:
  - Hero skins: Doodle Boy, Ninja, Robot, Space Cat, Dragon.
  - Backgrounds: Doodle Paper, Forest, Sky High, Underwater, Space, Lava World.
- First default hero skin and background are free and selected by default.
- Buying an item unlocks it; tapping an unlocked item selects it.
- Selected hero skin and background are applied to the game screen.
- Local top-10 leaderboard stored via Room scores.
- Achievement system stored via Room:
  - First Jump
  - Sky Climber
  - Spring Lover
  - Jetpack Pilot
  - Monster Stomper
  - Marathon Climber
  - Untouchable
  - Star Collector
  - Speed Demon
  - JumpyDoodle Legend
- Settings screen stores sound/music flags in the player profile.

Note: the project is intentionally single-module and DI-free. AppServiceLocator owns the repositories and DAOs.

## Passable level generation fix

The Jumper level generator now uses a safe-first platform chain:

- every next platform is generated from the previous landable platform;
- vertical gaps are capped below the hero's theoretical jump height;
- horizontal platform offsets are capped to remain reachable with touch/tilt control;
- static recovery platforms are injected regularly;
- hazards are placed outside the mandatory route corridor;
- boosters are optional and never required to complete a level;
- generated levels are validated at runtime and fail fast in debug-like scenarios if an unreachable gap is produced.

This means all 45 levels should be passable without relying on lucky booster placement or impossible jumps.


## Swipe control update

The old edge-hold fallback control has been removed. The player can now control horizontal movement in two ways only:

- device tilt left/right through the accelerometer;
- horizontal swipe/drag across the game screen.

A simple tap or holding the left/right side of the screen no longer moves the hero. Movement starts only after a real horizontal gesture passes the swipe dead zone.

## Milestone: splash screen + portrait orientation

Added in this build:

- Portrait orientation is locked in `AndroidManifest.xml` with `android:screenOrientation="portrait"` on `MainActivity`.
- The navigation graph now starts from `SplashFragment` instead of `MenuFragment`.
- `SplashFragment` shows a polished animated entry sequence:
  - static hero/logo image fade-in and scale-in;
  - soft jump motion;
  - animated shadow;
  - floating clouds;
  - title/subtitle entrance;
  - progress bar fill before opening the menu.
- The placeholder splash image is `app/src/main/res/drawable/splash_static_image.xml`.

To use your own static splash picture, replace `splash_static_image.xml` with your own PNG/WebP using the same resource name:

```text
app/src/main/res/drawable/splash_static_image.png
```

Keep the name `splash_static_image` so the layout continues to work without code changes.

## Passable level generation update

The Jumper level generator now uses a verified two-layer algorithm:

1. **Critical static route**: every level has a guaranteed chain of static platforms from the start to the goal. The route is generated using physics-based limits from the hero jump velocity, gravity and horizontal speed. Boosters, rare platforms and monster kills are never required to finish a level.
2. **Challenge layer**: moving, vanishing, fragile and rare platforms are added as optional risk/reward objects around the critical route. They make higher levels more difficult and varied without making them mathematically impossible.
3. **Runtime validation**: after generation, the route is checked for vertical reach, horizontal reach and hazard placement. If a generated route would be impossible, the game fails fast during development instead of shipping a broken level.

Difficulty follows the design document:

- Level pattern 1: 200 m, calm scrolling, static route, very small gaps.
- Level pattern 2: 500 m, medium speed, optional moving platforms.
- Level pattern 3: 1000 m, faster, optional vanishing platforms.
- Level pattern 4: 1800 m, very fast, optional fragile platforms.
- Level pattern 5: 3000 m, maximum difficulty, all platform types and rare platforms.

Levels 6-45 repeat the five difficulty patterns until handcrafted content is added.

## Milestone: harder passable generation V3

The previous safe-route generator was too trivial. This build keeps the passability guarantee, but makes the gameplay more interesting:

- route platforms are no longer a simple centered static ladder;
- hard levels use wider horizontal zig-zags, side lanes and smaller platforms;
- jumps are generated in rhythm bursts: several tense jumps followed by a recovery jump;
- level patterns 2-5 can place moving / vanishing / fragile route platforms where appropriate;
- optional challenge platforms appear more often and are placed as risk/reward side choices;
- hazards can appear closer to the playable route on higher patterns, but still cannot block the verified landing corridor;
- physics validation still checks vertical and horizontal reachability after generation.

The design target is: every level should be finishable without mandatory boosters, but the player should need active swipe/tilt control instead of just bouncing upward passively.

## Generation V4 notes

This build replaces the too-safe V2/V3 feel with a harder passable generator:

- levels still have a verified critical path, but jumps are wider, platform widths are smaller, and more route platforms can be moving/vanishing/fragile on levels 2-5;
- extra challenge platforms are generated around the route to bring back the less predictable feeling of the first version;
- hazards are never allowed to crash the app: if a hazard intersects the mandatory landing lane it is discarded during generation;
- boosters remain optional and are not required for level completion.

## Generation V5: production playable graph

This build replaces the previous ladder-like safe route with an authored procedural graph:

- platforms are generated with strict no-overlap validation;
- the main route is always reachable using normal jump physics, without mandatory boosters;
- the route uses side lanes, zig-zags, rhythm bursts and recovery jumps so it does not feel like a trivial staircase;
- optional branch platforms create risk/reward paths, but they are also checked for reachability;
- moving, vanishing, fragile and rare platforms appear according to the level pattern;
- hazards are placed near the route for pressure, but never inside the mandatory landing corridor;
- invalid worlds are regenerated automatically instead of throwing a runtime exception.

The generator is original project code. It intentionally does not copy proprietary algorithms from any Google Play game.

## Milestone: level unlock, audio, edge control, theming (current build)

This build adds the features required for a Google Play release.

### Level unlock progression
- `PlayerProfileEntity.maxUnlockedLevel` tracks progress (Room DB v3, with a
  real `MIGRATION_2_3` so existing player data survives an update).
- Completing a level unlocks the next one (`GameRepository.unlockUpToLevel`).
- `LevelsFragment` shows locked levels with a padlock and blocks entry.

### Audio
- `core/audio/SoundManager` plays sound effects (`SoundPool`) and looping
  background music (`MediaPlayer`), both gated by the player's settings.
- Background music ships in `res/raw`: `music_menu` (menu) and `music_game`
  (gameplay). Sound effects fall back to synthesized tones until real
  `sfx_*` files are added to `res/raw` — see `docs/AUDIO_ASSETS.md`.
- The Settings sound / music switches are now fully wired.

### Controls
- Horizontal control is now: device tilt (accelerometer) OR pressing /
  holding the left / right side of the screen. Swipe control was removed.
- Multi-touch aware, with a small centre dead zone and on-screen edge hints.

### Difficulty progression
- `JumperLevelConfig.forLevel` and `LevelGenerationProfile.forLevel` apply a
  continuous difficulty ramp across all 45 levels: scroll speed, jump width,
  platform narrowness, hazard pressure and branch density all grow with the
  level number, on top of the five base archetypes.
- Route gaps follow sine-based "tension arcs" with periodic challenge bursts
  instead of uniform random gaps.

### Theming and skins ("полная смена дизайна")
- `games/jumper/JumperTheme.kt` — every gameplay colour (background,
  platforms, hazards, goal). Edit one entry to restyle the whole game.
- `games/jumper/JumperSkins.kt` — every hero skin's shape and colours, plus
  `JumperSkinRenderer` which draws distinct characters (not recoloured
  circles). Buying a skin in the shop changes the in-game hero.
- `games/jumper/JumperPreviewView.kt` — shop preview widget reusing the real
  renderer / theme, so shop previews always match gameplay.

### Release configuration
- `app/build.gradle.kts`: release build type with R8 minification, resource
  shrinking and a keystore-properties-driven signing config.
- `app/proguard-rules.pro`, `keystore.properties.template`, adaptive launcher
  icon, backup rules. See `RELEASE.md` for the full release checklist.

## Milestone: artwork sprites + level-select music (current build)

- All gameplay elements — platforms, hazards, boosters — are now drawn from
  artwork sprites in `res/drawable` instead of procedural shapes. The mapping
  is in `games/jumper/JumperSprites.kt`; see `docs/GAME_SPRITES.md`.
- Splash screen now uses the supplied full-screen artwork; only the loading
  progress bar is overlaid, pinned to the bottom.
- Third background music track added for the level-select screen
  (`music_levels`), wired into `LevelsFragment`.
- `PlatformType` / `BoosterType` / `HazardType` moved to top level so the
  sprite registry can map them to artwork.

Placeholders still to replace with dedicated art: Moving/Vanishing/Rare
platforms (reuse `platform_simple`), Magnet & Star boosters (reuse
`booster_shield`), and the five hero skins (procedural fallback). See the
files in `docs/` for exact resource names.
