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
package com.soulfiremc.server.protocol.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.codec.PacketDefinition;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;

@RequiredArgsConstructor
public class SFTcpPacketCodec extends ByteToMessageCodec<Packet> {
  private final Session session;

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf buf) throws Exception {
    var initial = buf.writerIndex();

    var packetProtocol = this.session.getPacketProtocol();
    var codecHelper = this.session.getCodecHelper();
    try {
      var packetId = packetProtocol.getServerboundId(packet);
      PacketDefinition definition = packetProtocol.getServerboundDefinition(packetId);

      packetProtocol.getPacketHeader().writePacketId(buf, codecHelper, packetId);
      definition.getSerializer().serialize(buf, codecHelper, packet);

      // Change protocol here before it hits the via codec
      var protocol = (MinecraftProtocol) this.session.getPacketProtocol();
      if (packet instanceof ServerboundLoginAcknowledgedPacket) {
        protocol.setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
      } else if (packet instanceof ServerboundFinishConfigurationPacket) {
        protocol.setState(ProtocolState.GAME); // CONFIGURATION -> GAME
      } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
        protocol.setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
      }
    } catch (Throwable t) {
      // Reset writer index to make sure incomplete data is not written out.
      buf.writerIndex(initial);

      var e = new PacketErrorEvent(this.session, t);
      this.session.callEvent(e);
      if (!e.shouldSuppress()) {
        throw t;
      }
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
    var initial = buf.readerIndex();

    var packetProtocol = this.session.getPacketProtocol();
    var codecHelper = this.session.getCodecHelper();
    try {
      var id = packetProtocol.getPacketHeader().readPacketId(buf, codecHelper);
      if (id == -1) {
        buf.readerIndex(initial);
        return;
      }

      var packet = packetProtocol.createClientboundPacket(id, buf, codecHelper);

      if (buf.readableBytes() > 0) {
        throw new IllegalStateException(
          "Packet \"%s\" not fully read.".formatted(packet.getClass().getSimpleName()));
      }

      out.add(packet);
    } catch (Throwable t) {
      // Advance buffer to end to make sure remaining data in this packet is skipped.
      buf.readerIndex(buf.readerIndex() + buf.readableBytes());

      var e = new PacketErrorEvent(this.session, t);
      this.session.callEvent(e);
      if (!e.shouldSuppress()) {
        throw t;
      }
    }
  }
}
