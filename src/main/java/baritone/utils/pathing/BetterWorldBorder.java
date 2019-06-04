/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils.pathing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.util.math.Vec3i;

/**
 * Essentially, a "rule" for the path finder, prevents proposed movements from attempting to venture
 * into the world border, and prevents actual movements from placing blocks in the world border.
 */
public class BetterWorldBorder {

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    public BetterWorldBorder( double minX, double maxX, double minZ, double maxZ ){

        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public BetterWorldBorder( Vec3i point1, Vec3i point2 ){

        this(
                Math.min( point1.getX(), point2.getX() ),
                Math.max( point1.getX(), point2.getX() ),
                Math.min( point1.getZ(), point2.getZ() ),
                Math.max( point1.getZ(), point2.getZ() )
        );

    }

    public BetterWorldBorder(WorldBorder border) {
        this( border.minX(), border.maxX(), border.minZ(), border.maxZ() );
    }

    public boolean entirelyContains(int x, int z) {
        return x + 1 > minX && x < maxX && z + 1 > minZ && z < maxZ;
    }
    public boolean entirelyContains(BlockPos pos) {
        return entirelyContains( pos.getX(), pos.getZ() );
    }
    public boolean containsXZ(int x, int z) {
        return x + 1 > minX && x -1 < maxX && z + 1 > minZ && z - 1 < maxZ;
    }
    public boolean containsXZ(BlockPos pos) {
        return containsXZ( pos.getX(), pos.getZ() );
    }


    public boolean canPlaceAt(int x, int z) {
        // move it in 1 block on all sides
        // because we can't place a block at the very edge against a block outside the border
        // it won't let us right click it
        return x > minX && x + 1 < maxX && z > minZ && z + 1 < maxZ;
    }
    @Override
    public String toString(){
        return "[" + minX + "," + minZ + "]-[" + maxX + "," + maxZ + "]";
    }
}
