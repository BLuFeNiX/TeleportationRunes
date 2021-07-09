package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class SwirlAnimation {

    private int duration = 20; // ticks
    private int cycles = 1; // how many times the animation repeats
    private int cycleDuration; // ticks
    private int frames = 600; // enough frames for 30 seconds of smooth animation
    private int segments;
    private double radius;
    private double rotations;
    private boolean useFakeTicks;
    private Location location;
    private World world;
    private Particle[] particles;
    private int numParticles;
    private boolean repeat;
    private Entity trackedEntity = null;

    private double degreesPerFrame;
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
                .setRotations(2)
                .setCycles(1);

        if (defaultCompiledVectors != null) {
            anim.setCompiledVectors(defaultCompiledVectors);
        } else {
            defaultCompiledVectors = SwirlAnimation.compile(anim);
        }

        return anim;
    }

    public SwirlAnimation clone() {
        return new SwirlAnimation()
                .setParticle(particles)
                .setRadius(radius)
                .setSegments(segments)
                .setNumParticles(numParticles)
                .enableFakeTicks(useFakeTicks)
                .enableRepeat(repeat)
                .setRotations(rotations)
                .setCycles(cycles)
                .setCompiledVectors(getCompiledVectors())
                .setDuration(duration);
    }

    public SwirlAnimation setDuration(int duration) {
        this.duration = duration;
        this.cycleDuration = duration/cycles;
        return this;
    }

    public SwirlAnimation setCycles(int cycles) {
        this.cycles = cycles;
        this.cycleDuration = duration/cycles;
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

    public SwirlAnimation setLocation(Entity trackedEntity) {
        this.trackedEntity = trackedEntity;
        this.world  = trackedEntity.getLocation().getWorld();
        return this;
    }

    private Location getBaseAnimationLocation() {
        return trackedEntity != null ? trackedEntity.getLocation().clone() : location.clone();
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
        this.rotations = rotations;
        this.totalRotationsDegrees = (int) (360 * rotations);
        this.degreesPerFrame = (double) totalRotationsDegrees/frames;
        return this;
    }

    public SwirlAnimation enableRepeat(boolean repeat) {
        this.repeat = repeat;
        return this;
    }

    public void update(int elapsedTicks) {
        if (repeat && elapsedTicks > cycleDuration) {
            elapsedTicks %= cycleDuration;
            lastElapsedTicks = -1;
        } else if (elapsedTicks > cycleDuration) {
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
        int frameIdx = (frames / cycleDuration) * tick;
        for (int segment = 0; segment < compiledVectors[frameIdx].length; segment++) {
            for (Particle p : particles) {
                world.spawnParticle(p, getBaseAnimationLocation().add(compiledVectors[frameIdx][segment]), numParticles, null);
            }
        }
    }

    public SwirlAnimation compile() {
        return this.setCompiledVectors(compile(this));
    }

    public static Vector[][] compile(SwirlAnimation anim) {
        Vector[][] compiledVectors = new Vector[anim.frames][];
        for (int frame = 0; frame < anim.frames; frame++) {
            compiledVectors[frame] = compileFrame(anim, frame);
        }
        anim.compiledVectors = compiledVectors;
        return compiledVectors;
    }

    private static Vector[] compileFrame(SwirlAnimation anim, int frame) {
        Vector[] vectors = new Vector[anim.segments];
        for (int segment = 0; segment < anim.segments; segment++) {
            double segmentOffset = ((double)360/anim.segments) * segment;
            double radians = Math.toRadians(((double) frame * anim.degreesPerFrame) + segmentOffset);
            double r = anim.radius * (1 - ((double) frame / anim.frames));
            double xPos = r * Math.cos(radians);
            double zPos = r * Math.sin(radians);
            vectors[segment] = new Vector(xPos, 0, zPos);
        }
        return vectors;
    }

    public SwirlAnimation setCompiledVectors(Vector[][] compiledVectors) {
        this.compiledVectors = compiledVectors;
        return this;
    }

    public Vector[][] getCompiledVectors() {
        return compiledVectors;
    }
}
