#!/bin/bash
# prints selected Minecraft class sources into the CI log
S="$1"
show() { f=$(find "$S" -name "$1" | head -1); echo "################ $1 ($f)"; [ -n "$f" ] && cat "$f"; }
show Display.java
show KeyMapping.java
show Identifier.java
show AbstractSliderButton.java
show SoundEvent.java
echo "################ Entity load/save signatures"
grep -n "public void load\|public final void load\|void saveWithoutId\|getEntityData()" "$(find "$S" -name Entity.java | head -1)" | head -20
echo "################ ClientLevel add/remove entity"
grep -n "public void addEntity\|addEntity\|removeEntity\|public void playLocalSound\|playSound" "$(find "$S" -name ClientLevel.java | head -1)" | head -30
echo "################ element rotation"
for f in $(find "$S" -name "BlockElementRotation.java" -o -name "ElementRotation.java" -o -name "BlockElement.java" | head -3); do echo "== $f"; cat "$f"; done
echo "################ Screen head"
grep -n "public\|protected" "$(find "$S" -name Screen.java | head -1)" | head -120
echo "################ Button"
grep -n "public\|protected" "$(find "$S" -name Button.java | head -1)" | head -40
echo "################ SimpleSoundInstance / AbstractTickableSoundInstance"
for n in SimpleSoundInstance.java AbstractTickableSoundInstance.java AbstractSoundInstance.java; do show $n; done
