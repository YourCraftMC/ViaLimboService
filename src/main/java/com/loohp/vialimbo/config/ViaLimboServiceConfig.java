package com.loohp.vialimbo.config;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.configuration.annotation.ConfigPath;
import cc.carm.lib.configuration.annotation.HeaderComments;
import cc.carm.lib.configuration.value.standard.ConfiguredValue;

@ConfigPath(root = true)
public class ViaLimboServiceConfig implements Configuration {
    @HeaderComments("Enable ViaBackwards")
    public static final ConfiguredValue<Boolean> VIA_BACKWARDS = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaRewind")
    public static final ConfiguredValue<Boolean> VIA_REWIND = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaLegacy")
    public static final ConfiguredValue<Boolean> VIA_LEGACY = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaAprilFools")
    public static final ConfiguredValue<Boolean> VIA_APRIL_FOOLS = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaBedrock")
    public static final ConfiguredValue<Boolean> VIA_BEDROCK = ConfiguredValue.of(true);
}
