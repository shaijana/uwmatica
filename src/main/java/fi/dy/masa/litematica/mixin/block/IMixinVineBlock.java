package fi.dy.masa.litematica.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

@Mixin(VineBlock.class)
public interface IMixinVineBlock
{
    @Invoker("shouldHaveSide")
    boolean litematica_invokeShouldConnectUp(BlockView blockReader, BlockPos pos, Direction side);
}
