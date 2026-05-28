Jumper — hero skin sprites
==========================

JumperSkinRenderer (games/jumper/JumperSkins.kt) automatically uses a sprite
drawable for a hero skin when a drawable with the matching name exists in
this folder. While a sprite is absent, a procedural vector character is
drawn instead, so the game always works.

Expected drawable names (one per skin):

  hero_doodle_boy   — skin_doodle_boy (default, free)
  hero_ninja        — skin_ninja
  hero_robot        — skin_robot
  hero_space_cat    — skin_space_cat
  hero_dragon       — skin_dragon

How to add real art
-------------------
* Drop a PNG or WebP into res/drawable using exactly the name above, e.g.
  res/drawable/hero_doodle_boy.png
* Square images work best (the sprite is drawn into a square box around the
  hero). A transparent background is recommended.
* Suggested source size: 256x256 px (or provide density variants in
  drawable-mdpi/hdpi/xhdpi/xxhdpi if you want pixel-perfect scaling).
* No code change is required — the renderer picks the file up by name.
* Vector drawables (.xml) also work; they are rasterised on first use.

The sprite is rendered slightly larger than the hero's physics circle, so
leave a little padding around the character inside the image.
