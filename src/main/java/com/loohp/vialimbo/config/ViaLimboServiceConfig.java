package com.loohp.vialimbo.config;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.configuration.annotation.ConfigPath;
import cc.carm.lib.configuration.annotation.HeaderComments;
import cc.carm.lib.configuration.value.standard.ConfiguredValue;

@ConfigPath(root = true)
public interface ViaLimboServiceConfig extends Configuration {
    @HeaderComments("Enable ViaBackwards")
    ConfiguredValue<Boolean> VIA_BACKWARDS = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaRewind")
    ConfiguredValue<Boolean> VIA_REWIND = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaLegacy")
    ConfiguredValue<Boolean> VIA_LEGACY = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaAprilFools")
    ConfiguredValue<Boolean> VIA_APRIL_FOOLS = ConfiguredValue.of(true);
    @HeaderComments("Enable ViaBedrock")
    ConfiguredValue<Boolean> VIA_BEDROCK = ConfiguredValue.of(true);
}
