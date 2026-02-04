package dev.itsrealperson.vision_goggles.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import org.jetbrains.annotations.Nullable;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    
    @Invoker("loadEffect")
    void vision_goggles$loadEffect(ResourceLocation location);

    @Accessor("postEffect")
    @Nullable
    PostChain vision_goggles$getPostEffect();

    @Accessor("postEffect")
    void vision_goggles$setPostEffect(@Nullable PostChain postChain);
}
