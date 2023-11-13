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
package net.pistonmaster.serverwrecker.addons;

import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketSentEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;

public class ClientBrand implements InternalExtension {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(ClientBrand.class);
        AddonHelper.registerBotEventConsumer(SWPacketSentEvent.class, ClientBrand::onPacket);
    }

    public static void onPacket(SWPacketSentEvent event) {
        if (event.packet() instanceof ServerboundLoginAcknowledgedPacket) {
            if (!event.connection().settingsHolder().has(ClientBrandSettings.class)) {
                return;
            }

            var clientBrandSettings = event.connection().settingsHolder().get(ClientBrandSettings.class);

            if (!clientBrandSettings.sendClientBrand()) {
                return;
            }

            var buf = Unpooled.buffer();
            event.connection().session().getCodecHelper()
                    .writeString(buf, clientBrandSettings.clientBrand());

            event.connection().session().send(new ServerboundCustomPayloadPacket(
                    "minecraft:brand",
                    ByteBufUtil.getBytes(buf)
            ));
        }
    }

    @EventHandler
    public static void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ClientBrandPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public static void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ClientBrandSettings.class, new ClientBrandCommand());
    }

    private static class ClientBrandPanel extends NavigationItem implements SettingsDuplex<ClientBrandSettings> {
        private final JCheckBox sendClientBrand;
        private final JTextField clientBrand;

        ClientBrandPanel(ServerWreckerServer serverWreckerServer) {
            super();
            serverWreckerServer.getSettingsManager().registerDuplex(ClientBrandSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Send Client Brand: "));
            sendClientBrand = new PresetJCheckBox(ClientBrandSettings.DEFAULT_SEND_CLIENT_BRAND);
            add(sendClientBrand);

            add(new JLabel("Client Brand: "));
            clientBrand = new JTextField(ClientBrandSettings.DEFAULT_CLIENT_BRAND);
            add(clientBrand);
        }

        @Override
        public String getNavigationName() {
            return "Client Brand";
        }

        @Override
        public String getNavigationId() {
            return "client-brand";
        }

        @Override
        public void onSettingsChange(ClientBrandSettings settings) {
            sendClientBrand.setSelected(settings.sendClientBrand());
            clientBrand.setText(settings.clientBrand());
        }

        @Override
        public ClientBrandSettings collectSettings() {
            return new ClientBrandSettings(
                    sendClientBrand.isSelected(),
                    clientBrand.getText()
            );
        }
    }

    private static class ClientBrandCommand implements SettingsProvider<ClientBrandSettings> {
        @CommandLine.Option(names = {"--send-client-brand"}, description = "Send client brand")
        private boolean sendClientBrand = ClientBrandSettings.DEFAULT_SEND_CLIENT_BRAND;
        @CommandLine.Option(names = {"--client-brand"}, description = "Client brand")
        private String clientBrand = ClientBrandSettings.DEFAULT_CLIENT_BRAND;

        @Override
        public ClientBrandSettings collectSettings() {
            return new ClientBrandSettings(
                    sendClientBrand,
                    clientBrand
            );
        }
    }

    private record ClientBrandSettings(
            boolean sendClientBrand,
            String clientBrand
    ) implements SettingsObject {
        public static final boolean DEFAULT_SEND_CLIENT_BRAND = true;
        public static final String DEFAULT_CLIENT_BRAND = "vanilla";
    }
}
