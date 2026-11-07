package com.addserverprecheck.mixin;

import net.minecraft.client.gui.screens.ManageServerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ManageServerScreen.class)
public interface ManageServerScreenAccessor {

    @Invoker("onAdd")
    void addserverprecheck$invokeOnAdd();
}