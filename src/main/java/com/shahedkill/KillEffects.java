package com.shahedkill;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client-side kill detection (the local player hit an entity and that entity then died)
 * and management of running effects.
 */
public final class KillEffects {
	/** Ticks after the last hit during which a death still counts as our kill. */
	private static final long KILL_WINDOW_TICKS = 100;

	private static final Map<Integer, Long> hits = new HashMap<>();
	private static final List<ShahedFlight> flights = new ArrayList<>();
	private static final List<ExplosionEffect> explosions = new ArrayList<>();

	public static void onAttack(Entity target) {
		if (Config.effect == KillEffectType.OFF) {
			return;
		}
		hits.put(target.getId(), target.level().getGameTime());
	}

	public static void tick(ClientLevel level) {
		long now = level.getGameTime();

		Iterator<Map.Entry<Integer, Long>> it = hits.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Long> e = it.next();
			Entity entity = level.getEntity(e.getKey());
			if (entity == null || entity.isRemoved() || now - e.getValue() > KILL_WINDOW_TICKS) {
				it.remove();
				continue;
			}
			if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
				it.remove();
				if (living.getType() == EntityType.CAT || living.getType() == EntityType.WOLF) {
					PetMeme.play();
				} else {
					play(level, living.getBoundingBox().getCenter());
				}
			}
		}

		Iterator<ExplosionEffect> ei = explosions.iterator();
		while (ei.hasNext()) {
			if (!ei.next().tick(level)) {
				ei.remove();
			}
		}

		Iterator<ShahedFlight> fi = flights.iterator();
		while (fi.hasNext()) {
			ShahedFlight flight = fi.next();
			if (!flight.tick(level)) {
				fi.remove();
			}
		}
	}

	/** Starts the currently selected kill effect at the given position. */
	public static void play(ClientLevel level, Vec3 target) {
		switch (Config.effect) {
			case SHAHED -> flights.add(ShahedFlight.spawn(level, target));
			case OFF -> {
			}
		}
	}

	/** Visual explosion at the impact point (called by the drone when it hits). */
	public static void explode(ClientLevel level, Vec3 impact) {
		explosions.add(new ExplosionEffect(level, impact));
	}

	public static void clear() {
		explosions.clear();
		hits.clear();
		PetMeme.stop();
		for (ShahedFlight flight : flights) {
			flight.discard();
		}
		flights.clear();
	}

	private KillEffects() {
	}
}
