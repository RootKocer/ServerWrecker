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
package net.pistonmaster.serverwrecker;

import net.pistonmaster.serverwrecker.gui.MainFrame;
import org.apache.commons.cli.ParseException;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

import java.awt.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ServerWrecker.getLogger().error(throwable.getMessage(), throwable));

        File dataFolder = initConfigDir();

        if (GraphicsEnvironment.isHeadless() || args.length > 0) {
            runHeadless(args);
        } else {
            new MainFrame(ServerWrecker.getInstance());
        }

        initPlugins(dataFolder);
    }

    private static File initConfigDir() {
        File dataDirectory = new File(System.getProperty("user.home"), ".serverwrecker");

        //noinspection ResultOfMethodCallIgnored
        dataDirectory.mkdirs();

        return dataDirectory;
    }

    private static void initPlugins(File dataFolder) {
        File pluginDir = new File(dataFolder, "plugins");

        //noinspection ResultOfMethodCallIgnored
        pluginDir.mkdirs();

        // create the plugin manager
        PluginManager pluginManager = new JarPluginManager(pluginDir.toPath());

        // start and load all plugins of application
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    private static void runHeadless(String[] args) {
        if (args.length == 0) {
            CommandLineParser.printHelp();
            return;
        }

        // parse the command line args
        CommandLineParser.ParseResult result;
        try {
            result = CommandLineParser.parse(args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            CommandLineParser.printHelp();
            return;
        }

        if (result.showHelp()) {
            CommandLineParser.printHelp();
            return;
        }

        ServerWrecker.getInstance().start(result.options());
    }
}
