package com.s32thc.ThChatMod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.LiteralText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.command.*;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import static com.mojang.brigadier.arguments.StringArgumentType.*;

public class ThChatMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("th-chat-mod");
	Gson gson = new Gson();

	@Override
	public void onInitialize() {
		JsonElement blockedWordsJson = null;
		try {
			blockedWordsJson = JsonParser.parseReader(new FileReader("blocked_words.json"));
			if(blockedWordsJson.isJsonArray()) {
				FabricLoader.getInstance().getObjectShare().put("th-chat-mod:blockedWords", blockedWordsJson.getAsJsonArray());
			} else {
				FabricLoader.getInstance().getObjectShare().put("th-chat-mod:blockedWords", new JsonArray());
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("Failed to load blocked_words.json, proceeding with no list");
			FabricLoader.getInstance().getObjectShare().put("th-chat-mod:blockedWords", new JsonArray());
		}
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("addBlockedWord")
					.requires(source -> source.hasPermissionLevel(3))
					.then(CommandManager.argument("word", greedyString()).executes(context -> {
				JsonArray arr = (JsonArray)FabricLoader.getInstance().getObjectShare().get("th-chat-mod:blockedWords");
				String word = getString(context, "word");
				JsonPrimitive jWord = new JsonPrimitive(word);
				if(arr.contains(jWord)) {
					context.getSource().sendError(new LiteralText("Word " + word + " already in list!"));
					return 1;
				}
				arr.add(jWord);
				context.getSource().sendFeedback(new LiteralText("Word added: " + word), true);
				try {
					FileWriter w = new FileWriter("blocked_words.json");
					gson.toJson(arr, w);
					w.close();
				} catch(java.io.IOException e) {
					context.getSource().sendError(new LiteralText("Failed to write blocked_words.json"));
				}
				return 1;
			})));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("removeBlockedWord")
					.requires(source -> source.hasPermissionLevel(3))
					.then(CommandManager.argument("word", greedyString()).executes(context -> {
				JsonArray arr = (JsonArray)FabricLoader.getInstance().getObjectShare().get("th-chat-mod:blockedWords");
				String word = getString(context, "word");
				if(!arr.remove(new JsonPrimitive(word))) {
					context.getSource().sendError(new LiteralText("Word " + word + " is not in the list!"));
					return 1;
				}
				context.getSource().sendFeedback(new LiteralText("Word removed: " + word), true);
				try {
					FileWriter w = new FileWriter("blocked_words.json");
					gson.toJson(arr, w);
					w.close();
				} catch(java.io.IOException e) {
					context.getSource().sendError(new LiteralText("Failed to write blocked_words.json"));
				}
				return 1;
			})));
		});
		LOGGER.info("Hello Fabric world!");
	}
}
