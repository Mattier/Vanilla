/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
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
package org.spout.vanilla.material.block.redstone;

import org.spout.api.geo.cuboid.Block;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.material.block.BlockFaces;

import org.spout.vanilla.configuration.VanillaConfiguration;
import org.spout.vanilla.material.VanillaBlockMaterial;
import org.spout.vanilla.material.VanillaMaterials;
import org.spout.vanilla.material.block.GroundAttachable;
import org.spout.vanilla.material.block.RedstoneSource;
import org.spout.vanilla.material.block.RedstoneTarget;

import org.spout.vanilla.util.RedstonePowerMode;

public class RedstoneWire extends GroundAttachable implements RedstoneSource, RedstoneTarget {

	public RedstoneWire() {
		super("Redstone Wire", 55);
	}

	@Override
	public void loadProperties() {
		super.loadProperties();
		this.setHardness(0.0F).setResistance(0.0F);
		this.setDrop(VanillaMaterials.REDSTONE_DUST);
	}

	@Override
	public boolean hasPhysics() {
		return true;
	}

	private void update(Block middle) {
		middle.update();
//		Block block;
//		for (BlockFace face : BlockFaces.NESWBT) {
//			block = middle.translate(face);
//			if (block.getMaterial().equals(this)) {
//				VanillaMaterials.REDSTONE_WIRE.onUpdate(block);
//			} else {
//				block.update(false);
//			}
//		}
	}
	
	@Override
	public void doRedstoneUpdates(Block block) {
		block = block.setSource(this);
		this.update(block);
		for (BlockFace face : BlockFaces.NESWB) {
			this.update(block.translate(face));
		}
	}

	@Override
	public void onUpdate(Block block) {
		super.onUpdate(block);
		if (!VanillaConfiguration.REDSTONE_PHYSICS.getBoolean()) {
			return;
		}
		
		if (block.getMaterial().equals(this)) {
			short receiving = this.getReceivingPower(block);
			setPowerAndUpdate(block, receiving);
		}
	}

	@Override
	public boolean hasRedstonePowerTo(Block block, BlockFace direction, RedstonePowerMode powerMode) {
		return this.getRedstonePowerTo(block, direction, powerMode) > 0;
	}

	@Override
	public short getRedstonePowerTo(Block block, BlockFace direction, RedstonePowerMode powerMode) {
		if (powerMode == RedstonePowerMode.ALLEXCEPTWIRE) {
			return REDSTONE_POWER_MIN;
		} else {
			short power = this.getRedstonePower(block);
			if (power == REDSTONE_POWER_MIN) {
				return power;
			} else {
				BlockMaterial mat = block.translate(direction).getSubMaterial();
				if (mat instanceof RedstoneSource || mat instanceof RedstoneTarget || !isDistractedFrom(block, direction)) {
					return power;
				} else {
					return REDSTONE_POWER_MIN;
				}
			}
		}
	}

	@Override
	public short getRedstonePower(Block block, RedstonePowerMode powerMode) {
		return powerMode == RedstonePowerMode.ALLEXCEPTWIRE ? REDSTONE_POWER_MIN : block.getData();
	}

	@Override
	public boolean hasRedstonePower(Block block, RedstonePowerMode powerMode) {
		return powerMode == RedstonePowerMode.ALLEXCEPTWIRE ? false : block.getData() > 0;
	}

	@Override
	public boolean isReceivingPower(Block block) {
		return this.getReceivingPower(block) > 0;
	}

	public short getReceivingPower(Block block) {
		short maxPower = 0;
		Block rel, relvert;
		BlockMaterial mat;
		//detect power from direct neighbouring sources
		boolean topIsConductor = false;
		for (BlockFace face : BlockFaces.BTEWNS) {
			rel = block.translate(face);
			mat = rel.getSubMaterial();
			if (mat.equals(this)) {
				//handle neighbouring redstone wires
				maxPower = (short) Math.max(maxPower, this.getRedstonePower(rel) - 1);
			} else if (mat instanceof VanillaBlockMaterial) {
				//handle solid blocks and redstone sources
				maxPower = (short) Math.max(maxPower, ((VanillaBlockMaterial) mat).getRedstonePower(rel, RedstonePowerMode.ALLEXCEPTWIRE));
				if (mat instanceof RedstoneSource) {
					maxPower = (short) Math.max(maxPower, ((RedstoneSource) mat).getRedstonePowerTo(rel, face.getOpposite(), RedstonePowerMode.ALL));
				}
			}
			//shortcut just in case the answer is simple
			if (maxPower == REDSTONE_POWER_MAX) {
				return maxPower;
			}
			//check relatively up and down faces
			if (face == BlockFace.TOP) {
				topIsConductor = isConductor(mat);
			} else if (face != BlockFace.BOTTOM) {
				//check below for wire
				if (!isConductor(mat)) {
					relvert = rel.translate(BlockFace.BOTTOM);
					if (relvert.getMaterial().equals(this)) {
						maxPower = (short) Math.max(maxPower, this.getRedstonePower(relvert) - 1);
					}
				}
				//check above for wire
				if (!topIsConductor) {
					relvert = rel.translate(BlockFace.TOP);
					if (relvert.getMaterial().equals(this)) {
						maxPower = (short) Math.max(maxPower, this.getRedstonePower(relvert) - 1);
					}
				}
			}
		}
		return maxPower;
	}

	/**
	 * Sets the wire at the block to the given power and initiates an update process that will recalculate the wire.
	 * @param block of the wire
	 * @param power to set to
	 */
	public void setPowerAndUpdate(Block block, short power) {
		short current = this.getRedstonePower(block);
		if (current != power) {
			block.setMaterial(this, power);
			
			//TODO: Make sure updating is not done by performing 15 ever-lessening power updates...
			this.doRedstoneUpdates(block);			
			if (true) return;
		}
	}

	/**
	 * Checks if a redstone wire is connected to a certain block<br>
	 * No solid block connections are checked.
	 * 
	 * @param block of the wire
	 * @param face to connect to
	 * @return True if connected
	 */
	public boolean isConnectedToSource(Block block, BlockFace face) {
		Block target = block.translate(face);
		BlockMaterial mat = target.getSubMaterial();
		if (mat instanceof RedstoneSource || mat instanceof RedstoneTarget) {
			return true;
		} else {
			//check below
			if (!isConductor(mat)) {
				if (target.translate(BlockFace.BOTTOM).getMaterial().equals(this)) {
					return true;
				}
			}
			//check above
			if (target.translate(BlockFace.TOP).getMaterial().equals(this)) {
				if (!isConductor(block.translate(BlockFace.TOP).getSubMaterial())) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Checks if the block material given is a redstone conductor
	 * @param mat to check
	 * @return True if it is a redstone conductor
	 */
	public boolean isConductor(BlockMaterial mat) {
		return mat instanceof VanillaBlockMaterial && ((VanillaBlockMaterial) mat).isRedstoneConductor();
	}

	/**
	 * Checks if this wire is distracted from connecting to a certain solid block
	 * @param block of the wire
	 * @param direction it tries to connect to a solid block
	 * @return True if the wire is distracted
	 */
	public boolean isDistractedFrom(Block block, BlockFace direction) {
		switch (direction) {
		case NORTH :
		case SOUTH :
			return this.isConnectedToSource(block, BlockFace.EAST) || this.isConnectedToSource(block, BlockFace.WEST);
		case EAST :
		case WEST :
			return this.isConnectedToSource(block, BlockFace.NORTH) || this.isConnectedToSource(block, BlockFace.SOUTH);
		default :
			return false;
		}
	}
}
