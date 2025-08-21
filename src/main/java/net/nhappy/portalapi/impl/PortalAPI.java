package net.nhappy.portalapi.impl;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import net.nhappy.portalapi.api.PortalAPIInit;
import net.nhappy.portalapi.impl.config.PortalAPIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalAPI implements ModInitializer {
	public static final String MOD_ID = "portalapi";

	public static Identifier id(String id) {
		return Identifier.of(MOD_ID, id);
	}

	public static final Logger LOGGER = LoggerFactory.getLogger("Portal-API");

	@Override
	public void onInitialize() {
		AutoConfig.register(PortalAPIConfig.class, GsonConfigSerializer::new);
		PortalAPIInit.init();
	}
}