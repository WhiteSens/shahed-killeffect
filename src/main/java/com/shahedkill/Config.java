package com.shahedkill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored in config/shahedkill.json. */
public final class Config {
	public static final int MIN_DISTANCE = 8;
	public static final int MAX_DISTANCE = 80;
	public static final float MIN_SPEED = 0.5f;
	public static final float MAX_SPEED = 3.0f;

	public static KillEffectType effect = KillEffectType.SHAHED;
	/** 0..1 */
	public static float volume = 0.8f;
	/** Horizontal distance (blocks) the Shahed starts its flight from. */
	public static int distance = 30;
	/** Flight speed multiplier, 0.5..3.0 */
	public static float speed = 1.0f;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final class Data {
		String effect = KillEffectType.SHAHED.name();
		float volume = 0.8f;
		int distance = 30;
		float speed = 1.0f;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("shahedkill.json");
	}

	public static void load() {
		Path file = file();
		if (!Files.exists(file)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			Data data = GSON.fromJson(reader, Data.class);
			if (data == null) {
				return;
			}
			try {
				effect = KillEffectType.valueOf(data.effect);
			} catch (IllegalArgumentException | NullPointerException e) {
				effect = KillEffectType.SHAHED;
			}
			volume = Mth.clamp(data.volume, 0f, 1f);
			distance = Mth.clamp(data.distance, MIN_DISTANCE, MAX_DISTANCE);
			speed = data.speed <= 0f ? 1.0f : Mth.clamp(data.speed, MIN_SPEED, MAX_SPEED);
		} catch (IOException | RuntimeException e) {
			ShahedKillMod.LOGGER.warn("Could not read config, using defaults", e);
		}
	}

	public static void save() {
		Data data = new Data();
		data.effect = effect.name();
		data.volume = volume;
		data.distance = distance;
		data.speed = speed;
		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file())) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			ShahedKillMod.LOGGER.warn("Could not save config", e);
		}
	}

	private Config() {
	}
}
