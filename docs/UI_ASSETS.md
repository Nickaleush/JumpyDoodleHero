# Jumper — UI artwork assets

Supplied artwork used by the menu, shop and leaders screens.

| Drawable                  | Used for                                        |
|----------------------------|-------------------------------------------------|
| `item_bg.9.png`            | Shop item card background (9-patch panel)       |
| `ic_coin.png`              | Coin icon — shop card price, menu/shop counters |
| `icon_trophy.png`          | Trophy icon on each achievement card            |
| `shop_skin_doodle_boy.jpg` | Shop badge — Doodle Boy skin                    |
| `shop_skin_ninja.jpg`      | Shop badge — Ninja skin                         |
| `shop_skin_robot.jpg`      | Shop badge — Robot skin                         |
| `shop_skin_space_cat.jpg`  | Shop badge — Space Cat skin                     |
| `shop_bg_forest.jpg`       | Shop badge — Forest background                  |
| `shop_bg_underwater.jpg`   | Shop badge — Underwater background              |
| `shop_bg_lava.jpg`         | Shop badge — Lava World background              |

## Notes

- The shop badge mapping lives in `feature/shop/ShopArtwork.kt`. Items
  without a dedicated badge (`skin_dragon`, `bg_doodle_paper`, `bg_sky_high`,
  `bg_space`) fall back to the procedural `JumperPreviewView`.
- `item_bg` is a 9-patch: the centre stretches, the decorative frame stays
  crisp, and the content padding is defined by the patch — so the card
  layout adds no extra padding.
- JPEG sources with solid backgrounds (platforms, trophy, coin) were
  converted to PNG with transparency so they composite cleanly on any
  background.

## Settings toggle icons (vector)

| Drawable          | State                    |
|-------------------|--------------------------|
| `ic_sound_on`     | Sound enabled (red)      |
| `ic_sound_off`    | Sound disabled (grey)    |
| `ic_music_on`     | Music enabled (red)      |
| `ic_music_off`    | Music disabled (grey)    |

The settings screen shows a tappable icon per row; tapping toggles the
setting and swaps the icon. Wired in `SettingsFragment`.

## Booster art status

Spring / Jetpack / Shield use real sprites (`booster_*`). Magnet and Star
have NO dedicated art yet, so `Sprite.forBooster` returns null for them and
`JumperGameView` draws a distinct coloured badge ("M" / "★"). This fixes the
earlier confusion where Star/Magnet were drawn as a Shield. Add
`booster_magnet` / `booster_star` drawables and map them in
`JumperSprites.kt` to replace the badges.

## Disappearing platforms

Both Vanishing and Fragile platforms (which vanish when landed on) use the
`platform_frozen` ice art so the player can tell them apart from solid
platforms.
