package com.jagex;

import com.displee.cache.CacheLibrary;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Builds revision-241 world-map data from the map files staged in index 5. */
final class WorldMapSync {
    private static final int MAPS = 5, CONFIGS = 2, GEOGRAPHY = 18, GROUND = 20;

    static void rebuild(CacheLibrary cache, Set<Integer> regions) throws IOException {
        if (regions.isEmpty()) return;
        Map<Integer, FloorColour> underlays = underlayColours(cache);
        Map<Integer, Terrain> terrainCache = new HashMap<>();
        for (int regionId : regions) {
            if (cache.index(GEOGRAPHY).archive(regionId) == null
                || cache.index(GROUND).archive(regionId) == null) continue;
            byte[] terrainData = cache.data(MAPS, regionId, 0);
            if (terrainData == null) throw new IOException("Missing terrain for region " + regionId);
            Terrain terrain = decodeTerrain(terrainData);
            terrainCache.put(regionId, terrain);
            byte[] objectData = cache.data(MAPS, regionId, 1);
            List<Location> locations = objectData == null ? List.of() : decodeLocations(objectData);
            byte[] geography = encodeGeography(terrain, locations);
            validateGeography(geography);
            cache.put(GEOGRAPHY, regionId, 0, geography);
            cache.put(GROUND, regionId, 0,
                encodeGround(cache, regionId, terrainCache, underlays));
        }
    }

    private static Terrain decodeTerrain(byte[] data) throws IOException {
        Cursor in = new Cursor(data);
        Terrain terrain = new Terrain();
        for (int plane = 0; plane < 4; plane++) for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) {
            Tile tile = terrain.tiles[plane][x][y] = new Tile();
            while (true) {
                int attribute = in.u16();
                if (attribute == 0) break;
                if (attribute == 1) { in.u8(); break; }
                if (attribute <= 49) {
                    tile.overlay = in.u16();
                    tile.shape = (attribute - 2) / 4;
                    tile.rotation = attribute - 2 & 3;
                } else if (attribute <= 81) tile.settings = attribute - 49;
                else tile.underlay = attribute - 81;
            }
        }
        return terrain;
    }

    private static List<Location> decodeLocations(byte[] data) throws IOException {
        Cursor in = new Cursor(data);
        List<Location> result = new ArrayList<>();
        int id = -1;
        while (true) {
            int idDelta = in.smartCompat();
            if (idDelta == 0) return result;
            id += idDelta;
            int position = 0;
            while (true) {
                int delta = in.ushortSmart();
                if (delta == 0) break;
                position += delta - 1;
                int attributes = in.u8();
                result.add(new Location(id, position >>> 12 & 3, position >>> 6 & 63,
                    position & 63, attributes >>> 2, attributes & 3));
            }
        }
    }

    private static byte[] encodeGeography(Terrain terrain, List<Location> locations) throws IOException {
        Map<Integer, List<Location>> byTile = new HashMap<>();
        for (Location location : locations) byTile.computeIfAbsent(location.key(), ignored -> new ArrayList<>()).add(location);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(32_768);
        DataOutputStream out = new DataOutputStream(bytes);
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) {
            int highest = 0;
            boolean hasOverlays = false, hasDecorations = false;
            for (int plane = 0; plane < 4; plane++) {
                Tile tile = terrain.tiles[plane][x][y];
                boolean decorated = byTile.containsKey(key(plane, x, y));
                hasOverlays |= tile.overlay != 0;
                hasDecorations |= decorated;
                if (tile.overlay != 0 || tile.underlay != 0 || decorated) highest = plane;
            }
            Tile ground = terrain.tiles[0][x][y];
            if (!hasDecorations && highest == 0) {
                out.writeByte(1 | (ground.overlay == 0 ? 0 : 2));
                if (ground.overlay != 0) out.writeShort(ground.overlay);
                out.writeShort(ground.underlay);
                continue;
            }
            out.writeByte(highest << 3 | (hasOverlays ? 2 : 0) | (hasDecorations ? 4 : 0));
            out.writeShort(ground.underlay);
            if (hasOverlays) {
                out.writeByte(highest + 1);
                for (int plane = 0; plane <= highest; plane++) {
                    Tile tile = terrain.tiles[plane][x][y];
                    out.writeShort(tile.overlay);
                    if (tile.overlay != 0) out.writeByte(tile.shape << 2 | tile.rotation);
                }
            }
            if (hasDecorations) for (int plane = 0; plane <= highest; plane++) {
                List<Location> values = byTile.getOrDefault(key(plane, x, y), List.of());
                out.writeByte(values.size());
                for (Location location : values) {
                    // Revision 241 uses a fixed-width loc id in world-map geography.
                    // The older 2/4-byte big-smart shifts the tile stream for small ids.
                    out.writeInt(location.id);
                    out.writeByte(location.type << 2 | location.rotation);
                }
            }
        }
        return bytes.toByteArray();
    }

    private static void validateGeography(byte[] data) throws IOException {
        Cursor in = new Cursor(data);
        for (int tile = 0; tile < 4096; tile++) {
            int flag = in.u8();
            if (flag == 0) continue;
            if ((flag & 1) != 0) {
                if ((flag & 2) != 0) in.u16();
                in.u16();
                continue;
            }
            int planes = ((flag & 24) >>> 3) + 1;
            in.u16();
            if ((flag & 2) != 0) {
                int count = in.u8();
                if (count > 4) throw new IOException("Invalid overlay plane count " + count);
                for (int plane = 0; plane < count; plane++) if (in.u16() != 0) in.u8();
            }
            if ((flag & 4) != 0) for (int plane = 0; plane < planes; plane++) {
                int count = in.u8();
                for (int i = 0; i < count; i++) { in.u32(); in.u8(); }
            }
        }
        if (in.offset != data.length) throw new IOException("Trailing world-map geography bytes");
    }

    private static byte[] encodeGround(CacheLibrary cache, int regionId,
                                       Map<Integer, Terrain> terrainCache,
                                       Map<Integer, FloorColour> underlays) throws IOException {
        Terrain terrain = terrainCache.get(regionId);
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) {
            Tile tile = terrain.tiles[0][x][y];
            int rgb = blendedUnderlay(cache, regionId, x, y, terrainCache, underlays);
            image.setRGB(x, 63 - y, new Color(rgb).getRGB());
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder unavailable");
        return output.toByteArray();
    }

    /** Matches the client's five-tile floor-colour averaging and includes adjacent regions. */
    private static int blendedUnderlay(CacheLibrary cache, int regionId, int tileX, int tileY,
                                       Map<Integer, Terrain> terrainCache,
                                       Map<Integer, FloorColour> underlays) throws IOException {
        int hue = 0, saturation = 0, lightness = 0, count = 0;
        int regionX = regionId >>> 8, regionY = regionId & 255;
        for (int dx = -5; dx <= 5; dx++) for (int dy = -5; dy <= 5; dy++) {
            int x = tileX + dx, y = tileY + dy;
            int sampleRegionX = regionX + Math.floorDiv(x, 64);
            int sampleRegionY = regionY + Math.floorDiv(y, 64);
            int sampleRegion = sampleRegionX << 8 | sampleRegionY & 255;
            Terrain sample = terrain(cache, sampleRegion, terrainCache);
            if (sample == null) continue;
            Tile sampleTile = sample.tiles[0][Math.floorMod(x, 64)][Math.floorMod(y, 64)];
            FloorColour colour = underlays.get(sampleTile.underlay - 1);
            if (colour == null) continue;
            hue += colour.weightedHue;
            saturation += colour.saturation;
            lightness += colour.lightness;
            count++;
        }
        if (count == 0) return 0x3f3f3f;
        return hslToRgb(hue / count, saturation / count, lightness / count);
    }

    private static Terrain terrain(CacheLibrary cache, int regionId,
                                   Map<Integer, Terrain> terrainCache) throws IOException {
        if (terrainCache.containsKey(regionId)) return terrainCache.get(regionId);
        byte[] data = cache.data(MAPS, regionId, 0);
        Terrain result = data == null ? null : decodeTerrain(data);
        terrainCache.put(regionId, result);
        return result;
    }

    private static Map<Integer, FloorColour> underlayColours(CacheLibrary cache) {
        Map<Integer, FloorColour> result = new HashMap<>();
        for (var entry : colours(cache, 1).entrySet())
            result.put(entry.getKey(), FloorColour.fromRgb(entry.getValue()));
        return result;
    }

    private static int hslToRgb(int hue, int saturation, int lightness) {
        double h = (hue & 255) / 256.0;
        double s = Math.max(0, Math.min(255, saturation)) / 256.0;
        double l = Math.max(0, Math.min(255, lightness)) / 256.0;
        double r, g, b;
        if (s == 0) r = g = b = l;
        else {
            double q = l < .5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3.0);
        }
        return (int) Math.round(r * 255) << 16
            | (int) Math.round(g * 255) << 8 | (int) Math.round(b * 255);
    }

    private static double hueToRgb(double p, double q, double value) {
        if (value < 0) value++;
        if (value > 1) value--;
        if (value < 1.0 / 6.0) return p + (q - p) * 6 * value;
        if (value < .5) return q;
        if (value < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - value) * 6;
        return p;
    }

    private static Map<Integer, Integer> colours(CacheLibrary cache, int archiveId) {
        Map<Integer, Integer> result = new HashMap<>();
        var archive = cache.index(CONFIGS).archive(archiveId);
        if (archive == null) return result;
        for (int file : archive.fileIds()) {
            byte[] data = cache.data(CONFIGS, archiveId, file);
            if (data == null) continue;
            int offset = 0, rgb = -1, secondaryRgb = -1;
            while (offset < data.length) {
                int opcode = data[offset++] & 255;
                if (opcode == 0) break;
                if ((opcode == 1 || opcode == 7) && offset + 3 <= data.length) {
                    int colour = (data[offset] & 255) << 16
                        | (data[offset + 1] & 255) << 8 | data[offset + 2] & 255;
                    if (opcode == 1) rgb = colour;
                    else secondaryRgb = colour;
                    offset += 3;
                } else if (opcode == 2) offset++;
                else if (opcode != 5 && opcode != 8) break;
            }
            // Secondary colour is what the map renderer uses for water and several cliffs.
            int mapRgb = secondaryRgb != -1 ? secondaryRgb : rgb;
            if (mapRgb != -1) result.put(file, mapRgb);
        }
        return result;
    }

    private static int key(int plane, int x, int y) { return plane << 12 | x << 6 | y; }
    private static final class Terrain { final Tile[][][] tiles = new Tile[4][64][64]; }
    private static final class Tile { int underlay, overlay, shape, rotation, settings; }
    private record Location(int id, int plane, int x, int y, int type, int rotation) {
        int key() { return WorldMapSync.key(plane, x, y); }
    }
    private record FloorColour(int weightedHue, int saturation, int lightness) {
        static FloorColour fromRgb(int rgb) {
            double red = (rgb >> 16 & 255) / 256.0;
            double green = (rgb >> 8 & 255) / 256.0;
            double blue = (rgb & 255) / 256.0;
            double min = Math.min(red, Math.min(green, blue));
            double max = Math.max(red, Math.max(green, blue));
            double lightness = (max + min) / 2.0;
            double saturation = 0, hue = 0;
            if (min != max) {
                saturation = lightness < .5 ? (max - min) / (max + min)
                    : (max - min) / (2.0 - max - min);
                if (max == red) hue = (green - blue) / (max - min);
                else if (max == green) hue = 2 + (blue - red) / (max - min);
                else hue = 4 + (red - green) / (max - min);
            }
            hue /= 6.0;
            if (hue < 0) hue++;
            int multiplier = (int) (saturation * (lightness > .5 ? 1 - lightness : lightness) * 512);
            multiplier = Math.max(1, multiplier);
            return new FloorColour((int) (hue * multiplier) * 256 / multiplier,
                clamp((int) (saturation * 256)), clamp((int) (lightness * 256)));
        }

        private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }
    }
    private static final class Cursor {
        final byte[] data; int offset;
        Cursor(byte[] data) { this.data = data; }
        int u8() throws IOException { require(1); return data[offset++] & 255; }
        int u16() throws IOException { require(2); return (data[offset++] & 255) << 8 | data[offset++] & 255; }
        int ushortSmart() throws IOException { require(1); return (data[offset] & 255) < 128 ? u8() : u16() - 32_768; }
        int smartCompat() throws IOException { int total = 0, value; do { value = ushortSmart(); total += value; } while (value == 32_767); return total; }
        int u32() throws IOException { require(4); return (data[offset++] & 255) << 24 | (data[offset++] & 255) << 16 | (data[offset++] & 255) << 8 | data[offset++] & 255; }
        void require(int count) throws IOException { if (offset + count > data.length) throw new IOException("Truncated data at " + offset); }
    }
}
