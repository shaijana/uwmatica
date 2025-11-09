package fi.dy.masa.litematica.mixin.screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    private MixinHandledScreen(Component title)
    {
        super(title);
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void litematica_renderSlotHighlightsPre(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory(drawContext, (AbstractContainerScreen<?>) (Object) this, this.minecraft);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void litematica_renderSlotHighlightsPost(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory(drawContext, (AbstractContainerScreen<?>) (Object) this, this.minecraft);
    }
}
