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
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import net.pistonmaster.serverwrecker.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketSentEvent;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.util.BusInvoker;

public class SWSessionListener extends SessionAdapter {
    private final SessionDataManager bus;
    private final BotConnection botConnection;
    private final BusInvoker busInvoker;

    public SWSessionListener(SessionDataManager bus, BotConnection botConnection) {
        this.bus = bus;
        this.botConnection = botConnection;
        this.busInvoker = new BusInvoker(bus);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        var event = new SWPacketReceiveEvent(botConnection, (MinecraftPacket) packet);
        botConnection.eventBus().post(event);
        if (event.cancelled()) {
            return;
        }

        botConnection.logger().debug("Received packet: {}", packet.getClass().getSimpleName());

        try {
            busInvoker.handlePacket(event.getPacket());
        } catch (Throwable t) {
            botConnection.logger().error("Error while handling packet!", t);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        var event1 = new SWPacketSendingEvent(botConnection, event.getPacket());
        botConnection.eventBus().post(event1);
        event.setPacket(event1.getPacket());
        event.setCancelled(event1.cancelled());

        if (event1.cancelled()) {
            return;
        }

        botConnection.logger().debug("Sending packet: {}", event.getPacket().getClass().getSimpleName());
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        var event = new SWPacketSentEvent(botConnection, (MinecraftPacket) packet);
        botConnection.eventBus().post(event);

        botConnection.logger().trace("Sent packet: {}", packet.getClass().getSimpleName());
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        try {
            bus.onDisconnectEvent(event);
        } catch (Throwable t) {
            botConnection.logger().error("Error while handling disconnect event!", t);
        }

        botConnection.eventBus().post(new BotDisconnectedEvent(botConnection));
    }
}
