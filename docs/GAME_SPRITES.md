# Jumper — gameplay element sprites

All gameplay elements (platforms, hazards, boosters) are drawn from artwork
in `app/src/main/res/drawable`. The mapping lives in
`games/jumper/JumperSprites.kt` (the `Sprite` enum). To swap any art, replace
the file of the same name — no code change needed.

## Supplied sprites

| File                     | Used for                                    |
|--------------------------|---------------------------------------------|
| `platform_simple.png`    | Static, Moving, Vanishing, Rare platforms    |
| `platform_frozen.png`    | Fragile platform                             |
| `hazard_enemy.png`       | Monster hazard                               |
| `hazard_bomb.png`        | Spike hazard                                 |
| `booster_spring.png`     | Spring booster                               |
| `booster_jetpack.png`    | Jetpack booster                              |
| `booster_shield.png`     | Shield booster (also Magnet & Star for now)  |

## Not yet supplied — currently reusing other art

These reuse an existing sprite as a placeholder. To give them unique art,
add a drawable and a new `Sprite` enum entry, then point the mapping at it
in `JumperSprites.kt`:

- Moving / Vanishing / Rare platforms — reuse `platform_simple`.
- Magnet booster — reuses `booster_shield`.
- Star booster — reuses `booster_shield`.

## Hero skins

Hero skin sprites are separate — see `docs/HERO_SPRITES.md`.

## Notes

- The background sky gradient, clouds and the GOAL line are still drawn
  procedurally (no sprites were supplied for them); they are screen
  decoration, not gameplay objects.
- Sprite sizes on screen: hazards ~96px, boosters ~70px (largest side,
  aspect-ratio preserved). Tune `hazardDrawSize` / `boosterDrawSize` in
  `JumperGameView` if needed.

## Background images (swappable via shop)

`games/jumper/JumperBackgrounds.kt` maps each background item id to a
full-screen image drawn behind the gameplay (centre-crop fill, like the menu
background). Currently only `bg_doodle_paper` -> `default_bg` ships art; the
others fall back to the JumperTheme gradient.

To enable a background image, drop a PNG into `res/drawable` named:
`game_bg_forest`, `game_bg_sky_high`, `game_bg_underwater`,
`game_bg_space`, `game_bg_lava` — no code change needed.
