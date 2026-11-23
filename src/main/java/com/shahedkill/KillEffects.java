package com.shahedkill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client-side kill detection (the local player hit an entity and that entity then died)
 * and management of running effects.
 *
 * <p>Besides the vanilla death state (health 0), a fallback handles servers that never show a real
 * death: if the target disappears or is teleported away shortly after our hit, that counts as a kill,
 * unless an ender pearl of that target was seen in flight (the target just teleported itself).
 */
public final class KillEffects {
	/** Ticks after the last hit during which a death still counts as our kill. */
	private static final long KILL_WINDOW_TICKS = 100;
	/** Ticks after the last hit during which a disappearance / teleport still counts as a kill. */
	private static final long VANISH_WINDOW_TICKS = 60;
	/** A single-tick position change larger than this is treated as a teleport. */
	private static final double TELEPORT_DISTANCE = 6.0;
	/** The target must have been this close to the player when it vanished. */
	private static final double VANISH_MAX_PLAYER_DISTANCE = 12.0;
	/** A pearl seen this recently before the vanish means the target teleported itself. */
	private static final long PEARL_MEMORY_TICKS = 100;

	private static final class Track {
		long hitTime;
		Vec3 lastPos;
		long pearlSeen = Long.MIN_VALUE / 2;
		final EntityType<?> type;

		Track(long hitTime, Vec3 lastPos, EntityType<?> type) {
			this.hitTime = hitTime;
			this.lastPos = lastPos;
			this.type = type;
		}
	}

	private static final Map<Integer, Track> hits = new HashMap<>();
	private static final List<ShahedFlight> flights = new ArrayList<>();
	private static final List<ExplosionEffect> explosions = new ArrayList<>();

	public static void onAttack(Entity target) {
		if (Config.effect == KillEffectType.OFF) {
			return;
		}
		long now = target.level().getGameTime();
		Track track = hits.get(target.getId());
		if (track == null) {
			hits.put(target.getId(), new Track(now, target.getBoundingBox().getCenter(), target.getType()));
			ShahedKillMod.LOGGER.info("[kill] hit {} (id {})", target.getType(), target.getId());
		} else {
			track.hitTime = now;
		}
	}

	public static void tick(ClientLevel level) {
		long now = level.getGameTime();

		Iterator<Map.Entry<Integer, Track>> it = hits.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Track> e = it.next();
			Track track = e.getValue();
			Entity entity = level.getEntity(e.getKey());

			if (now - track.hitTime > KILL_WINDOW_TICKS) {
				ShahedKillMod.LOGGER.info("[kill] drop id {}: no death within the window", e.getKey());
				it.remove();
				continue;
			}

			if (entity == null || entity.isRemoved()) {
				// the target vanished from the client world
				it.remove();
				long since = now - track.hitTime;
				boolean pearl = now - track.pearlSeen <= PEARL_MEMORY_TICKS;
				Minecraft mc = Minecraft.getInstance();
				boolean near = mc.player != null && mc.player.position().distanceTo(track.lastPos) <= VANISH_MAX_PLAYER_DISTANCE;
				if (since <= VANISH_WINDOW_TICKS && !pearl && near) {
					ShahedKillMod.LOGGER.info("[kill] id {} vanished {} ticks after the hit -> kill", e.getKey(), since);
					kill(level, track.lastPos, track.type);
				} else {
					ShahedKillMod.LOGGER.info("[kill] id {} vanished, ignored (since={}, pearl={}, near={})", e.getKey(), since, pearl, near);
				}
				continue;
			}

			Vec3 center = entity.getBoundingBox().getCenter();

			if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
				it.remove();
				ShahedKillMod.LOGGER.info("[kill] id {} is dead -> kill", e.getKey());
				kill(level, center, entity.getType());
				continue;
			}

			// remember pearls thrown by the target
			for (Entity other : level.entitiesForRendering()) {
				if (other.getType() == EntityType.ENDER_PEARL && other instanceof Projectile p && p.getOwner() == entity) {
					track.pearlSeen = now;
					break;
				}
			}

			// sudden teleport right after our hit (servers that "respawn" the target silently)
			if (center.distanceTo(track.lastPos) > TELEPORT_DISTANCE) {
				it.remove();
				long since = now - track.hitTime;
				boolean pearl = now - track.pearlSeen <= PEARL_MEMORY_TICKS;
				if (since <= VANISH_WINDOW_TICKS && !pearl) {
					ShahedKillMod.LOGGER.info("[kill] id {} teleported {} ticks after the hit -> kill", e.getKey(), since);
					kill(level, track.lastPos, track.type);
				} else {
					ShahedKillMod.LOGGER.info("[kill] id {} teleported, ignored (since={}, pearl={})", e.getKey(), since, pearl);
				}
				continue;
			}
			track.lastPos = center;
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

	private static void kill(ClientLevel level, Vec3 pos, EntityType<?> type) {
		if (type == EntityType.CAT || type == EntityType.WOLF) {
			PetMeme.play();
		} else {
			play(level, pos);
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
