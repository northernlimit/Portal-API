package net.nhappy.portalapi.impl.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "portalapi")
public class PortalAPIConfig implements ConfigData {

    public int portalRenderDistance = 3;

    public static PortalAPIConfig get() {
        return AutoConfig.getConfigHolder(PortalAPIConfig.class).getConfig();
    }

    public static int getPortalViewDistance() {
        return getClampedPortalViewDistance() * 16;
    }

    public static int getClampedPortalViewDistance() {
        return Math.clamp(get().portalRenderDistance, 3, 16);
    }

}
