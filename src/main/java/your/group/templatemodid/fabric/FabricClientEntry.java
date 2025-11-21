//? if fabric {
package your.group.templatemodid.fabric;

import net.fabricmc.api.ClientModInitializer;
import your.group.templatemodid.client.RockbreakerClientEntry;

public class FabricClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RockbreakerClientEntry.init();
    }
}
//?}
