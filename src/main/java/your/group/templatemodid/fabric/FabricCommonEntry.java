//? if fabric {
package your.group.templatemodid.fabric;

import net.fabricmc.api.ModInitializer;
import your.group.templatemodid.common.RockbreakerCommonEntry;

public class FabricCommonEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        RockbreakerCommonEntry.init();
    }
}
//?}