/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.pathfinding.graph;

import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    public List<GraphInstructions> getActions(BotEntityState node) {
        List<GraphInstructions> targetSet = new ArrayList<>();
        for (var direction : MovementDirection.values()) {
            for (var modifier : MovementModifier.values()) {
                if (direction.isDiagonal()) {
                    for (var side : MovementSide.values()) {
                        targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, side).getInstructions());
                    }
                } else {
                    targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, null).getInstructions());
                }
            }
        }

        log.debug("Found {} possible actions for {}", targetSet.stream()
                .filter(a -> !a.isImpossible())
                .count(), node.position());

        return targetSet;
    }
}
