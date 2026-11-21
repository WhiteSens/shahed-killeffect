package com.shahedkill;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Hidden easter egg: when the player kills a cat or a dog, a short video clip
 * (pre-rendered frames + its sound track) is shown in the middle of the screen.
 * Intentionally has no entry in the config menu.
 */
public final class PetMeme {
	private static final int FRAMES = 34;
	private static final int FPS = 15;
	private static final int FRAME_W = 320;
	private static final int FRAME_H = 336;
	private static final long DURATION_MS = FRAMES * 1000L / FPS;

	private static final Identifier[] TEXTURES = new Identifier[FRAMES];
	private static final SoundEvent SOUND = SoundEvent.createVariableRangeEvent(ShahedKillMod.id("pet_meme"));

	private static long startedAt = -1;

	static {
		for (int i = 0; i < FRAMES; i++) {
			TEXTURES[i] = ShahedKillMod.id(String.format("textures/gui/meme/meme_%02d.png", i + 1));
		}
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ShahedKillMod.id("pet_meme"),
				(graphics, tickCounter) -> render(graphics));
	}

	public static void play() {
		startedAt = System.currentTimeMillis();
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SOUND, 1.0f, Math.max(0.05f, Config.volume)));
	}

	public static void stop() {
		startedAt = -1;
	}

	private static void render(GuiGraphics graphics) {
		if (startedAt < 0) {
			return;
		}
		long elapsed = System.currentTimeMillis() - startedAt;
		if (elapsed >= DURATION_MS) {
			startedAt = -1;
			return;
		}
		int frame = Math.min(FRAMES - 1, (int) (elapsed * FPS / 1000L));

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int h = (int) (sh * 0.5f);
		int w = h * FRAME_W / FRAME_H;
		int x = (sw - w) / 2;
		int y = (sh - h) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURES[frame], x, y, 0.0f, 0.0f, w, h, FRAME_W, FRAME_H, FRAME_W, FRAME_H);
	}

	private PetMeme() {
	}
}
