package me.jayfella.pagedterrain.core;

import com.jme3.terrain.geomipmap.TerrainQuad;

public interface TerrainListener
{
    public boolean terrainLoadRequested(TerrainQuad tq);
    public boolean terrainUnloadRequested(TerrainQuad tq);
}
