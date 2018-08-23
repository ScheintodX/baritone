/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.event.events;

import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 8/22/2018
 */
public final class BlockInteractEvent {

    /**
     * The position of the block interacted with
     */
    private final BlockPos pos;

    /**
     * The type of interaction that occurred
     */
    private final Type type;

    public BlockInteractEvent(BlockPos pos, Type type) {
        this.pos = pos;
        this.type = type;
    }

    /**
     * @return The position of the block interacted with
     */
    public final BlockPos getPos() {
        return this.pos;
    }

    /**
     * @return The type of interaction with the target block
     */
    public final Type getType() {
        return this.type;
    }

    public enum Type {

        /**
         * We're breaking the target block.
         */
        BREAK,

        /**
         * We're right clicking on the target block. Either placing or interacting with.
         */
        USE
    }
}
