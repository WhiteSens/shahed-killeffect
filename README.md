# Shahed Kill Effect

Client-side Fabric mod for **Minecraft 1.21.11**. When you kill an entity, a Shahed drone flies in from a
distance and falls onto the target.

* Press **K** (rebindable) to open the menu: choose the kill effect, volume and the distance the drone flies in from.
  The **Test effect** button plays the effect in front of you.
* Settings are saved to `config/shahedkill.json`.
* Requires Fabric Loader >= 0.20.1 and Fabric API. The mod is client-only; other players do not see the effect.

## Build

`./gradlew build` — the jar appears in `build/libs/`. GitHub Actions also builds it on every push (artifact `shahed-killeffect-jar`).

## Credits & license

The Shahed model, texture and engine sound come from the **Skyfront** mod by **Lexik** and **Krutnet**
(GPL-3.0-or-later). The model was converted to a vanilla item model with `tools/convert_geo.py`
(original geometry in `tools/source/`). This mod is therefore licensed under **GPL-3.0-or-later**.
