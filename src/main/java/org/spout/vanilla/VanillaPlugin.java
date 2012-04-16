/*
 * This file is part of Vanilla (http://www.spout.org/).
 *
 * Vanilla is licensed under the SpoutDev License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.vanilla;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Level;

import org.spout.api.Engine;
import org.spout.api.Server;
import org.spout.api.command.CommandRegistrationsFactory;
import org.spout.api.command.annotated.AnnotatedCommandRegistrationFactory;
import org.spout.api.command.annotated.SimpleAnnotatedCommandExecutorFactory;
import org.spout.api.command.annotated.SimpleInjector;
import org.spout.api.entity.type.ControllerType;
import org.spout.api.exception.ConfigurationException;
import org.spout.api.geo.World;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.math.Quaternion;
import org.spout.api.math.Vector3;
import org.spout.api.plugin.CommonPlugin;
import org.spout.api.protocol.Protocol;
import org.spout.api.protocol.common.CommonBootstrapProtocol;
import org.spout.vanilla.command.AdministrationCommands;
import org.spout.vanilla.command.TestCommands;
import org.spout.vanilla.configuration.VanillaConfiguration;
import org.spout.vanilla.controller.world.sky.VanillaSky;
import org.spout.vanilla.controller.world.sky.NetherSky;
import org.spout.vanilla.controller.world.sky.NormalSky;
import org.spout.vanilla.controller.world.sky.TheEndSky;
import org.spout.vanilla.controller.world.PointObserver;
import org.spout.vanilla.generator.flat.FlatGenerator;
import org.spout.vanilla.generator.nether.NetherGenerator;
import org.spout.vanilla.generator.normal.NormalGenerator;
import org.spout.vanilla.generator.theend.TheEndGenerator;
import org.spout.vanilla.material.VanillaMaterials;
import org.spout.vanilla.protocol.VanillaProtocol;
import org.spout.vanilla.protocol.bootstrap.VanillaBootstrapProtocol;

public class VanillaPlugin extends CommonPlugin {
    public static final int MINECRAFT_PROTOCOL_ID = 29;
    public static final int VANILLA_PROTOCOL_ID = ControllerType.getProtocolId("org.spout.vanilla.protocol");
	private static VanillaPlugin instance;
	private VanillaConfiguration config;
	private final HashMap<World, VanillaSky> skies = new HashMap<World, VanillaSky>();
    private Engine game;

	public VanillaPlugin() {
		instance = this;
	}

	@Override
	public void onLoad() {
		config = new VanillaConfiguration(getDataFolder());
		// TODO - do we need a protocol manager ?
		// getGame().getProtocolManager().register ...
		Protocol.registerProtocol("VanillaProtocol", new VanillaProtocol());

		game = getGame();

		if (game instanceof Server) {
			int port = 25565;
			String[] split = game.getAddress().split(":");
			if (split.length > 1) {
				try {
					port = Integer.parseInt(split[1]);
				} catch (NumberFormatException e) {
					getLogger().warning(split[1] + " is not a valid port number! Defaulting to " + port + "!");
				}
			}

			((Server) game).bind(new InetSocketAddress(split[0], port), new VanillaBootstrapProtocol());
			((Server) game).bind(new InetSocketAddress(split[0], port + 1), new CommonBootstrapProtocol(new VanillaProtocol()));
		}

		VanillaMaterials.initialize();
		getLogger().info("Loaded");
	}

	@Override
	public void onDisable() {
		try {
			config.save();
		} catch (ConfigurationException e) {
			getGame().getLogger().log(Level.WARNING, "Error saving Vanilla configuration: ", e);
		}
		getLogger().info("Disabled");
	}

	@Override
	public void onEnable() {
		// IO
		try {
			config.load();
		} catch (ConfigurationException e) {
			getGame().getLogger().log(Level.WARNING, "Error loading Vanilla configuration: ", e);
		}

		//Register commands
		CommandRegistrationsFactory<Class<?>> commandRegFactory = new AnnotatedCommandRegistrationFactory(new SimpleInjector(this), new SimpleAnnotatedCommandExecutorFactory());
		game.getRootCommand().addSubCommands(this, AdministrationCommands.class, commandRegFactory);
		game.getRootCommand().addSubCommands(this, TestCommands.class, commandRegFactory);

		//Register events
		game.getEventManager().registerEvents(new VanillaEventListener(this), this);

		//Initialize our default Vanilla worlds.
		World normal;
		if (VanillaConfiguration.FLATWORLD.getBoolean()) {
			normal = game.loadWorld("world", new FlatGenerator());
		} else {
			normal = game.loadWorld("world", new NormalGenerator());
		}

		World nether = game.loadWorld("world_nether", new NetherGenerator());
		World end = game.loadWorld("world_end", new TheEndGenerator());

		//Create the sky.
		NormalSky normSky = new NormalSky();
		NetherSky netherSky = new NetherSky();
		TheEndSky endSky = new TheEndSky();

		//Register skys to the map
		skies.put(normal, normSky);
		skies.put(nether, netherSky);
		skies.put(end, endSky);

		//Create spawn points as well as spawn the sky. TODO Have spawn point set by generator.
		normal.setSpawnPoint(new Transform(new Point(normal, 0.5F, 64.5F, 0.5F), Quaternion.IDENTITY, Vector3.ONE));
		normal.createAndSpawnEntity(new Point(normal, 0.f, 0.f, 0.f), normSky);
		normal.createAndSpawnEntity(new Point(normal, 0.5F, 64.5F, 0.5F), new PointObserver());

		nether.setSpawnPoint(new Transform(new Point(nether, 0.5F, 64.5F, 0.5F), Quaternion.IDENTITY, Vector3.ONE));
		nether.createAndSpawnEntity(new Point(nether, 0.f, 0.f, 0.f), netherSky);
		nether.createAndSpawnEntity(new Point(nether, 0.5F, 64.5F, 0.5F), new PointObserver());

		end.setSpawnPoint(new Transform(new Point(end, 0.5F, 64.5F, 0.5F), Quaternion.IDENTITY, Vector3.ONE));
		end.createAndSpawnEntity(new Point(end, 0.f, 0.f, 0.f), endSky);
		end.createAndSpawnEntity(new Point(end, 0.5F, 64.5F, 0.5F), new PointObserver());

		getLogger().info("b" + this.getDescription().getVersion() + " enabled. Protocol: " + getDescription().getData("protocol").get());
	}

	public static VanillaPlugin getInstance() {
		return instance;
	}

	public VanillaSky getSky(World world) {
		return skies.get(world);
	}
}
