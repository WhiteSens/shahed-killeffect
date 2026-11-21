package com.shahedkill;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One Shahed flight: a client-only item display entity (using the converted Shahed item model)
 * that is animated with the vanilla display-entity transformation interpolation.
 * It flies in from a distance and falls onto the target point.
 */
public final class ShahedFlight {
	private static final Identifier MODEL = ShahedKillMod.id("shahed");
	private static final Identifier ENGINE_SOUND = ShahedKillMod.id("shahed_engine");
	private static final SoundEvent EXPLOSION_SOUND = SoundEvent.createVariableRangeEvent(ShahedKillMod.id("shahed_explosion"));

	/** The item model is built at half size to fit vanilla element limits; this is the final in-world scale. */
	private static final float MODEL_SCALE = 1.8f;
	/** Blocks per tick while cruising and while diving. */
	private static final double CRUISE_SPEED = 1.5;
	private static final double DIVE_SPEED = 2.2;
	/** Ticks after spawning before the first interpolation is started (the entity needs one tick to get its render state). */
	private static final int START_AGE = 2;

	/** Client-only entities use ids counting down from here so they never clash with server entity ids. */
	private static int nextEntityId = -1_000_000;

	private final Display.ItemDisplay display;
	private final Vec3 start;
	private final Vec3 mid;
	private final Vec3 end;
	private final Quaternionf cruiseRotation;
	private final Quaternionf diveRotation;
	private final int cruiseTicks;
	private final int diveTicks;
	private final EngineSound sound;
	private int age;
	private boolean finished;

	private ShahedFlight(Display.ItemDisplay display, Vec3 start, Vec3 mid, Vec3 end, int cruiseTicks, int diveTicks) {
		this.display = display;
		this.start = start;
		this.mid = mid;
		this.end = end;
		this.cruiseTicks = cruiseTicks;
		this.diveTicks = diveTicks;
		this.cruiseRotation = orientation(mid.subtract(start));
		this.diveRotation = orientation(end.subtract(mid));
		if (Config.volume > 0.001f) {
			this.sound = new EngineSound(this);
			Minecraft.getInstance().getSoundManager().play(this.sound);
			ShahedKillMod.LOGGER.info("Shahed engine sound started (volume {})", Config.volume);
		} else {
			this.sound = null;
		}
	}

	public static ShahedFlight spawn(ClientLevel level, Vec3 impact) {
		double distance = Config.distance;
		double height = Mth.clamp(distance * 0.5, 8.0, 40.0);
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
		double dx = Math.cos(angle) * distance;
		double dz = Math.sin(angle) * distance;

		Vec3 start = impact.add(dx, height, dz);
		// Cruise most of the way in a gentle glide, then fall steeply onto the target.
		Vec3 mid = impact.add(dx * 0.3, height * 0.55, dz * 0.3);

		double speed = Config.speed;
		int cruiseTicks = Math.max(3, (int) Math.round(start.distanceTo(mid) / (CRUISE_SPEED * speed)));
		int diveTicks = Math.max(3, (int) Math.round(mid.distanceTo(impact) / (DIVE_SPEED * speed)));

		Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
		display.setId(nextEntityId--);
		display.setPos(impact.x, impact.y, impact.z);
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.ITEM_MODEL, MODEL);
		display.setItemStack(stack);
		display.setItemTransform(ItemDisplayContext.NONE);
		display.setViewRange(4.0f); // render up to 256 blocks away
		ShahedFlight flight = new ShahedFlight(display, start, mid, impact, cruiseTicks, diveTicks);
		display.setTransformation(flight.transform(start, flight.cruiseRotation));
		level.addEntity(display);
		return flight;
	}

	/** @return false when the flight is over and should be removed. */
	public boolean tick(ClientLevel level) {
		if (finished || display.level() != level) {
			discard();
			return false;
		}
		age++;

		if (age == START_AGE) {
			display.setTransformationInterpolationDelay(1);
			display.setTransformationInterpolationDuration(cruiseTicks);
			display.setTransformation(transform(mid, cruiseRotation));
		} else if (age == START_AGE + 1 + cruiseTicks) {
			display.setTransformationInterpolationDelay(0);
			display.setTransformationInterpolationDuration(diveTicks);
			display.setTransformation(transform(end, diveRotation));
		} else if (age >= START_AGE + 2 + cruiseTicks + diveTicks) {
			// impact: explosion sound, the drone disappears
			level.playLocalSound(end.x, end.y, end.z, EXPLOSION_SOUND, SoundSource.NEUTRAL,
					Math.max(0.05f, Config.volume), 0.95f + level.getRandom().nextFloat() * 0.1f, false);
			KillEffects.explode(level, end);
			discard();
			return false;
		}
		return true;
	}

	public void discard() {
		finished = true;
		if (!display.isRemoved()) {
			display.discard();
		}
		if (display.level() instanceof ClientLevel level) {
			level.removeEntity(display.getId(), Entity.RemovalReason.DISCARDED);
		}
	}

	/** Current approximate drone position in the world (used to move the engine sound). */
	Vec3 currentPosition() {
		int t = age - (START_AGE + 1);
		if (t <= 0) {
			return start;
		}
		if (t < cruiseTicks) {
			return start.lerp(mid, (double) t / cruiseTicks);
		}
		t -= cruiseTicks;
		if (t < diveTicks) {
			return mid.lerp(end, (double) t / diveTicks);
		}
		return end;
	}

	boolean isFinished() {
		return finished;
	}

	/** Display transformation placing the model at {@code position} (translation is relative to the entity, which sits on the impact point). */
	private Transformation transform(Vec3 position, Quaternionf rotation) {
		Vec3 offset = position.subtract(end);
		return new Transformation(
				new Vector3f((float) offset.x, (float) offset.y, (float) offset.z),
				new Quaternionf(rotation),
				new Vector3f(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE),
				new Quaternionf());
	}

	/** Rotation that points the model's nose (-Z) along {@code direction}. */
	private static Quaternionf orientation(Vec3 direction) {
		Vec3 d = direction.normalize();
		float yaw = (float) Math.atan2(-d.x, -d.z);
		float pitch = (float) Math.asin(Mth.clamp(d.y, -1.0, 1.0));
		return new Quaternionf().rotateY(yaw).rotateX(pitch);
	}

	/** Looping engine sound that follows the drone. */
	private static final class EngineSound extends AbstractTickableSoundInstance {
		private final ShahedFlight flight;

		EngineSound(ShahedFlight flight) {
			super(SoundEvent.createVariableRangeEvent(ENGINE_SOUND), SoundSource.NEUTRAL, flight.display.level().getRandom());
			this.flight = flight;
			this.looping = true;
			// audible range comes from "attenuation_distance" in sounds.json
			this.volume = Math.max(0.0001f, Config.volume);
			Vec3 p = flight.start;
			this.x = p.x;
			this.y = p.y;
			this.z = p.z;
		}

		@Override
		public void tick() {
			if (flight.isFinished()) {
				stop();
				return;
			}
			Vec3 p = flight.currentPosition();
			this.x = p.x;
			this.y = p.y;
			this.z = p.z;
		}
	}
}
