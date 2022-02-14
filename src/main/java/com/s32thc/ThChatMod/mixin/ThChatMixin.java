package com.s32thc.ThChatMod.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.*;

import java.util.function.Consumer;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ThChatMixin implements ServerPlayPacketListener {

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    protected abstract void handleMessage(TextStream.Message message);

    @Shadow
    protected abstract void filterText(String text, Consumer<TextStream.Message> consumer);

    @Shadow
    public abstract void disconnect(Text text);

    /**
     * @author s32thc
     * @reason
     */
    @Overwrite
    public void onChatMessage(ChatMessageC2SPacket packet) {
        String string = StringUtils.normalizeSpace(packet.getChatMessage());



        for(int i = 0; i < string.length(); ++i) {
            if (!SharedConstants.isValidChar(string.charAt(i))) {
                this.disconnect(new TranslatableText("multiplayer.disconnect.illegal_characters"));
                return;
            }
        }

        if (string.startsWith("/")) {
            NetworkThreadUtils.forceMainThread(packet, this, this.player.getWorld());
            this.handleMessage(TextStream.Message.permitted(string));
        } else {
            JsonArray blockedWordsJson = (JsonArray)FabricLoader.getInstance().getObjectShare().get("th-chat-mod:blockedWords");
            for(int i = 0; i < blockedWordsJson.size(); i++) {
                JsonElement _e = blockedWordsJson.get(i);
                String word = _e.getAsString();

                int startIndex = 0;
                for(;;) {
                    int index = string.toUpperCase().indexOf(word.toUpperCase());
                    if(index == -1) break;
                    String preString = string.substring(startIndex, index);
                    String postString = string.substring(preString.length() + word.length());
                    string = preString + "[Hyperlink Blocked]" + postString;
                    startIndex = index + "[Hyperlink Blocked]".length();
                }
            }

            this.filterText(string, this::handleMessage);
        }
    }
}
