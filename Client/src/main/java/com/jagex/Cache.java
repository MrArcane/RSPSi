package com.jagex;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;

import com.jagex.cache.graphics.Sprite;
import com.jagex.net.ResourceProvider;
import com.rspsi.cache.CacheFileType;
import com.rspsi.core.misc.FixedIntegerKeyMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class Cache {


    @Setter
    /**
     * Set this to override how all cache files except maps are loaded
     * args = [fileType, fileId]
     * return byte[] or Optional.empty() to continue with normal loading
     */
    private BiFunction<CacheFileType, Integer, Optional<byte[]>> fileRetrieverOverride;

    @Setter
    /**
     * Set this to override how the map files are loaded
     * args = [fileId, regionId]
     * return byte[] or Optional.empty() to continue with normal loading
     */
    private BiFunction<Integer, Integer, Optional<byte[]>> mapRetrieverOverride;


    @Getter
    private CacheLibrary indexedFileSystem;

    private Index modelArchive, mapArchive, configArchive, skeletonArchive, skinArchive, spriteIndex, textureIndex, spotAnimIndex, varbitIndex, locIndex;
    /** OpenRune's filesystem correctly splits revision-241 multi-file map groups. */
    private dev.openrune.filesystem.Cache openRuneCache;
    private final Path cachePath;

    private boolean isCacheNewOSRS(CacheLibrary library) {
        Index idx = library.index(2);
        int indexCount = ArrayUtils.indexOf(library.indices(), null) - 1;
        return idx.getRevision() >= 300 && indexCount <= 24;
    }

    public Cache(Path path) {
        cachePath = path.toAbsolutePath().normalize();
        log.info("Loading cache at {}", path);
        indexedFileSystem = new CacheLibrary(path.toFile().toString(), false, null);
        if (indexedFileSystem.is317()) {
            modelArchive = indexedFileSystem.index(1);
            mapArchive = indexedFileSystem.index(4);
            configArchive = indexedFileSystem.index(0);
            skinArchive = indexedFileSystem.index(2);
            skeletonArchive = null;//317 loads inside skins
            log.info("Loaded cache in 317 format!");
        } else if (isCacheNewOSRS(indexedFileSystem)) {
            modelArchive = indexedFileSystem.index(7);
            mapArchive = indexedFileSystem.index(5);
            configArchive = indexedFileSystem.index(2);
            skeletonArchive = indexedFileSystem.index(0);
            skinArchive = indexedFileSystem.index(1);
            spriteIndex = indexedFileSystem.index(8);
            textureIndex = indexedFileSystem.index(9);
            log.info("Loaded cache in OSRS format!");
            openRuneCache = dev.openrune.filesystem.Cache.Companion.load(path);
        } else if (indexedFileSystem.isRS3()) {
            modelArchive = indexedFileSystem.index(7);
            mapArchive = indexedFileSystem.index(5);
            configArchive = indexedFileSystem.index(2);
            skeletonArchive = indexedFileSystem.index(0);
            skinArchive = indexedFileSystem.index(1);
            spriteIndex = indexedFileSystem.index(8);
            textureIndex = indexedFileSystem.index(9);
            spotAnimIndex = indexedFileSystem.index(21);
            varbitIndex = indexedFileSystem.index(22);
            locIndex = indexedFileSystem.index(16);
            log.info("Loaded cache in RS3 format!");
        } else if (indexedFileSystem.isRS3()) {
            throw new UnsupportedOperationException("Cache format not supported!");
        }
        resourceProvider = new ResourceProvider(this);
        Thread t = new Thread(resourceProvider);
        t.start();
    }

    public ResourceProvider resourceProvider;


    private FixedIntegerKeyMap<Sprite> spriteCache = new FixedIntegerKeyMap<Sprite>(100);


    public Sprite getSprite(int id) {
        if (spriteCache.contains(id))
            return spriteCache.get(id);
        if (!isCacheNewOSRS(indexedFileSystem))
            throw new RuntimeException("Cannot grab sprite by ID on 317!");
        Sprite sprite = Sprite.decode(ByteBuffer.wrap(spriteIndex.archive(id).file(0).getData()));
        spriteCache.put(id, sprite);
        System.out.println("GETSPRITE " + id);
        return sprite;
    }

    public final Index getFile(CacheFileType index) {
        try {
            switch (index) {
                case CONFIG:
                    return configArchive;
                case MODEL:
                    return modelArchive;
                case ANIMATION:
                    return skinArchive;
                case SKELETON:
                    return skeletonArchive;
                case SOUND:
                    break;
                case MAP:
                    return mapArchive;
                case SPRITE:
                    return spriteIndex;
                case TEXTURE:
                    return textureIndex;
                case SPOT:
                    return spotAnimIndex;
                case VARBIT:
                    return varbitIndex;
                case LOC:
                    return locIndex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public final synchronized byte[] readMap(int groupId, int fileId, int regionId) {
        if (mapRetrieverOverride != null) {
            Optional<byte[]> data = mapRetrieverOverride.apply(groupId, regionId);
            if (data.isPresent())
                return data.get();
        }
        if (indexedFileSystem.is317())
            return mapArchive.archive(groupId).file(0).getData();
        if (openRuneCache != null) {
            byte[] data = openRuneCache.data(5, groupId, fileId, null);
            if (data != null)
                return data;
        }
        return mapArchive.archive(groupId).file(fileId).getData();
    }

    /** Stages one uncompressed map-group file for the next cache update. */
    public final synchronized void writeMap(int groupId, int fileId, byte[] data) {
        if (groupId < 0)
            throw new IllegalArgumentException("Map group ID must be non-negative");
        if (fileId != 0 && fileId != 1)
            throw new IllegalArgumentException("Map file ID must be 0 (terrain) or 1 (objects)");
        indexedFileSystem.put(5, groupId, fileId, data);
    }

    /** Writes staged cache changes to disk and refreshes the revision-241 reader. */
    public final synchronized void save() throws IOException {
        if (openRuneCache != null) {
            openRuneCache.close();
            openRuneCache = null;
        }
        try {
            indexedFileSystem.update();
        } finally {
            if (indexedFileSystem.isOSRS())
                openRuneCache = dev.openrune.filesystem.Cache.Companion.load(cachePath);
        }
    }

    public final synchronized byte[] getFile(CacheFileType type, int file) {
        try {
            if (fileRetrieverOverride != null) {
                Optional<byte[]> data = fileRetrieverOverride.apply(type, file);
                if (data.isPresent())
                    return data.get();
            }
            switch (type) {
                case CONFIG:
                    return configArchive.archive(file).file(0).getData();
                case MODEL:
                    if (openRuneCache != null) {
                        byte[] data = openRuneCache.data(7, file, 0, null);
                        if (data != null) return data;
                    }
                    return modelArchive.archive(file).file(0).getData();
                case ANIMATION:
                    if (openRuneCache != null) {
                        byte[] data = openRuneCache.data(1, file, 0, null);
                        if (data != null) return data;
                    }
                    return skinArchive.archive(file).file(0).getData();
                case SKELETON:
                    if (openRuneCache != null) {
                        byte[] data = openRuneCache.data(0, file, 0, null);
                        if (data != null) return data;
                    }
                    return skeletonArchive.archive(file).file(0).getData();
                case SOUND:
                    break;
                case MAP:
                    return mapArchive.archive(file).file(0).getData();
                case TEXTURE:
                    break;
                case SPOT:
                    return spotAnimIndex.archive(file >>> 8).file(file & 0xff).getData();
                case VARBIT:
                    return varbitIndex.archive(file >>> 1416501898).file(file & 0x3ffff).getData();
                case LOC:
                    return locIndex.archive(file >> 8).file((file) & (1 << 8) - 1).getData();
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        return null;
    }


    public final File writegetFile(CacheFileType index, String name, int file, byte[] data) {
        try {
            switch (index) {
                case CONFIG:
                   // configArchive.createIfNotExist(file);
                    return configArchive.archive(file).add(0, data);
                case MODEL:
                   // modelArchive.createIfNotExist(file);
                    return modelArchive.archive(file).add(0, data);
                case ANIMATION:
                    //skinArchive.createIfNotExist(file);
                    return skinArchive.archive(file).add(0, data);
                case SOUND:
                    break;
                case MAP:
                   // mapArchive.createIfNotExist(file);
                    return mapArchive.archive(file).add(0, data);
                case TEXTURE:
                    break;
                case SPOT:
                    return spotAnimIndex.add(file >>> 8).add(file & 0xff, data);
                case VARBIT:
                    return varbitIndex.add(file >>> 1416501898).add(file & 0x3ffff, data);
                case LOC:
                    return locIndex.add(file >> 8).add((file) & (1 << 8) - 1, data);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public final Archive createArchive(int file, String name) {
        return configArchive.archive(file);
    }

    public void close() throws IOException {
        if (openRuneCache != null) {
            openRuneCache.close();
            openRuneCache = null;
        }
        indexedFileSystem.close();
    }

    public ResourceProvider getProvider() {
        return resourceProvider;
    }


}
