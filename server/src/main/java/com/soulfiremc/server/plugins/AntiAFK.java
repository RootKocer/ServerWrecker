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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.AwayFromPosGoal;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableMinMaxProperty;
import com.soulfiremc.server.settings.property.MinMaxProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.concurrent.TimeUnit;

@Slf4j
@Extension
public class AntiAFK extends InternalPlugin {
  public AntiAFK() {
    super(new PluginInfo(
      "anti-afk",
      "1.0.0",
      "Automatically moves x amount of blocks in a random direction to prevent being kicked for being AFK",
      "AlexProgrammerDE",
            "GPL-3.0",
            "https://soulfiremc.com"));
  }

  @EventHandler
  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection
      .scheduler()
      .scheduleWithDynamicDelay(
        () -> {
          if (!settingsSource.get(AntiAFKSettings.ENABLED)) {
            return;
          }

          log.info("Moving bot to prevent AFK");
          PathExecutor.executePathfinding(
            connection,
            new AwayFromPosGoal(
              SFVec3i.fromDouble(connection
                .dataManager()
                .clientEntity()
                .pos()),
              settingsSource.getRandom(AntiAFKSettings.DISTANCE).getAsInt()),
            new PathConstraint(connection));
        },
        settingsSource.getRandom(AntiAFKSettings.DELAY).asLongSupplier(),
        TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AntiAFKSettings.class, "Anti AFK", this, "activity");
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AntiAFKSettings implements SettingsObject {
    private static final String NAMESPACE = "anti-afk";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Anti AFK")
        .description("Enable the Anti AFK feature")
        .defaultValue(false)
        .build();
    public static final MinMaxProperty DISTANCE = ImmutableMinMaxProperty.builder()
      .namespace(NAMESPACE)
      .key("distance")
      .minUiName("Min distance (blocks)")
      .maxUiName("Max distance (blocks)")
      .minDescription("Minimum distance to walk")
      .maxDescription("Maximum distance to walk")
      .minDefaultValue(10)
      .maxDefaultValue(30)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .stepValue(1)
      .build();
    public static final MinMaxProperty DELAY = ImmutableMinMaxProperty.builder()
      .namespace(NAMESPACE)
      .key("delay")
      .minUiName("Min delay (seconds)")
      .maxUiName("Max delay (seconds)")
      .minDescription("Minimum delay between moves")
      .maxDescription("Maximum delay between moves")
      .minDefaultValue(15)
      .maxDefaultValue(30)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .stepValue(1)
      .build();
  }
}
