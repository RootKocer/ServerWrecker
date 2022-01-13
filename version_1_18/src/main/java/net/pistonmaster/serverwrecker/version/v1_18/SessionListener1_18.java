/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
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
package net.pistonmaster.serverwrecker.version.v1_18;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.common.SessionEventBus;

@RequiredArgsConstructor
public class SessionListener1_18 extends SessionAdapter {
    private final SessionEventBus bus;
    private final ProtocolWrapper1_18 wrapper;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundChatPacket chatPacket) {
            Component message = chatPacket.getMessage();
            bus.onChat(PlainTextComponentSerializer.plainText().serialize(message));
        } else if (packet instanceof ClientboundPlayerPositionPacket posPacket) {
            double posX = posPacket.getX();
            double posY = posPacket.getY();
            double posZ = posPacket.getZ();
            float pitch = posPacket.getPitch();
            float yaw = posPacket.getYaw();
            bus.onPosition(posX, posY, posZ, pitch, yaw);
        } else if (packet instanceof ClientboundSetHealthPacket healthPacket) {
            bus.onHealth(healthPacket.getHealth(), healthPacket.getFood());
        } else if (packet instanceof ClientboundPlayerInfoPacket infoPacket) {
            if (infoPacket.getAction() == PlayerListEntryAction.ADD_PLAYER && infoPacket.getEntries()[0].getProfile().getName().equals(wrapper.getProfileName())) {
                bus.onJoin(); // TODO Implement everywhere else
            }
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        bus.onDisconnect(event.getReason(), event.getCause());
    }
}