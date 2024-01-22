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
package net.pistonmaster.serverwrecker.generator.generators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.pistonmaster.serverwrecker.generator.util.GeneratorConstants;
import net.pistonmaster.serverwrecker.generator.util.ResourceHelper;

import java.util.Locale;

public class BlocksJavaGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "BlockType.java";
    }

    @Override
    public String generateDataJson() {
        var base = ResourceHelper.getResource("/templates/BlockType.java");
        return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                BuiltInRegistries.BLOCK
                        .stream().map(s -> {
                            var name = BuiltInRegistries.BLOCK.getKey(s).getPath();
                            return "public static final BlockType " + name.toUpperCase(Locale.ROOT) + " = register(\"" + name + "\");";
                        })
                        .toArray(String[]::new)));
    }
}
