
package com.addserverprecheck.mixin;

import java.net.UnknownHostException;

import com.addserverprecheck.WhiteLoadingDotsWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManageServerScreen.class)
public abstract class AddServerPrecheckMixin extends Screen {

    @Shadow @Final
    private ServerData serverData;

    @Shadow
    private EditBox nameEdit;

    @Shadow
    private EditBox ipEdit;

    @Shadow
    private Button addButton;

    @Unique
    private static final long addserverprecheck$CHECK_TIMEOUT = 10000L;

    @Unique
    private boolean addserverprecheck$checking = false;

    @Unique
    private boolean addserverprecheck$allowNextAdd = false;

    @Unique
    private boolean addserverprecheck$pingerReady = false;

    @Unique
    private long addserverprecheck$checkStartedAt;

    @Unique
    private String addserverprecheck$checkedAddress = "";

    @Unique
    private ServerStatusPinger addserverprecheck$pinger;

    @Unique
    private ServerData addserverprecheck$probeData;

    protected AddServerPrecheckMixin(Component title) {
        super(title);
    }

    // Intercept Minecraft's original Done button.
    @Inject(
            method = "onAdd",
            at = @At("HEAD"),
            cancellable = true
    )
    private void addserverprecheck$checkBeforeAdding(CallbackInfo ci) {

        // Allow Minecraft to add the server after successful validation
        // or when the player chooses Add Anyway.
        if (this.addserverprecheck$allowNextAdd) {
            this.addserverprecheck$allowNextAdd = false;
            return;
        }

        ci.cancel();

        if (!this.addserverprecheck$checking) {
            this.addserverprecheck$beginCheck();
        }
    }

    // Restore the loading state if the screen is resized.
    @Inject(method = "init", at = @At("TAIL"))
    private void addserverprecheck$restoreLoadingState(CallbackInfo ci) {

        if (this.addserverprecheck$checking) {
            this.addserverprecheck$applyLoadingState();
        }
    }

    // Update the pinger and check the validation timeout.
    @Inject(
            method = "extractRenderState",
            at = @At("HEAD")
    )
    private void addserverprecheck$tickValidation(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {

        if (!this.addserverprecheck$checking) {
            return;
        }

        // Tick the pinger only after its background initialization finishes.
        if (this.addserverprecheck$pingerReady
                && this.addserverprecheck$pinger != null) {

            this.addserverprecheck$pinger.tick();
        }

        if (this.addserverprecheck$probeData != null) {

            ServerData.State state =
                    this.addserverprecheck$probeData.state();

            // Legacy servers can respond using Minecraft's old ping.
            if (state == ServerData.State.SUCCESSFUL
                    || state == ServerData.State.INCOMPATIBLE) {

                this.addserverprecheck$finishSuccess();
                return;
            }

            // Minecraft marks a completed status-ping failure as UNREACHABLE.
            // Handle it immediately instead of waiting for our 10-second timeout.
            if (state == ServerData.State.UNREACHABLE) {
                this.addserverprecheck$finishFailure(
                        "unreachable",
                        false
                );
                return;
            }
        }

        // Stop waiting after 10 seconds.
        if (Util.getMillis() - this.addserverprecheck$checkStartedAt
                >= addserverprecheck$CHECK_TIMEOUT) {

            this.addserverprecheck$finishFailure(
                    "timeout",
                    !this.addserverprecheck$pingerReady
            );
        }
    }

    // Prevent Escape from closing the form during validation.
    @Inject(
            method = "onClose",
            at = @At("HEAD"),
            cancellable = true
    )
    private void addserverprecheck$preventClosingWhileChecking(
            CallbackInfo ci
    ) {

        if (this.addserverprecheck$checking) {
            ci.cancel();
        }
    }

    // Begin server validation.
    @Unique
    private void addserverprecheck$beginCheck() {

        this.addserverprecheck$checking = true;
        this.addserverprecheck$pingerReady = false;

        this.addserverprecheck$checkStartedAt = Util.getMillis();

        String name = this.nameEdit.getValue();
        String address = this.ipEdit.getValue();

        this.addserverprecheck$checkedAddress = address.trim();

        // Save the entered information.
        this.serverData.name = name;
        this.serverData.ip = address;

        // Create separate temporary server data for validation.
        ServerData localProbeData = new ServerData(
                name,
                address,
                ServerData.Type.OTHER
        );

        this.addserverprecheck$probeData = localProbeData;

        // Create Minecraft's server pinger.
        ServerStatusPinger localPinger = new ServerStatusPinger();

        this.addserverprecheck$pinger = localPinger;

        // Capture Minecraft's client instance.
        var client = this.minecraft;

        // Lock all controls and immediately display the white animation.
        this.addserverprecheck$applyLoadingState();

        // DNS resolution and pinger initialization run on a background thread.
        Thread validationThread = new Thread(() -> {

            try {

                localPinger.pingServer(
                        localProbeData,

                        // No persistent data needs to be saved.
                        () -> {},

                        // Minecraft received a valid server pong.
                        () -> client.execute(() -> {

                            if (this.addserverprecheck$pinger == localPinger
                                    && this.addserverprecheck$checking) {

                                this.addserverprecheck$finishSuccess();
                            }
                        }),

                        EventLoopGroupHolder.remote(false)
                );

                // Notify the Minecraft thread that initialization finished.
                client.execute(() ->
                        this.addserverprecheck$handlePingerReady(
                                localPinger,
                                localProbeData
                        )
                );

            } catch (UnknownHostException exception) {

                client.execute(() -> {

                    if (this.addserverprecheck$pinger == localPinger
                            && this.addserverprecheck$checking) {

                        this.addserverprecheck$finishFailure(
                                "hostname",
                                false
                        );

                    } else {
                        localPinger.removeAll();
                    }
                });

            } catch (RuntimeException exception) {

                client.execute(() -> {

                    if (this.addserverprecheck$pinger == localPinger
                            && this.addserverprecheck$checking) {

                        this.addserverprecheck$finishFailure(
                                "error",
                                false
                        );

                    } else {
                        localPinger.removeAll();
                    }
                });
            }

        }, "Add Server Precheck Validation");

        validationThread.setDaemon(true);
        validationThread.start();
    }

    // Called on Minecraft's main thread when pinger initialization finishes.
    @Unique
    private void addserverprecheck$handlePingerReady(
            ServerStatusPinger pinger,
            ServerData probeData
    ) {

        // If this validation already ended, clean up its pinger.
        if (this.addserverprecheck$pinger != pinger
                || !this.addserverprecheck$checking) {

            pinger.removeAll();

            if (this.addserverprecheck$pinger == pinger) {
                this.addserverprecheck$pinger = null;
                this.addserverprecheck$probeData = null;
                this.addserverprecheck$pingerReady = false;
            }

            return;
        }

        this.addserverprecheck$pingerReady = true;

        // A legacy server may already have responded.
        if (probeData.state() == ServerData.State.INCOMPATIBLE
                || probeData.state() == ServerData.State.SUCCESSFUL) {

            this.addserverprecheck$finishSuccess();
            return;
        }

        // A fast connection refusal can arrive before the first screen tick.
        if (probeData.state() == ServerData.State.UNREACHABLE) {
            this.addserverprecheck$finishFailure(
                    "unreachable",
                    false
            );
            return;
        }

        // Minecraft uses this message when the address cannot be resolved.
        if (this.addserverprecheck$isUnknownHostResult(probeData)) {

            this.addserverprecheck$finishFailure(
                    "hostname",
                    false
            );
        }
    }

    // Detect Minecraft's unresolved-host result.
    @Unique
    private boolean addserverprecheck$isUnknownHostResult(
            ServerData data
    ) {

        if (data.motd == null) {
            return false;
        }

        String actualMessage = data.motd.getString();

        String expectedMessage = Component.translatable(
                "multiplayer.status.cannot_connect"
        ).getString();

        return actualMessage.equals(expectedMessage);
    }

    // Disable all interactive controls and display the animated dots.
    @Unique
    private void addserverprecheck$applyLoadingState() {

        // Disable buttons, text fields and the resource-pack option.
        for (var child : this.children()) {

            if (child instanceof AbstractWidget widget) {
                widget.active = false;
            }
        }

        // Make sure both text fields cannot be edited.
        this.nameEdit.setEditable(false);
        this.nameEdit.setFocused(false);

        this.ipEdit.setEditable(false);
        this.ipEdit.setFocused(false);

        // Replace Done's text with an empty message.
        this.addButton.setMessage(Component.empty());
        this.addButton.active = false;

        this.clearFocus();

        // Add the white Minecraft-style animated dots.
        WhiteLoadingDotsWidget dots =
                new WhiteLoadingDotsWidget(this.font);

        dots.setX(
                this.addButton.getX()
                        + (this.addButton.getWidth() - dots.getWidth()) / 2
        );

        dots.setY(this.addButton.getY());

        this.addRenderableWidget(dots);
    }

    // The server responded successfully.
    @Unique
    private void addserverprecheck$finishSuccess() {

        if (!this.addserverprecheck$checking) {
            return;
        }

        this.addserverprecheck$checking = false;

        this.addserverprecheck$cleanupPinger();

        // Execute Minecraft's original Done action.
        this.addserverprecheck$allowNextAdd = true;

        ((ManageServerScreenAccessor) (Object) this)
                .addserverprecheck$invokeOnAdd();
    }

    // Show the appropriate error message.
    @Unique
    private void addserverprecheck$finishFailure(
            String reason,
            boolean startupStillRunning
    ) {

        if (!this.addserverprecheck$checking) {
            return;
        }

        this.addserverprecheck$checking = false;

        // Don't clean up while background initialization is still running.
        // It will be cleaned up when the background thread finishes.
        if (!startupStillRunning) {
            this.addserverprecheck$cleanupPinger();
        }

        // Preserve the player's entered information.
        this.serverData.name = this.nameEdit.getValue();
        this.serverData.ip = this.ipEdit.getValue();

        boolean localAddress =
                this.addserverprecheck$isLoopbackAddress(
                        this.addserverprecheck$checkedAddress
                );

        Component title;
        Component message;

        if (localAddress) {

            title = Component.literal(
                    "No local Minecraft server found"
            );

            message = Component.literal(
                    "Nothing responded to the Minecraft status check "
                    + "on your computer at "
                    + this.addserverprecheck$checkedAddress
                    + ".\n\n"
                    + "Make sure a local Minecraft server is running "
                    + "and that the port is correct.\n\n"
                    + "Would you like to add this address anyway?"
            );

        } else if ("hostname".equals(reason)) {

            title = Component.literal("Hostname not found");

            message = Component.literal(
                    "Minecraft couldn't resolve this server address: "
                    + this.addserverprecheck$checkedAddress
                    + ".\n\n"
                    + "Check the spelling and your internet connection."
                    + "\n\n"
                    + "Would you like to add it anyway?"
            );

        } else if ("timeout".equals(reason)) {

            title = Component.literal("Connection timed out");

            message = Component.literal(
                    "The server didn't respond within 10 seconds. "
                    + "It might be offline, slow, or blocking status requests."
                    + "\n\n"
                    + "Would you like to add it anyway?"
            );

        } else if ("unreachable".equals(reason)) {

            title = Component.literal("Server unreachable");

            message = Component.literal(
                    "Minecraft couldn't establish a connection to "
                    + this.addserverprecheck$checkedAddress
                    + ".\n\n"
                    + "The server might be offline or unreachable."
                    + "\n\n"
                    + "Would you like to add it anyway?"
            );

        } else {

            title = Component.literal("Couldn't verify server");

            message = Component.literal(
                    "Something went wrong while checking "
                    + this.addserverprecheck$checkedAddress
                    + ".\n\n"
                    + "Would you like to add it anyway?"
            );
        }

        this.minecraft.gui.setScreen(
                new ConfirmScreen(
                        confirmed -> {

                            // Preserve both fields before returning to the form.
                            this.serverData.name = this.nameEdit.getValue();
                            this.serverData.ip = this.ipEdit.getValue();

                            // Return to the original Add Server screen.
                            this.minecraft.gui.setScreen(
                                    (Screen) (Object) this
                            );

                            if (confirmed) {

                                // The player chose Add Anyway.
                                this.addserverprecheck$allowNextAdd = true;

                                ((ManageServerScreenAccessor)
                                        (Object) this)
                                        .addserverprecheck$invokeOnAdd();
                            }
                        },

                        title,
                        message,

                        Component.literal("Add Anyway"),
                        Component.literal("Go Back")
                )
        );
    }

    // Detect localhost and loopback IP addresses.
    @Unique
    private boolean addserverprecheck$isLoopbackAddress(
            String enteredAddress
    ) {

        if (enteredAddress == null || enteredAddress.isBlank()) {
            return false;
        }

        String host = enteredAddress.trim();

        // Handle IPv6 addresses with square brackets.
        if (host.startsWith("[") && host.contains("]")) {

            host = host.substring(1, host.indexOf(']'));

        } else {

            // Remove a port from ordinary hostname:port addresses.
            int firstColon = host.indexOf(':');
            int lastColon = host.lastIndexOf(':');

            if (firstColon >= 0 && firstColon == lastColon) {
                host = host.substring(0, lastColon);
            }
        }

        // Ignore a trailing DNS dot.
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }

        return host.equalsIgnoreCase("localhost")
                || host.equalsIgnoreCase("localhost.localdomain")
                || host.equals("127.0.0.1")
                || host.startsWith("127.")
                || host.equals("::1")
                || host.equals("0:0:0:0:0:0:0:1");
    }

    // Release the server pinger and temporary data.
    @Unique
    private void addserverprecheck$cleanupPinger() {

        if (this.addserverprecheck$pinger != null) {
            this.addserverprecheck$pinger.removeAll();
            this.addserverprecheck$pinger = null;
        }

        this.addserverprecheck$probeData = null;
        this.addserverprecheck$pingerReady = false;
    }
}
