#!/bin/bash
# prints selected Minecraft class sources into the CI log
S="$1"
echo "################ class paths"
find "$S" \( -name "Cat.java" -o -name "Wolf.java" -o -name "Ocelot.java" -o -name "HudElementRegistry.java" -o -name "VanillaHudElements.java" -o -name "DeltaTracker.java" \) | head
echo "################ GuiGraphics blit signatures"
grep -n "public void blit\|public void fill(" "$(find "$S" -name GuiGraphics.java | head -1)" | head -30
echo "################ RenderPipelines GUI"
grep -n "GUI_TEXTURED\|GUI =" "$(find "$S" -name RenderPipelines.java | head -1)" | head
echo "################ SimpleSoundInstance"
f=$(find "$S" -name SimpleSoundInstance.java | head -1); grep -n "public\|static" "$f" | head -30
echo "################ Minecraft window/gui"
grep -n "getWindow()\|public Gui gui\|getGuiScale\|getGuiScaledWidth\|getGuiScaledHeight" "$(find "$S" -name Minecraft.java | head -1)" | head
grep -n "getGuiScaledWidth\|getGuiScaledHeight" "$(find "$S" -name Window.java | head -1)" | head
echo "################ SoundManager"
grep -n "public .*(" "$(find "$S" -name SoundManager.java | head -1)" | head -30
