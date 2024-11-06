/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.data;

import com.soulfiremc.server.util.mcstructs.AABB;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

public record BlockState(
  int id,
  BlockType blockType,
  boolean defaultState,
  BlockStateProperties properties,
  BlockShapeGroup blockShapeGroup) {
  public BlockState(
    int id,
    boolean defaultState,
    BlockStateProperties properties,
    BlockType blockType,
    Key key,
    int stateIndex) {
    this(id, blockType, defaultState, properties, getBlockShapeGroup(key, stateIndex));
  }

  public static BlockState forDefaultBlockType(BlockType blockType) {
    return blockType.statesData().defaultState();
  }

  private static BlockShapeGroup getBlockShapeGroup(Key key, int stateIndex) {
    var shapeGroups = BlockShapeLoader.BLOCK_SHAPES.get(key);
    var size = shapeGroups.size();
    if (size == 0) {
      // This block has no shape stored, this is for example for air or grass
      return BlockShapeGroup.EMPTY;
    } else if (size == 1) {
      // This block shares a shape for all states
      return shapeGroups.getFirst();
    } else {
      return shapeGroups.get(stateIndex);
    }
  }

  public List<AABB> getCollisionBoxes(Vector3i block) {
    return blockShapeGroup.getCollisionBoxes(block, blockType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BlockState blockState)) {
      return false;
    }
    return id == blockState.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return "BlockState{" +
      "id=" + id +
      ", properties=" + properties +
      ", blockShapeGroup=" + blockShapeGroup +
      '}';
  }
}
