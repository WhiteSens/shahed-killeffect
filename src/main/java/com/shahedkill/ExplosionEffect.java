package com.shahedkill;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Visual explosion after the Shahed hits: flash and fireball, radial debris, a rising black smoke
 * column that ends in a mushroom cap, and fire burning on the ground for a few seconds.
 * Built only from vanilla particles.
 */
public final class ExplosionEffect {
	private static final int DURATION = 140;

	private final double x;
	private final double y;
	private final double z;
	private final ParticleOptions groundDebris;
	private final ParticleOptions blackDebris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COAL_BLOCK.defaultBlockState());
	private int age;

	public ExplosionEffect(ClientLevel level, Vec3 impact) {
		this.x = impact.x;
		this.z = impact.z;
		// find the ground below the impact point
		double ground = impact.y - 1.0;
		BlockState groundState = Blocks.DIRT.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(impact.x), Math.floor(impact.y), Math.floor(impact.z));
		for (int i = 0; i < 6; i++) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				ground = pos.getY() + 1.0;
				groundState = state;
				break;
			}
			pos.move(0, -1, 0);
		}
		this.y = ground;
		this.groundDebris = new BlockParticleOption(ParticleTypes.BLOCK, groundState);
	}

	/** @return false when the effect has finished. */
	public boolean tick(ClientLevel level) {
		RandomSource r = level.getRandom();
		if (age == 0) {
			burst(level, r);
		}
		if (age <= 10) {
			fireball(level, r);
		}
		if (age >= 8 && age <= 22) {
			mushroomCap(level, r);
		}
		if (age < 75) {
			column(level, r);
		}
		groundFire(level, r);
		age++;
		return age < DURATION;
	}

	private void burst(ClientLevel level, RandomSource r) {
		level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0, z, 0, 0, 0);
		for (int i = 0; i < 20; i++) {
			level.addParticle(ParticleTypes.EXPLOSION, x + r.nextGaussian() * 1.5, y + 0.5 + r.nextDouble() * 2.0, z + r.nextGaussian() * 1.5, 0, 0, 0);
		}
		for (int i = 0; i < 45; i++) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double s = 0.3 + r.nextDouble() * 0.5;
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, Math.cos(a) * s, 0.04 + r.nextDouble() * 0.08, Math.sin(a) * s);
		}
		// debris thrown outwards: ground chunks (radial streaks) and black bits in high arcs
		for (int i = 0; i < 70; i++) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double s = 0.4 + r.nextDouble() * 0.8;
			level.addParticle(groundDebris, x, y + 0.3, z, Math.cos(a) * s, 0.3 + r.nextDouble() * 0.9, Math.sin(a) * s);
		}
		for (int i = 0; i < 30; i++) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double s = 0.1 + r.nextDouble() * 0.4;
			level.addParticle(blackDebris, x, y + 0.5, z, Math.cos(a) * s, 0.8 + r.nextDouble() * 0.9, Math.sin(a) * s);
		}
		for (int i = 0; i < 30; i++) {
			level.addParticle(ParticleTypes.LAVA, x + r.nextGaussian() * 0.5, y + 0.5, z + r.nextGaussian() * 0.5,
					r.nextGaussian() * 0.2, 0.3 + r.nextDouble() * 0.4, r.nextGaussian() * 0.2);
		}
		for (int i = 0; i < 30; i++) {
			level.addParticle(ParticleTypes.FLAME, x + r.nextGaussian() * 0.8, y + 0.3 + r.nextDouble(), z + r.nextGaussian() * 0.8,
					r.nextGaussian() * 0.15, 0.1 + r.nextDouble() * 0.2, r.nextGaussian() * 0.15);
		}
	}

	private void fireball(ClientLevel level, RandomSource r) {
		double h = y + 1.5 + age * 0.5;
		for (int i = 0; i < 6; i++) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, x + r.nextGaussian() * 1.2, h + r.nextGaussian() * 0.8, z + r.nextGaussian() * 1.2, 0, 0.05, 0);
		}
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.FLAME, x + r.nextGaussian() * 1.0, h + r.nextGaussian() * 0.6, z + r.nextGaussian() * 1.0,
					r.nextGaussian() * 0.04, 0.05, r.nextGaussian() * 0.04);
		}
		level.addParticle(ParticleTypes.LAVA, x + r.nextGaussian() * 0.8, h, z + r.nextGaussian() * 0.8, 0, 0.1, 0);
	}

	private void mushroomCap(ClientLevel level, RandomSource r) {
		double h = y + 7.0 + (age - 8) * 0.25;
		for (int i = 0; i < 8; i++) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double s = 0.1 + r.nextDouble() * 0.15;
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, h, z, Math.cos(a) * s, 0.02, Math.sin(a) * s);
		}
	}

	private void column(ClientLevel level, RandomSource r) {
		level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x + r.nextGaussian() * 0.5, y + 0.5, z + r.nextGaussian() * 0.5, 0, 0.07, 0);
		level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x + r.nextGaussian() * 0.5, y + 0.5, z + r.nextGaussian() * 0.5, 0, 0.07, 0);
		if (age < 40) {
			for (int i = 0; i < 3; i++) {
				level.addParticle(ParticleTypes.LARGE_SMOKE, x + r.nextGaussian() * 0.8, y + 0.5, z + r.nextGaussian() * 0.8,
						r.nextGaussian() * 0.03, 0.15 + r.nextDouble() * 0.15, r.nextGaussian() * 0.03);
			}
		}
		if (age % 3 == 0 && age < 60) {
			level.addParticle(blackDebris, x + r.nextGaussian() * 1.0, y + 1.0, z + r.nextGaussian() * 1.0,
					r.nextGaussian() * 0.15, 0.5 + r.nextDouble() * 0.5, r.nextGaussian() * 0.15);
		}
	}

	private void groundFire(ClientLevel level, RandomSource r) {
		int flames = age < 90 ? 5 : 2;
		for (int i = 0; i < flames; i++) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double d = Math.sqrt(r.nextDouble()) * (age < 6 ? 2.0 : 3.5);
			level.addParticle(ParticleTypes.FLAME, x + Math.cos(a) * d, y + 0.1, z + Math.sin(a) * d, 0, 0.01 + r.nextDouble() * 0.03, 0);
		}
		if (age % 4 == 0 && age < 110) {
			double a = r.nextDouble() * Math.PI * 2.0;
			double d = r.nextDouble() * 3.0;
			level.addParticle(ParticleTypes.LARGE_SMOKE, x + Math.cos(a) * d, y + 0.3, z + Math.sin(a) * d, 0, 0.04, 0);
		}
	}
}
