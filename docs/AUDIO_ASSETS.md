Jumper — audio assets
=====================

SoundManager (app/src/main/java/.../core/audio/SoundManager.kt) automatically
picks up audio files placed in this folder by resource name.

Background music (PRESENT in this build):
  music_menu.mp3  — main menu / shop / leaders / settings  (Cosmic Bubble Hop)
  music_game.mp3  — gameplay screen                        (Neon Jet Hop)

Sound effects (NOT yet provided — currently synthesized tones are used):
  sfx_jump      — hero bounces off a platform
  sfx_booster   — a booster is collected
  sfx_monster   — hero stomps a monster
  sfx_hazard    — hero is hit by a monster or spike
  sfx_win       — level completed
  sfx_lose      — game over

To replace the synthesized SFX with real audio, drop short .ogg/.wav files
here using exactly the names above (lowercase, digits, underscores only,
no extension in code). No code change is required.
