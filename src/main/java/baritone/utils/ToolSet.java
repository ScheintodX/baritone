/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public class ToolSet {
    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Map<Block, Double> breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final Function<Block, Double> backendCalculation;

    private final EntityPlayerSP player;

    public ToolSet(EntityPlayerSP player) {
        breakStrengthCache = new HashMap<>();
        this.player = player;

        if (Baritone.settings().considerPotionEffects.value) {
            double amplifier = potionAmplifier();
            Function<Double, Double> amplify = x -> amplifier * x;
            backendCalculation = amplify.compose(this::getBestDestructionTime);
        } else {
            backendCalculation = this::getBestDestructionTime;
        }
    }

    /**
     * Using the best tool on the hotbar, how fast we can mine this block
     *
     * @param state the blockstate to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    public double getStrVsBlock(IBlockState state) {
        return breakStrengthCache.computeIfAbsent(state.getBlock(), backendCalculation);
        //return backendCalculation.apply( state.getBlock() );
    }

    /**
     * Evaluate the material cost of a possible tool. The priority matches the
     * listed order in the Item.ToolMaterial enum.
     *
     * @param itemStack a possibly empty ItemStack
     * @return values range from -1 to 4
     */
    private int getMaterialCost(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemTool) {
            ItemTool tool = (ItemTool) itemStack.getItem();
            return ToolMaterial.valueOf(tool.getToolMaterialName()).ordinal();
        } else {
            return -1;
        }
    }

    /**
     * Calculate which tool on the hotbar is best for mining
     *
     * @param b the blockstate to be mined
     * @return A byte containing the index in the tools array that worked best
     */
    public byte getBestSlot(Block b) {

        Settings settings = BaritoneAPI.getSettings();
        boolean cheap = settings.eco.value;

        if( settings.useMending.value && settings.useMendingOn.value.contains(b) ) return getEnchantedSlotFor( b, Enchantments.MENDING );
        else if( settings.useSilkTouch.value && settings.useSilkTouchOn.value.contains(b)) return getEnchantedSlotFor( b, Enchantments.SILK_TOUCH );
        else if( settings.useFortune.value && settings.useFortuneOn.value.contains(b)) return getEnchantedSlotFor( b, Enchantments.FORTUNE );

        return getFastestSlotFor(b, cheap);
    }

    public byte getFastestSlotFor(Block b, boolean cheap){
        byte best = 0;
        double value = Double.NEGATIVE_INFINITY;
        int materialCost = Integer.MIN_VALUE;
        int enchantmentCost = Integer.MAX_VALUE;
        IBlockState blockState = b.getDefaultState();
        for (byte i = 0; i < 9; i++) {
            ItemStack itemStack = player.inventory.getStackInSlot(i);
            double v = calculateSpeedVsBlock(itemStack, blockState, !cheap );
            if (v > value) {
                value = v;
                best = i;
                materialCost = getMaterialCost(itemStack);
                if( cheap ) enchantmentCost = calculateEnchantmentCost( itemStack );
            } else if (v == value) {
                int c = getMaterialCost(itemStack);
                if (c < materialCost) {
                    value = v;
                    best = i;
                    materialCost = c;
                    if( cheap ) materialCost = getMaterialCost(itemStack);
                } else if( cheap && c == materialCost ) {
                    int x = calculateEnchantmentCost( itemStack );
                    if( x < enchantmentCost ) {
                        value = v;
                        best = i;
                        enchantmentCost = x;
                    }

                }
            }
        }
        return best;
    }

    public byte getEnchantedSlotFor(Block b, net.minecraft.enchantment.Enchantment enchantment ){

        System.err.println( "SILK" );

        byte best = 0;
        double value = Double.NEGATIVE_INFINITY;
        IBlockState blockState = b.getDefaultState();
        int effectLevel = Integer.MIN_VALUE;

        for (byte i = 0; i < 9; i++) {
            ItemStack itemStack = player.inventory.getStackInSlot(i);
            double v = calculateSpeedVsBlock(itemStack, blockState, false);
            if (v > value) {
                value = v;
                best = i;
                effectLevel = EnchantmentHelper.getEnchantmentLevel(enchantment, itemStack);
            } else if (v == value) {

                int effLevel = EnchantmentHelper.getEnchantmentLevel(enchantment, itemStack);
                if( effLevel > effectLevel ){
                    value = v;
                    best = i;
                    effectLevel = effLevel;
                }
            }
        }

        return best;
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param b the blockstate to be mined
     * @return A double containing the destruction ticks with the best tool
     */
    private double getBestDestructionTime(Block b ) {

        byte slot = getBestSlot(b);
        ItemStack stack = player.inventory.getStackInSlot(slot);
        return calculateSpeedVsBlock(stack, b.getDefaultState(), true) * avoidanceMultiplier(b);
    }

    private double avoidanceMultiplier(Block b) {
        return Baritone.settings().blocksToAvoidBreaking.value.contains(b) ? 0.1 : 1;
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param item  the item to mine it with
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public static double calculateSpeedVsBlock(ItemStack item, IBlockState state, boolean honorEfficiency ) {

        float hardness = state.getBlockHardness(null, null);
        if (hardness < 0) {
            return -1;
        }

        float speed = item.getDestroySpeed(state);
        if (honorEfficiency && speed > 1) {
            int effLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, item);
            if (effLevel > 0 && !item.isEmpty()) {
                speed += effLevel * effLevel + 1;
            }
        }

        speed /= hardness;
        if (state.getMaterial().isToolNotRequired() || (!item.isEmpty() && item.canHarvestBlock(state))) {
            return speed / 30;
        } else {
            return speed / 100;
        }
    }

    public static int calculateEnchantmentCost(ItemStack item) {
        Map<Enchantment,Integer> enchantments = EnchantmentHelper.getEnchantments( item );
        return enchantments == null ? 0 : enchantments.size();
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;
        if (player.isPotionActive(MobEffects.HASTE)) {
            speed *= 1 + (player.getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2;
        }
        if (player.isPotionActive(MobEffects.MINING_FATIGUE)) {
            switch (player.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    speed *= 0.3;
                    break;
                case 1:
                    speed *= 0.09;
                    break;
                case 2:
                    speed *= 0.0027; // you might think that 0.09*0.3 = 0.027 so that should be next, that would make too much sense. it's 0.0027.
                    break;
                default:
                    speed *= 0.00081;
                    break;
            }
        }
        return speed;
    }
}
