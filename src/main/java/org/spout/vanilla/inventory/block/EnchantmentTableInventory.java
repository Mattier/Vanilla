/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, Spout LLC <http://www.spout.org/>
 * Vanilla is licensed under the Spout License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.vanilla.inventory.block;

import org.spout.api.geo.cuboid.Block;
import org.spout.api.inventory.HeldInventory;
import org.spout.api.inventory.ItemStack;

/**
 * Represents a enchantment table inventory belonging to an enchantment table
 * entity.
 */
public class EnchantmentTableInventory extends HeldInventory<Block> {
	private static final long serialVersionUID = 1L;
	public static final int SIZE = 1;
	public static final int SLOT = 0;

	public EnchantmentTableInventory() {
		super(SIZE);
	}

	/**
	 * Whether the inventory contains an item to enchant
	 * @return true if an item is present
	 */
	public boolean has() {
		return get(SLOT) != null;
	}

	/**
	 * Returns the {@link ItemStack} in the enchantment slot; can
	 * return null.
	 * @return ingredient item stack
	 */
	public ItemStack get() {
		return get(SLOT);
	}
}
