package me.jayfella.pagedterrain.core;

public final class GridPosition implements Comparable<GridPosition>
{
    private int x, z;

    public GridPosition(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    public void set(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    public int getX() { return this.x; }
    public void setX(int x) { this.x = x; }

    public int getZ() { return this.z; }
    public void setZ(int z) { this.z = z; }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof GridPosition)) return false;
        if (this == o) return true;

        GridPosition comp = (GridPosition) o;

        if (Integer.compare(x, comp.x) != 0) return false;
        if (Integer.compare(z, comp.z) != 0) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 53 * hash + this.x;
        hash = 53 * hash + this.z;
        return hash;
    }

    @Override
    public int compareTo(GridPosition o)
    {
        return (this.x == o.x && this.z == o.z) ? 0 : 1;
    }
}