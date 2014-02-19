package me.jayfella.pagedterrain.core;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class GridBasedTerrain extends AbstractControl
{
    // private GridBasedTerrain gridBasedTerrain;

    // private SimpleApplication app;
    private SceneApplication app = SceneApplication.getApplication();

    private FilteredBasis noiseGenerator;
    private Material defaultMaterial;

    private int vd_north = 2, vd_east = 2, vd_south = 2, vd_west = 2, totalVisibleChunks = 25;
    private int blockSize, patchSize, bitshift, positionAdjustment;

    private Vector3f worldScale;

    private boolean isLoaded;
    private volatile boolean cacheInterrupted;

    private GridPosition
            currentChunkLoc = new GridPosition(0, 0),
            oldChunkLoc = new GridPosition(Integer.MAX_VALUE, Integer.MAX_VALUE),
            topViewBox = new GridPosition(0, 0), bottomViewBox = new GridPosition(0, 0);

    private final Map<GridPosition, TerrainQuad> loadedTiles = new HashMap<GridPosition, TerrainQuad>();
    private final Map<GridPosition, TerrainQuad> cachedTiles = new HashMap<GridPosition, TerrainQuad>();
    private final Set<GridPosition> generationQue = new HashSet<GridPosition>();
    private final Queue<TerrainQuad> pendingAddition = new ConcurrentLinkedQueue<TerrainQuad>();
    private final ScheduledThreadPoolExecutor threadpool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    // private Node parentNode;
    private final Node terrainNode = new Node("Paged Terrain");

    public GridBasedTerrain() { }

    // used for Terrain Wizard
    public GridBasedTerrain(AssetManager assetManager, Node parentNode,
            int blockSize, int patchSize,
            Vector3f worldScale,
            int vd_north, int vd_east, int vd_south, int vd_west,
            String defaultMaterial)
    {
        // this.app = app;
        // this.parentNode = parentNode;
        this.blockSize = blockSize;
        this.patchSize = patchSize;
        this.worldScale = worldScale;

        this.bitshift = bitCalc(blockSize);
        this.positionAdjustment = (blockSize - 1) / 2;

        this.vd_north = vd_north;
        this.vd_east = vd_east;
        this.vd_south = vd_south;
        this.vd_west = vd_west;

        // this.noiseGenerator = createNoiseGenerator();

        this.defaultMaterial = assetManager.loadMaterial(defaultMaterial);

        // app.getRootNode().attachChild(terrainNode);
        parentNode.attachChild(terrainNode);
    }

    public void setFilteredBasis(FilteredBasis basis)
    {
        this.noiseGenerator = basis;
    }

    /* private FilteredBasis createNoiseGenerator()
    {
        FractalSum base = new FractalSum();
        base.setRoughness(0.7f);
        base.setFrequency(1.0f);
        base.setAmplitude(1.0f);
        base.setLacunarity(3.12f);
        base.setOctaves(8);
        base.setScale(0.02125f);
        base.addModulator(new NoiseModulator()
            {
                @Override public float value(float... in)
                {
                    return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
                }
            });

        FilteredBasis ground = new FilteredBasis(base);
        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(5);
        therm.setTalus(0.011f);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1);

        ground.addPreFilter(iterate);

        return ground;
    } */


    public boolean terrainLoadRequested(TerrainQuad tq) { return true; }
    public boolean terrainUnloadRequested(TerrainQuad tq) { return true; }

    public TerrainQuad getTerrainQuad(GridPosition location)
    {
        String tqName = new StringBuilder()
                .append("TerrainQuad_")
                .append(location.getX())
                .append("_")
                .append(location.getZ())
                .toString();

        float[] heightmap = getHeightmap(location.getX(), location.getZ());

        TerrainQuad tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), heightmap);
        tq.setLocalScale(this.getWorldScale());
        tq.setMaterial(defaultMaterial);

        Vector3f worldLocation = this.toWorldLocation(location);
        tq.setLocalTranslation(worldLocation);

        return tq;
    }

    private float[] getHeightmap(int x, int z)
    {
        FloatBuffer buffer = this.noiseGenerator.getBuffer(x * (this.getBlockSize() - 1), z * (this.getBlockSize() - 1), 0, this.getBlockSize());
        return buffer.array();
    }

    private int bitCalc(int blockSize)
    {
        switch (blockSize)
        {
            case 17: return 4;
            case 33: return 5;
            case 65: return 6;
            case 129: return 7;
            case 257: return 8;
            case 513: return 9;
            case 1025: return 10;
        }

        throw new IllegalArgumentException("Invalid block size specified.");
    }

    public final void setViewDistance(int n, int e, int s, int w)
    {
        this.vd_north = n;
        this.vd_east = e;
        this.vd_south = s;
        this.vd_west = w;

        totalVisibleChunks = (vd_west + vd_east + 1) * (vd_north + vd_south + 1);
    }

    public int getThreadPoolCount() { return threadpool.getPoolSize(); }
    public void setThreadPoolCount(int threadcount) { threadpool.setCorePoolSize(threadcount); }

    public int getPatchSize() { return this.patchSize; }
    public int getBlockSize() { return this.blockSize; }
    public Vector3f getWorldScale() { return this.worldScale; }

    public Node getTerrainNode() { return this.terrainNode; }

    public Vector3f toWorldLocation(GridPosition gridPosition)
    {
        int x = gridPosition.getX() << bitshift;
        int z = gridPosition.getZ() << bitshift;

        return new Vector3f(x, 0, z);
    }

    public GridPosition toGridPosition(Vector3f location)
    {
        int x = (int)location.getX() >> bitshift;
        int z = (int)location.getZ() >> bitshift;

        return new GridPosition(x, z);
    }

    private boolean checkForOldTerrain()
    {
        Iterator<Map.Entry<GridPosition, TerrainQuad>> iterator = loadedTiles.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<GridPosition, TerrainQuad> entry = iterator.next();
            GridPosition position = entry.getKey();

            if (position.getX() < topViewBox.getX() || position.getX() > bottomViewBox.getX() || position.getZ() < topViewBox.getZ() || position.getZ() > bottomViewBox.getZ())
            {
                TerrainQuad tq = entry.getValue();

                if (!terrainUnloadRequested(tq))
                    return false;

                tq.removeFromParent();
                iterator.remove();

                return true;
            }
        }

        return false;
    }

    private boolean checkForNewTerrain()
    {
        // if the tiles in the world are equal the the expected tile size
        // it is safe to assume everything is ok.
        if (loadedTiles.size() == totalVisibleChunks)
        {
            this.isLoaded = true; // the world is safe to join
            return false;
        }

        // check if any tiles are generated and waiting to be added to the scene.
        TerrainQuad pendingTq = pendingAddition.poll();
        if (pendingTq != null)
        {
            if (!terrainLoadRequested(pendingTq))
                return false;

            TerrainLodControl lodControl = new TerrainLodControl(pendingTq, app.getCamera());
            // lodControl.setExecutor(threadpool);
            pendingTq.addControl(lodControl);

            loadedTiles.put(new GridPosition((int)pendingTq.getWorldTranslation().getX() >> bitshift, (int)pendingTq.getWorldTranslation().getZ() >> bitshift), pendingTq);
            terrainNode.attachChild(pendingTq);

            return true;
        }
        else
        {
            // iterate over the view distance
            for (int x = topViewBox.getX(); x <= bottomViewBox.getX(); x++)
            {
                for (int z = topViewBox.getZ(); z <= bottomViewBox.getZ(); z++)
                {
                    final GridPosition terrainLoc = new GridPosition(x, z);

                    // check if it's already in the scene.
                    if (loadedTiles.get(terrainLoc) != null) continue;

                    // check if it's already being generated (by a previous call).
                    if (generationQue.contains(terrainLoc)) continue;

                    // check if it's in the cache.
                    TerrainQuad cachedChunk = cachedTiles.get(terrainLoc);
                    if (cachedChunk != null)
                    {
                        pendingAddition.add(cachedChunk);
                        cachedTiles.remove(terrainLoc);
                    }
                    else
                    {
                        // generate it in the threadpool
                        generationQue.add(terrainLoc);

                        threadpool.submit(new Runnable()
                        {
                            @Override public void run()
                            {
                                TerrainQuad newTq = getTerrainQuad(terrainLoc);
                                pendingAddition.add(newTq);

                                // thread safety.
                                app.enqueue(new Callable<Boolean>()
                                {
                                    @Override public Boolean call()
                                    {
                                        generationQue.remove(terrainLoc);
                                        return true;
                                    }
                                });
                            }
                        });
                    }

                }
            }
        }

        return false;
    }


    private void recalculateCache()
    {
        cachedTiles.clear();
        cacheInterrupted = false;

        threadpool.execute(new Runnable()
        {
           @Override public void run()
           {
               // top and bottom
                for (int x = (topViewBox.getX() - 1); x <= (bottomViewBox.getX() + 1); x++)
                {
                    if (cacheInterrupted)
                        return;

                    //top
                    final GridPosition topLocation = new GridPosition(x, topViewBox.getZ() - 1);
                    final TerrainQuad topQuad = getTerrainQuad(topLocation);

                    // bottom
                    final GridPosition bottomLocation = new GridPosition(x, bottomViewBox.getZ() + 1);
                    final TerrainQuad bottomQuad = getTerrainQuad(bottomLocation);

                    app.enqueue(new Callable<Boolean>()
                    {
                        @Override public Boolean call()
                        {
                            cachedTiles.put(topLocation, topQuad);
                            cachedTiles.put(bottomLocation, bottomQuad);

                            return true;
                        }
                    });

                }

                // left and right
                for (int z = (topViewBox.getZ() - 1); z <= (bottomViewBox.getZ() + 1); z++)
                {
                    if (cacheInterrupted)
                        continue;

                    // left
                    final GridPosition leftLocation = new GridPosition(topViewBox.getX() - 1, z);
                    final TerrainQuad leftQuad = getTerrainQuad(leftLocation);

                    // right
                    final GridPosition rightLocation = new GridPosition(bottomViewBox.getX() + 1, z);
                    final TerrainQuad rightQuad = getTerrainQuad(rightLocation);

                    app.enqueue(new Callable<Boolean>()
                    {
                        @Override public Boolean call()
                        {
                            cachedTiles.put(leftLocation, leftQuad);
                            cachedTiles.put(rightLocation, rightQuad);

                            return true;
                        }
                    });
                }
           }
        });
    }


    @Override
    // public void update(float tpf)
    protected void controlUpdate(float tpf)
    {
        currentChunkLoc.set((int)(app.getCamera().getLocation().getX() + positionAdjustment) >> bitshift, (int)(app.getCamera().getLocation().getZ() + positionAdjustment) >> bitshift);

        if (currentChunkLoc.equals(oldChunkLoc) && generationQue.isEmpty() && pendingAddition.isEmpty())
        {
            return;
        }

        topViewBox.set(currentChunkLoc.getX() - vd_west, currentChunkLoc.getZ() - vd_north);
        bottomViewBox.set(currentChunkLoc.getX() + vd_east, currentChunkLoc.getZ() + vd_south);

        if (checkForOldTerrain())
            return;

        if (checkForNewTerrain())
            return;

        if (generationQue.isEmpty() && pendingAddition.isEmpty())
        {
            cacheInterrupted = true;
            recalculateCache();

            oldChunkLoc.set(currentChunkLoc.getX(), currentChunkLoc.getZ());
        }
    }

    /* @Override
    public void close()
    {
        threadpool.shutdown();
    }*/

    /* @Override
    public void destroy() throws IOException
    {
        super.destroy();
        threadpool.shutdown();
    } */

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp)
    {

    }
}
