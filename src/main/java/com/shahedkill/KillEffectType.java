package com.shahedkill;

import net.minecraft.network.chat.Component;

/** Selectable kill effects. More can be added here later. */
public enum KillEffectType {
	OFF("shahedkill.effect.off"),
	SHAHED("shahedkill.effect.shahed");

	private final String key;

	KillEffectType(String key) {
		this.key = key;
	}

	public Component label() {
		return Component.translatable(key);
	}

	public KillEffectType next() {
		KillEffectType[] all = values();
		return all[(ordinal() + 1) % all.length];
	}
}
