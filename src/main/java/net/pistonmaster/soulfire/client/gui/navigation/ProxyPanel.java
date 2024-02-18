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
package net.pistonmaster.soulfire.client.gui.navigation;

import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.lenni0451.commons.swing.GBC;
import net.pistonmaster.soulfire.client.gui.GUIFrame;
import net.pistonmaster.soulfire.client.gui.GUIManager;
import net.pistonmaster.soulfire.client.gui.libs.JEnumComboBox;
import net.pistonmaster.soulfire.client.gui.popups.ImportTextDialog;
import net.pistonmaster.soulfire.proxy.ProxyType;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.util.BuiltinSettingsConstants;
import net.pistonmaster.soulfire.util.SFPathConstants;

public class ProxyPanel extends NavigationItem {
  @Inject
  public ProxyPanel(GUIManager guiManager, GUIFrame parent, CardsContainer cardsContainer) {
    setLayout(new GridBagLayout());

    var proxySettingsPanel = new JPanel();
    proxySettingsPanel.setLayout(new GridBagLayout());

    GeneratedPanel.addComponents(
        proxySettingsPanel,
        cardsContainer.getByNamespace(BuiltinSettingsConstants.PROXY_SETTINGS_ID),
        guiManager.settingsManager());

    GBC.create(this).grid(0, 0).fill(GBC.HORIZONTAL).weightx(1).add(proxySettingsPanel);

    var toolBar = new JToolBar();
    toolBar.setFloatable(false);
    var addButton = new JButton("+");
    addButton.setToolTipText("Add proxies");
    addButton.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        var menu = new JPopupMenu();
        menu.add(createProxyLoadButton(guiManager, parent, ProxyType.HTTP));
        menu.add(createProxyLoadButton(guiManager, parent, ProxyType.SOCKS4));
        menu.add(createProxyLoadButton(guiManager, parent, ProxyType.SOCKS5));
        menu.show(e.getComponent(), e.getX(), e.getY());
      }
    });

    toolBar.add(addButton);
    toolBar.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    toolBar.setBackground(UIManager.getColor("Table.background"));

    GBC.create(this).grid(0, 1).insets(10, 4, -5, 4).fill(GBC.HORIZONTAL).weightx(0).add(toolBar);

    var columnNames = new String[] {"IP", "Port", "Username", "Password", "Type", "Enabled"};
    var model =
        new DefaultTableModel(columnNames, 0) {
          final Class<?>[] columnTypes =
              new Class<?>[] {
                Object.class,
                Integer.class,
                Object.class,
                Object.class,
                ProxyType.class,
                Boolean.class
              };

          @Override
          public Class<?> getColumnClass(int columnIndex) {
            return columnTypes[columnIndex];
          }
        };

    var proxyList = new JTable(model);

    var proxyRegistry = guiManager.settingsManager().proxyRegistry();
    proxyRegistry.addLoadHook(
        () -> {
          model.getDataVector().removeAllElements();

          var proxies = proxyRegistry.getProxies();
          var registrySize = proxies.size();
          var dataVector = new Object[registrySize][];
          for (var i = 0; i < registrySize; i++) {
            var proxy = proxies.get(i);

            dataVector[i] =
                new Object[] {
                  proxy.host(),
                  proxy.port(),
                  proxy.username(),
                  proxy.password(),
                  proxy.type(),
                  proxy.enabled()
                };
          }

          model.setDataVector(dataVector, columnNames);

          proxyList
              .getColumnModel()
              .getColumn(4)
              .setCellEditor(new DefaultCellEditor(new JEnumComboBox<>(ProxyType.class)));

          model.fireTableDataChanged();
        });

    proxyList.addPropertyChangeListener(
        evt -> {
          if ("tableCellEditor".equals(evt.getPropertyName()) && !proxyList.isEditing()) {
            var proxies = new ArrayList<SWProxy>();

            for (var i = 0; i < proxyList.getRowCount(); i++) {
              var row = new Object[proxyList.getColumnCount()];
              for (var j = 0; j < proxyList.getColumnCount(); j++) {
                row[j] = proxyList.getValueAt(i, j);
              }

              var host = (String) row[0];
              var port = (int) row[1];
              var username = (String) row[2];
              var password = (String) row[3];
              var type = (ProxyType) row[4];
              var enabled = (boolean) row[5];

              proxies.add(new SWProxy(type, host, port, username, password, enabled));
            }

            proxyRegistry.setProxies(proxies);
          }
        });

    var scrollPane = new JScrollPane(proxyList);

    GBC.create(this).grid(0, 2).fill(GBC.BOTH).weight(1, 1).add(scrollPane);
  }

  private static JMenuItem createProxyLoadButton(
      GUIManager guiManager, GUIFrame parent, ProxyType type) {
    var button = new JMenuItem(type.toString());

    button.addActionListener(
        e ->
            new ImportTextDialog(
                SFPathConstants.WORKING_DIRECTORY,
                String.format("Load %s proxies", type),
                String.format("%s list file", type),
                guiManager,
                parent,
                text -> guiManager.settingsManager().proxyRegistry().loadFromString(text, type)));

    return button;
  }

  @Override
  public String getNavigationName() {
    return "Proxies";
  }

  @Override
  public String getNavigationId() {
    return "proxy-menu";
  }
}
