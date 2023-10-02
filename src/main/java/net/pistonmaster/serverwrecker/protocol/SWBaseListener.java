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

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.viaversion.viaversion.api.connection.UserConnection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.SWConstants;
import net.pistonmaster.serverwrecker.auth.service.JavaData;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.ProtocolMetadataStorage;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class SWBaseListener extends SessionAdapter {
    private final BotConnection botConnection;
    private final @NonNull ProtocolState targetState;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(session instanceof ViaClientSession viaSession)) {
            throw new IllegalStateException("Session is not a ViaSession!");
        }

        var protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            if (packet instanceof ClientboundHelloPacket helloPacket) {
                var botSettings = botConnection.settingsHolder().get(BotSettings.class);
                var minecraftAccount = botConnection.meta().getMinecraftAccount();
                UserConnection viaUserConnection = session.getFlag(SWProtocolConstants.VIA_USER_CONNECTION);

                var authSupport = minecraftAccount.isPremiumJava();
                if (!authSupport) {
                    botConnection.logger().info("Server sent a encryption request, but we're offline mode. Not authenticating with mojang.");
                }

                var auth = authSupport;
                var isLegacy = SWConstants.isLegacy(botSettings.protocolVersion());
                if (auth && isLegacy) {
                    auth = Objects.requireNonNull(viaUserConnection.get(ProtocolMetadataStorage.class)).authenticate;
                }

                botConnection.logger().debug("Performing mojang request: {}", auth);

                SecretKey key;
                try {
                    var gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    key = gen.generateKey();
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Failed to generate shared key.", e);
                }

                if (auth) {
                    var serverId = botConnection.meta().getSessionService()
                            .getServerId(helloPacket.getServerId(), helloPacket.getPublicKey(), key);
                    botConnection.meta().joinServerId(serverId, viaSession);
                }

                session.send(new ServerboundKeyPacket(helloPacket.getPublicKey(), key, helloPacket.getChallenge()));

                if (!isLegacy) { // Legacy encryption is handled in SWViaEncryptionProvider
                    viaSession.enableJavaEncryption(key);
                } else {
                    botConnection.logger().debug("Storing legacy secret key.");
                    session.setFlag(SWProtocolConstants.ENCRYPTION_SECRET_KEY, key);
                }
            } else if (packet instanceof ClientboundGameProfilePacket) {
                session.send(new ServerboundLoginAcknowledgedPacket());
            } else if (packet instanceof ClientboundLoginDisconnectPacket loginDisconnectPacket) {
                session.disconnect(loginDisconnectPacket.getReason());
            } else if (packet instanceof ClientboundLoginCompressionPacket loginCompressionPacket) {
                viaSession.setCompressionThreshold(loginCompressionPacket.getThreshold());
            }
        } else if (protocol.getState() == ProtocolState.STATUS) {
            if (packet instanceof ClientboundStatusResponsePacket) {
                session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
            } else if (packet instanceof ClientboundPongResponsePacket) {
                session.disconnect("Finished");
            }
        } else if (protocol.getState() == ProtocolState.GAME) {
            if (packet instanceof ClientboundKeepAlivePacket keepAlivePacket) {
                session.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            } else if (packet instanceof ClientboundPingPacket pingPacket) {
                session.send(new ServerboundPongPacket(pingPacket.getId()));
            } else if (packet instanceof ClientboundDisconnectPacket disconnectPacket) {
                session.disconnect(disconnectPacket.getReason());
            } else if (packet instanceof ClientboundStartConfigurationPacket) {
                session.send(new ServerboundConfigurationAcknowledgedPacket());
            }
        } else if (protocol.getState() == ProtocolState.CONFIGURATION) {
            if (packet instanceof ClientboundFinishConfigurationPacket) {
                session.send(new ServerboundFinishConfigurationPacket());
            }
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        var protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (packet instanceof ClientIntentionPacket) {
            // Once the HandshakePacket has been sent, switch to the next protocol mode.
            protocol.setState(this.targetState);

            if (this.targetState == ProtocolState.LOGIN) {
                var minecraftAccount = botConnection.meta().getMinecraftAccount();

                UUID uuid;
                if (minecraftAccount.accountData() instanceof JavaData javaData) {
                    uuid = javaData.profileId();
                } else {
                    uuid = UUID.randomUUID(); // We are using a bedrock account, the uuid doesn't matter.
                }

                session.send(new ServerboundHelloPacket(minecraftAccount.username(), uuid));
            } else {
                session.send(new ServerboundStatusRequestPacket());
            }
        } else if (packet instanceof ServerboundLoginAcknowledgedPacket) {
            protocol.setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
        } else if (packet instanceof ServerboundFinishConfigurationPacket) {
            protocol.setState(ProtocolState.GAME); // CONFIGURATION -> GAME
        } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
            protocol.setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        var protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
        var botSettings = botConnection.settingsHolder().get(BotSettings.class);
        var host = botSettings.host();
        var port = botSettings.port();

        event.getSession().send(new ClientIntentionPacket(
                protocol.getCodec().getProtocolVersion(),
                host,
                port,
                switch (this.targetState) {
                    case LOGIN -> HandshakeIntent.LOGIN;
                    case STATUS -> HandshakeIntent.STATUS;
                    default -> throw new IllegalStateException("Unexpected value: " + this.targetState);
                }
        ));
    }
}
