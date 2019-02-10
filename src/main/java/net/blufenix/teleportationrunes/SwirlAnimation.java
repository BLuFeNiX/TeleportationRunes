package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class SwirlAnimation {

    private int duration = 20; // ticks
    private int segments;
    private double radius;
    private boolean useFakeTicks;
    private Location location;
    private World world;
    private Particle[] particles;
    private int numParticles;
    private boolean repeat;

    private double degreesPerTick;
    private int lastElapsedTicks = -1;

    private int totalRotationsDegrees = 360;

    private Vector[][] compiledVectors;

    private static Vector[][] defaultCompiledVectors;

    public static SwirlAnimation getDefault() {

        SwirlAnimation anim = new SwirlAnimation()
                .setDuration(20*3)
                .setParticle(Particle.SPELL_WITCH)
                .setRadius(5)
                .setSegments(3)
                .setNumParticles(10)
                .enableFakeTicks(true)
                .enableRepeat(true)
                .setRotations(2);

        if (defaultCompiledVectors != null) {
            anim.setCompiledVectors(defaultCompiledVectors);
        } else {
            defaultCompiledVectors = SwirlAnimation.compile(anim);
        }

        return anim;
    }

    public SwirlAnimation setDuration(int duration) {
        this.duration = duration;
        this.degreesPerTick = (double) totalRotationsDegrees/duration;
        return this;
    }

    public SwirlAnimation setSegments(int segments) {
        this.segments = segments;
        return this;
    }

    public SwirlAnimation setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public SwirlAnimation enableFakeTicks(boolean useFakeTicks) {
        this.useFakeTicks = useFakeTicks;
        return this;
    }

    public SwirlAnimation setLocation(Location location) {
        this.location = location;
        this.world  = location.getWorld();
        return this;
    }

    public SwirlAnimation setParticle(Particle... particles) {
        this.particles = particles;
        return this;
    }

    public SwirlAnimation setNumParticles(int numParticles) {
        this.numParticles = numParticles;
        return this;
    }

    public SwirlAnimation setRotations(double rotations) {
        this.totalRotationsDegrees = (int) (360 * rotations);
        this.degreesPerTick = (double) totalRotationsDegrees/duration;
        return this;
    }

    public SwirlAnimation enableRepeat(boolean repeat) {
        this.repeat = repeat;
        return this;
    }

    public void update(int elapsedTicks) {
        if (repeat && elapsedTicks > duration) {
            elapsedTicks %= duration;
            lastElapsedTicks = -1;
        } else if (elapsedTicks > duration) {
            Log.e("animation finished. not ticking!");
            return;
        }

        if (useFakeTicks) {
            for (int fakeTicks = lastElapsedTicks + 1; fakeTicks <= elapsedTicks; fakeTicks++) {
                onTick(fakeTicks);
            }
        } else {
            onTick(elapsedTicks);
        }

        lastElapsedTicks = elapsedTicks;
    }

    private void onTick(int tick) {
        for (int segment = 0; segment < compiledVectors[tick].length; segment++) {
            for (Particle p : particles) {
                world.spawnParticle(p, location.clone().add(compiledVectors[tick][segment]), numParticles, null);
            }
        }
    }

    public static Vector[][] compile(SwirlAnimation anim) {
        Vector[][] compiledVectors = new Vector[anim.duration][];
        for (int tick = 0; tick < anim.duration; tick++) {
            compiledVectors[tick] = compileTick(anim, tick);
        }
        anim.compiledVectors = compiledVectors;
        return compiledVectors;
    }

    private static Vector[] compileTick(SwirlAnimation anim, int tick) {
        Vector[] vectors = new Vector[anim.segments];
        for (int segment = 0; segment < anim.segments; segment++) {
            double segmentOffset = ((double)360/anim.segments) * segment;
            double radians = Math.toRadians(((double) tick * anim.degreesPerTick) + segmentOffset);
            double r = anim.radius * (1 - ((double) tick / anim.duration));
            double xPos = r * Math.cos(radians);
            double zPos = r * Math.sin(radians);
            vectors[segment] = new Vector(xPos, 0, zPos);
        }
        return vectors;
    }

    public void setCompiledVectors(Vector[][] compiledVectors) {
        this.compiledVectors = compiledVectors;
    }
}
