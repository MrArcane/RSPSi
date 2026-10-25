package com.jagex.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.Lists;
import com.jagex.chunk.Chunk;
import com.rspsi.core.misc.Vector2;

public class MultiMapEncoder {
	
	public static byte[] encode(List<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			if (!chunk.hasLoaded())
				throw new IllegalStateException("Region at " + chunk.offsetX / 64 + "," + chunk.offsetY / 64
					+ " has not finished loading and cannot be packed yet.");
		}
		return encode(chunks, false);
	}

	private static byte[] encode(List<Chunk> chunks, boolean shallow) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			output.writeInt(chunks.size());
			for(Chunk chunk : chunks) {
				byte[] objectMap = shallow ? chunk.objectMapData : chunk.scenegraph.saveObjects(chunk);
				byte[] tileMap = shallow ? chunk.tileMapData : chunk.mapRegion.save_terrain_block(chunk);
				if (objectMap == null || tileMap == null)
					throw new IllegalStateException("Region at " + chunk.offsetX / 64 + "," + chunk.offsetY / 64
						+ " is missing terrain or object data.");
				output.writeInt(chunk.objectMapId);
				output.writeInt(chunk.tileMapId);
				output.writeInt(chunk.offsetX / 64);
				output.writeInt(chunk.offsetY / 64);
				output.writeInt(objectMap.length);
				output.write(objectMap);
				output.writeInt(tileMap.length);
				output.write(tileMap);
			}
		} catch (IOException impossible) {
			throw new IllegalStateException("Unable to build map pack", impossible);
		}
		return bytes.toByteArray();
	}
	
	public static byte[] encodeShallow(List<Chunk> chunks) {
		return encode(chunks, true);
	}

	public static Vector2 getSize(byte[] encoded) {
		ByteBuffer buffer = ByteBuffer.wrap(encoded);
		int size = buffer.getInt();

		int maximumX = 0;
		int maximumY = 0;
		for(int i = 0;i<size;i++) {
			buffer.getInt();
			buffer.getInt();

			int positionX = buffer.getInt();
			int positionY = buffer.getInt();

			if(positionX > maximumX)
				maximumX = positionX;
			if(positionY > maximumY)
				maximumY = positionY;

			buffer.get(new byte[buffer.getInt()]);
			buffer.get(new byte[buffer.getInt()]);

		}

		return new Vector2(maximumX, maximumY);
	}
	public static List<Chunk> decode(byte[] encoded){
		List<Chunk> chunks = Lists.newArrayList();
		ByteBuffer buffer = ByteBuffer.wrap(encoded);
		if (buffer.remaining() < Integer.BYTES)
			throw new IllegalArgumentException("Pack file is missing its region count.");
		int size = buffer.getInt();
		if (size < 0 || size > 16_384)
			throw new IllegalArgumentException("Invalid pack region count: " + size);
		
		for(int i = 0;i<size;i++) {
			if (buffer.remaining() < 24)
				throw new IllegalArgumentException("Pack ended before region " + i + " was complete.");
			int objectMapId = buffer.getInt();
			int tileMapId = buffer.getInt();
			
			int positionX = buffer.getInt();
			int positionY = buffer.getInt();
			
			int cX = (0 + 64 * positionX) / 64;
			int cY = (0 + 64 * positionY) / 64;

			int hash = (cX << 8) + cY;
			
			int objLen = checkedLength(buffer, "object", i, Integer.BYTES);
			byte[] objData = new byte[objLen];
			buffer.get(objData);
			
			int landscapeLen = checkedLength(buffer, "terrain", i, 0);
			byte[] landscapeData = new byte[landscapeLen];
			buffer.get(landscapeData);
			
			Chunk chunk = new Chunk(hash);

			chunk.offsetX = 64 * positionX;
			chunk.offsetY = 64 * positionY;
			
			chunk.objectMapData = objData;
			chunk.tileMapData = landscapeData;
			chunk.objectMapId = objectMapId;
			chunk.tileMapId = tileMapId;
			
			System.out.println("Loaded chunk " + chunk.offsetX + " : " + chunk.offsetY + " IDS: " + chunk.objectMapId + " : " + chunk.tileMapId);
			chunks.add(chunk);
		}
		if (buffer.hasRemaining())
			throw new IllegalArgumentException("Pack contains " + buffer.remaining() + " unexpected trailing bytes.");
		return chunks;
	}

	private static int checkedLength(ByteBuffer buffer, String kind, int region, int reservedBytes) {
		if (buffer.remaining() < Integer.BYTES)
			throw new IllegalArgumentException("Pack is missing the " + kind + " length for region " + region + '.');
		int length = buffer.getInt();
		if (length < 0 || length > buffer.remaining() - reservedBytes)
			throw new IllegalArgumentException("Invalid " + kind + " length " + length + " for region " + region + '.');
		return length;
	}

}
