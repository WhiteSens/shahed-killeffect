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
		float lastHealth = -1;
		boolean healthVaried;
		final EntityType<?> type;

		Track(long hitTime, Vec3 lastPos, EntityType<?> type) {
			this.hitTime = hitTime;
			this.lastPos = lastPos;
			this.type = type;
		}
	}

	/** If health is synced by the server, only low-health targets can have been killed by a hit. */
	private static final float KILL_MAX_HEALTH = 14.0f;
	/** Any ender pearl within this distance of the target counts as "the target teleported itself". */
	private static final double PEARL_RADIUS = 96.0;

	private static long pearlSeenTime = Long.MIN_VALUE / 2;
	private static Vec3 pearlSeenPos = Vec3.ZERO;

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

		if (!hits.isEmpty()) {
			for (Entity other : level.entitiesForRendering()) {
				if (other.getType() == EntityType.ENDER_PEARL) {
					pearlSeenTime = now;
					pearlSeenPos = other.position();
				}
			}
		}

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
				boolean pearl = pearlNear(now, track.lastPos);
				boolean weak = false; // health gate disabled: servers often show the last health before the kill, which proves nothing
				Minecraft mc = Minecraft.getInstance();
				boolean near = mc.player != null && mc.player.position().distanceTo(track.lastPos) <= VANISH_MAX_PLAYER_DISTANCE;
				if (since <= VANISH_WINDOW_TICKS && !pearl && !weak && near) {
					ShahedKillMod.LOGGER.info("[kill] id {} vanished {} ticks after the hit (last health {}) -> kill", e.getKey(), since, track.lastHealth);
					kill(level, track.lastPos, track.type);
				} else {
					ShahedKillMod.LOGGER.info("[kill] id {} vanished, ignored (since={}, pearl={}, healthy={}, near={})", e.getKey(), since, pearl, weak, near);
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

			if (entity instanceof LivingEntity living2) {
				float h = living2.getHealth();
				if (track.lastHealth >= 0 && Math.abs(h - track.lastHealth) > 0.01f) {
					track.healthVaried = true;
				}
				track.lastHealth = h;
			}

			// sudden teleport right after our hit (servers that "respawn" the target silently)
			if (center.distanceTo(track.lastPos) > TELEPORT_DISTANCE) {
				it.remove();
				long since = now - track.hitTime;
				boolean pearl = pearlNear(now, track.lastPos);
				boolean weak = false; // health gate disabled: servers often show the last health before the kill, which proves nothing
				if (since <= VANISH_WINDOW_TICKS && !pearl && !weak) {
					ShahedKillMod.LOGGER.info("[kill] id {} teleported {} ticks after the hit (last health {}) -> kill", e.getKey(), since, track.lastHealth);
					kill(level, track.lastPos, track.type);
				} else {
					ShahedKillMod.LOGGER.info("[kill] id {} teleported, ignored (since={}, pearl={}, healthy={})", e.getKey(), since, pearl, weak);
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

	private static boolean pearlNear(long now, Vec3 pos) {
		return now - pearlSeenTime <= PEARL_MEMORY_TICKS && pearlSeenPos.distanceTo(pos) <= PEARL_RADIUS;
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
