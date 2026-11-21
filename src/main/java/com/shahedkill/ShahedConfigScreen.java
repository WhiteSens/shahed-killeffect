package com.shahedkill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Settings menu: kill effect, volume, flight distance, test button. Opens with the K key by default. */
public class ShahedConfigScreen extends Screen {
	private final Screen parent;
	private Button effectButton;

	public ShahedConfigScreen(Screen parent) {
		super(Component.translatable("shahedkill.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 4 + 8;

		effectButton = addRenderableWidget(Button.builder(effectLabel(), button -> {
			Config.effect = Config.effect.next();
			button.setMessage(effectLabel());
			Config.save();
		}).bounds(cx - 100, y, 200, 20).build());

		addRenderableWidget(new VolumeSlider(cx - 100, y + 28, 200, 20));
		addRenderableWidget(new DistanceSlider(cx - 100, y + 56, 200, 20));

		addRenderableWidget(Button.builder(Component.translatable("shahedkill.screen.test"), button -> test())
				.bounds(cx - 100, y + 90, 200, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(cx - 100, y + 118, 200, 20).build());
	}

	private static Component effectLabel() {
		return Component.translatable("shahedkill.screen.effect", Config.effect.label());
	}

	/** Closes the menu and plays the effect a few blocks in front of the player. */
	private void test() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level instanceof ClientLevel level && mc.player != null) {
			Vec3 look = mc.player.getLookAngle();
			Vec3 target = mc.player.position().add(look.x * 8.0, 1.0, look.z * 8.0);
			onClose();
			KillEffects.play(level, target);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 14, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}

	private static final class VolumeSlider extends AbstractSliderButton {
		VolumeSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), Config.volume);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("shahedkill.screen.volume", Math.round(this.value * 100)));
		}

		@Override
		protected void applyValue() {
			Config.volume = (float) this.value;
		}
	}

	private static final class DistanceSlider extends AbstractSliderButton {
		DistanceSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(),
					(Config.distance - Config.MIN_DISTANCE) / (double) (Config.MAX_DISTANCE - Config.MIN_DISTANCE));
			updateMessage();
		}

		private int distance() {
			return Mth.clamp(Config.MIN_DISTANCE + (int) Math.round(this.value * (Config.MAX_DISTANCE - Config.MIN_DISTANCE)),
					Config.MIN_DISTANCE, Config.MAX_DISTANCE);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("shahedkill.screen.distance", distance()));
		}

		@Override
		protected void applyValue() {
			Config.distance = distance();
		}
	}
}
