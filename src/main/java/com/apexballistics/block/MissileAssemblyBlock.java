package com.apexballistics.block;

import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileModuleItem;
import com.apexballistics.item.MissileSpecification;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MissileAssemblyBlock extends Block {
    public MissileAssemblyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (held.getItem() instanceof MissileModuleItem module) {
            ItemStack missile = findMissile(player);
            if (missile.isEmpty()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable(
                            "message.apexballistics.assembly_no_missile").withStyle(ChatFormatting.RED), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide && missile.getItem() instanceof MissileItem missileItem) {
                MissileSpecification old = MissileSpecification.fromStack(missile, missileItem.kind());
                MissileSpecification.write(missile, module.apply(old));
                Component moduleName = held.getHoverName().copy();
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(Component.translatable(
                        "message.apexballistics.module_installed", moduleName,
                        missile.getHoverName()).withStyle(ChatFormatting.GREEN), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.getItem() instanceof MissileItem missileItem) {
            if (!level.isClientSide) {
                MissileSpecification spec = MissileSpecification.fromStack(held, missileItem.kind());
                player.displayClientMessage(Component.translatable(
                        "message.apexballistics.missile_spec",
                        spec.guidance().getSerializedName(), spec.payload().getSerializedName(),
                        spec.fuse().getSerializedName(), spec.stages(),
                        Math.round(spec.reliability() * 100)).withStyle(ChatFormatting.AQUA), false);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemStack findMissile(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MissileItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
