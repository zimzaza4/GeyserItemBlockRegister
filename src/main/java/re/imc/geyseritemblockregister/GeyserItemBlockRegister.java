package re.imc.geyseritemblockregister;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.populator.ItemRegistryPopulator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import re.imc.geyseritemblockregister.reader.SimpleAddonBlocksReader;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class GeyserItemBlockRegister implements Extension {


    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        this.dataFolder().toFile().mkdirs();
    }

    @Subscribe
    public void onGeyserDefineCustomBlocksEvent(GeyserDefineCustomBlocksEvent event) throws IOException {

        SimpleAddonBlocksReader reader = new SimpleAddonBlocksReader();
        List<CustomBlockData> blocks = reader.readBlocks(dataFolder());
        blocks.forEach(b -> {
            event.register(b);
        });

    }

    @Subscribe
    public void onGeyserLoaded(GeyserPostInitializeEvent event) {

        logger().info("Replacing Items...");
        for (CustomBlockData customBlock : BlockRegistries.CUSTOM_BLOCKS.get()) {
            for (ItemMappings value : Registries.ITEMS.get().values()) {
                ItemDefinition definition = value.getCustomBlockItemDefinitions().get(customBlock);
                for (ItemMapping item : value.getItems()) {
                    ListIterator<Pair<CustomItemOptions, ItemDefinition>> iter = item.getCustomItemOptions().listIterator();
                    while (iter.hasNext()) {
                        Pair<CustomItemOptions, ItemDefinition> customItemOption = iter.next();
                        if (customItemOption.value().getIdentifier().equals(customBlock.identifier())) {
                            // System.out.println("Replaced: " + customItemOption.value() + " with " + definition);
                            iter.set(Pair.of(customItemOption.first(), definition));
                        }
                    }
                }
                Int2ObjectMap<ItemDefinition> registry = value.getItemDefinitions();
                for (int i : registry.keySet()) {
                    ItemDefinition item = registry.get(i);
                    if (item.getIdentifier().equals(customBlock.identifier())) {
                        if (item.getRuntimeId() != definition.getRuntimeId()) {
                            registry.remove(i);
                            // System.out.println("Removed: " + item);
                        }
                    }
                }
            }
        }
        logger().info("Done");
    }

}
