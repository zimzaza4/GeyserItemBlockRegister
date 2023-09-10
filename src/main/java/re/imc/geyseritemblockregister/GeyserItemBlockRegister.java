package re.imc.geyseritemblockregister;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import re.imc.geyseritemblockregister.reader.SimpleAddonBlocksReader;

import java.io.IOException;
import java.util.List;

public class GeyserItemBlockRegister implements Extension {
    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        this.dataFolder().toFile().mkdirs();
    }

    @Subscribe
    public void onGeyserDefineCustomBlocksEvent(GeyserDefineCustomBlocksEvent event) throws IOException {
        System.out.println("11111");
        SimpleAddonBlocksReader reader = new SimpleAddonBlocksReader();
        System.out.println("22222");
        List<CustomBlockData> blocks = reader.readBlocks(dataFolder());
        System.out.println("33333");
        blocks.forEach(b -> {

            event.register(b);
        });
    }

}
