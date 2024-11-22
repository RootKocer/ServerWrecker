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
package com.soulfiremc.server.protocol.bot.container;

import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.util.MathHelper;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;

@Getter
public class SFItemStack extends ItemStack {
  private final ItemType type;

  private SFItemStack(SFItemStack clone, int amount) {
    super(clone.getId(), amount, clone.getDataComponents());
    this.type = clone.type;
  }

  private SFItemStack(ItemStack itemStack) {
    super(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponents());
    this.type = ItemType.REGISTRY.getById(itemStack.getId());
  }

  private SFItemStack(ItemType itemType, int amount) {
    super(itemType.id(), amount, null);
    this.type = itemType;
  }

  public static SFItemStack from(ItemStack itemStack) {
    if (itemStack == null) {
      return null;
    }

    return new SFItemStack(itemStack);
  }

  @VisibleForTesting
  public static SFItemStack forTypeSingle(ItemType itemType) {
    return forTypeWithAmount(itemType, 1);
  }

  @VisibleForTesting
  public static SFItemStack forTypeWithAmount(ItemType itemType, int amount) {
    return new SFItemStack(itemType, amount);
  }

  @Deprecated
  @Override
  @SuppressWarnings("DeprecatedIsStillUsed")
  public DataComponents getDataComponents() {
    return super.getDataComponents();
  }

  public SFDataComponents components() {
    var internalMap = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
    var newComponents = new SFDataComponents(internalMap);
    internalMap.putAll(type.components().components());

    var overrideComponents = super.getDataComponents();
    if (overrideComponents != null) {
      internalMap.putAll(overrideComponents.getDataComponents());
    }

    return newComponents;
  }

  public boolean canStackWith(SFItemStack other) {
    if (other == null) {
      return false;
    }

    return this.type == other.type;
  }

  public boolean has(DataComponentType<?> component) {
    return components().getOptional(component).isPresent();
  }

  public <T> T get(DataComponentType<T> component) {
    return components().get(component);
  }

  public <T> T getOrDefault(DataComponentType<T> component, T defaultValue) {
    return components().getOptional(component).orElse(defaultValue);
  }

  public int getMaxStackSize() {
    return this.getOrDefault(DataComponentType.MAX_STACK_SIZE, 1);
  }

  public boolean isStackable() {
    return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
  }

  public boolean isDamageableItem() {
    return this.has(DataComponentType.MAX_DAMAGE) && !this.has(DataComponentType.UNBREAKABLE) && this.has(DataComponentType.DAMAGE);
  }

  public boolean isDamaged() {
    return this.isDamageableItem() && this.getDamageValue() > 0;
  }

  public int getDamageValue() {
    return MathHelper.clamp(this.getOrDefault(DataComponentType.DAMAGE, 0), 0, this.getMaxDamage());
  }

  public int getMaxDamage() {
    return this.getOrDefault(DataComponentType.MAX_DAMAGE, 0);
  }

  public boolean isBroken() {
    return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
  }

  public boolean nextDamageWillBreak() {
    return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
  }
}
