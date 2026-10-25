package com.rspsi.plugin.loader;

import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;
import com.google.common.collect.Maps;
import com.jagex.Client;
import com.jagex.cache.config.VariableBits;
import com.jagex.cache.def.ObjectDefinition;
import com.jagex.cache.def.RSArea;
import com.jagex.cache.loader.config.VariableBitLoader;
import com.jagex.cache.loader.object.ObjectDefinitionLoader;
import com.jagex.io.Buffer;
import dev.openrune.definition.codec.ObjectCodec;
import dev.openrune.definition.type.ObjectType;
import java.util.List;
import java.util.Map;

/** Revision-241 object loader backed by the same codec as the Chaotix game server. */
public class ObjectDefinitionLoaderOSRS extends ObjectDefinitionLoader {
    private static final int REVISION = 241;
    private final ObjectCodec codec = new ObjectCodec(REVISION);
    private final Map<Integer, ObjectDefinition> cache = Maps.newConcurrentMap();
    private int count;

    @Override
    public void init(Archive archive) {
        int highestId = -1;
        cache.clear();
        for (File file : archive.files()) {
            if (file == null || file.getData() == null) continue;
            highestId = Math.max(highestId, file.getId());
            try {
                ObjectType type = codec.loadData(file.getId(), file.getData()).postDecode();
                cache.put(file.getId(), convert(type));
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                    "Unable to decode revision-241 object definition " + file.getId(), exception);
            }
        }
        count = highestId + 1;
    }

    @Override
    public void init(Buffer data, Buffer indexBuffer) {
        throw new UnsupportedOperationException("OSRS objects are stored as archive files");
    }

    static ObjectDefinition convert(ObjectType type) {
        ObjectDefinition definition = new ObjectDefinition();
        definition.reset();
        definition.setId(type.getId());
        definition.setName(type.getName());
        definition.setModelIds(toIntArray(type.getObjectModels()));
        definition.setModelTypes(toIntArray(type.getObjectTypes()));
        definition.setWidth(type.getSizeX());
        definition.setLength(type.getSizeY());
        definition.setSolid(type.getSolid() != 0);
        definition.setImpenetrable(type.getImpenetrable());
        definition.setInteractive(type.getInteractive() == 1);
        definition.setContouredGround(type.getClipType() >= 0);
        definition.setDelayShading(type.getNonFlatShading());
        definition.setOccludes(type.getModelClipped());
        definition.setAnimation(type.getAnimationId());
        definition.setDecorDisplacement(type.getDecorDisplacement());
        definition.setAmbientLighting((byte) type.getAmbient());
        definition.setLightDiffusion((byte) type.getContrast());
        definition.setInteractions(actions(type));
        definition.setOriginalColours(toIntArray(type.getOriginalColours()));
        definition.setReplacementColours(toIntArray(type.getModifiedColours()));
        definition.setRetextureToFind(toIntArray(type.getOriginalTextureColours()));
        definition.setTextureToReplace(toIntArray(type.getModifiedTextureColours()));
        definition.setCategory(type.getCategory());
        definition.setInverted(type.isRotated());
        definition.setCastsShadow(type.getClipped());
        definition.setScaleX(type.getModelSizeX());
        definition.setScaleY(type.getModelSizeZ());
        definition.setScaleZ(type.getModelSizeY());
        definition.setMapscene(type.getMapSceneID());
        definition.setSurroundings(type.getClipMask());
        definition.setTranslateX(type.getOffsetX());
        definition.setTranslateY(type.getOffsetZ());
        definition.setTranslateZ(type.getOffsetY());
        definition.setObstructsGround(type.getObstructive());
        definition.setSupportItems(type.getSupportsItems());
        definition.setVarbit(type.getMultiVarBit());
        definition.setVarp(type.getMultiVarp());
        definition.setMorphisms(transforms(type));
        definition.setAreaId(type.getMapAreaId());
        definition.setRandomizeAnimStart(type.getRandomizeAnimStart());
        return definition;
    }

    private static String[] actions(ObjectType type) {
        String[] actions = new String[10];
        for (int index = 0; index < actions.length; index++) {
            String action = type.getActions().getOpOrNull(index);
            actions[index] = action == null || action.equalsIgnoreCase("hidden") ? null : action;
        }
        return actions;
    }

    private static int[] transforms(ObjectType type) {
        List<Integer> transforms = type.getTransforms();
        if ((transforms == null || transforms.isEmpty()) && type.getMultiDefault() == -1) return null;
        int size = transforms == null ? 0 : transforms.size();
        int[] result = new int[size + 1];
        for (int index = 0; index < size; index++) result[index] = transforms.get(index);
        result[size] = type.getMultiDefault();
        return result;
    }

    private static int[] toIntArray(List<Integer> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override public ObjectDefinition forId(int id) { return cache.get(id); }
    @Override public int count() { return count; }

    @Override
    public ObjectDefinition morphism(int id) {
        ObjectDefinition definition = forId(id);
        if (definition == null || definition.getMorphisms() == null) return null;
        int morphismIndex = -1;
        if (definition.getVarbit() != -1) {
            VariableBits bits = VariableBitLoader.lookup(definition.getVarbit());
            if (bits != null) {
                int mask = Client.BIT_MASKS[bits.getHigh() - bits.getLow()];
                morphismIndex = Client.getSingleton().settings[bits.getSetting()]
                    >> bits.getLow() & mask;
            }
        } else if (definition.getVarp() >= 0
                && definition.getVarp() < Client.getSingleton().settings.length) {
            morphismIndex = Client.getSingleton().settings[definition.getVarp()];
        }
        int[] morphisms = definition.getMorphisms();
        int morphismId = morphismIndex >= 0 && morphismIndex < morphisms.length - 1
            ? morphisms[morphismIndex] : morphisms[morphisms.length - 1];
        return morphismId == -1 ? null : forId(morphismId);
    }

    public void renameMapFunctions(RSAreaLoaderOSRS areaLoader) {
        cache.values().stream().filter(definition -> definition.getAreaId() != -1)
            .forEach(definition -> {
                RSArea area = areaLoader.forId(definition.getAreaId());
                if (area != null && (definition.getName() == null
                        || definition.getName().equals("null") || definition.getName().isEmpty())) {
                    definition.setName("minimap-function[" + area.getSpriteId() + "]");
                }
            });
    }
}
