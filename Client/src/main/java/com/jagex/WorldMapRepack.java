package com.jagex;

import com.displee.cache.CacheLibrary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Rebuilds world-map entries for regions whose playable map data differs from a clean cache. */
public final class WorldMapRepack {
    private static final int MAPS = 5;
    private static final int GEOGRAPHY = 18;
    private static final int GROUND = 20;

    private WorldMapRepack() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2)
            throw new IllegalArgumentException("Expected <clean-cache> <target-cache>");
        Path cleanPath = Path.of(args[0]).toAbsolutePath().normalize();
        Path targetPath = Path.of(args[1]).toAbsolutePath().normalize();
        if (!Files.isDirectory(cleanPath) || !Files.isDirectory(targetPath))
            throw new IllegalArgumentException("Both cache directories must exist");

        CacheLibrary clean = new CacheLibrary(cleanPath.toString(), false, null);
        CacheLibrary target = new CacheLibrary(targetPath.toString(), false, null);
        try {
            Set<Integer> changedRegions = new LinkedHashSet<>();
            for (int regionId : target.index(MAPS).archiveIds()) {
                if (target.index(MAPS).archive(regionId) == null) continue;
                // Some cache-level map archives (such as archive 0) are not world regions.
                if (target.index(GEOGRAPHY).archive(regionId) == null
                    || target.index(GROUND).archive(regionId) == null) continue;
                if (!same(target.data(MAPS, regionId, 0), clean.data(MAPS, regionId, 0))
                    || !same(target.data(MAPS, regionId, 1), clean.data(MAPS, regionId, 1))) {
                    changedRegions.add(regionId);
                }
            }
            System.out.println("Changed playable-map regions: " + changedRegions);
            WorldMapSync.rebuild(target, changedRegions);
            target.update();
            System.out.println("Rebuilt " + changedRegions.size() + " world-map regions");
        } finally {
            clean.close();
            target.close();
        }
    }

    private static boolean same(byte[] first, byte[] second) {
        return Arrays.equals(first, second);
    }
}
