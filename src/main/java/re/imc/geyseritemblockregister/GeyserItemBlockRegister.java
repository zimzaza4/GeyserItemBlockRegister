package re.imc.geyseritemblockregister;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.common.NamedDefinition;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import re.imc.geyseritemblockregister.reader.SimpleAddonBlocksReader;

import java.io.IOException;
import java.lang.reflect.Method;
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
        List<CustomBlockData> blocks = reader.readBlocks(dataFolder(), logger());
        blocks.forEach(b -> {
            try {
                event.register(b);
            } catch (Throwable t) {

            }
        });
    }

    @Subscribe
    public void onGeyserLoaded(GeyserPostInitializeEvent event) {
        logger().info("Replacing Items...");
        logger().info("S:" + BlockRegistries.CUSTOM_BLOCKS.get().length);
        for (CustomBlockData customBlock : BlockRegistries.CUSTOM_BLOCKS.get()) {
            for (ItemMappings value : Registries.ITEMS.get().values()) {
                ItemDefinition definition = value.getCustomBlockItemDefinitions().get(customBlock);
                for (ItemMapping item : value.getItems()) {
                    try {
                        Method getCustomItemOptionsMethod = item.getClass().getDeclaredMethod("getCustomItemOptions");
                        List<Object> customItemOptionsList = (List<Object>) getCustomItemOptionsMethod.invoke(item);
                        ListIterator<Object> iter = customItemOptionsList.listIterator();

                        while (iter.hasNext()) {
                            Object customItemOption = iter.next();
                            if (customItemOption == null) {
                                return;
                            }
                            Method getFirstMethod = customItemOption.getClass().getMethod("first");
                            Method getSecondMethod = customItemOption.getClass().getMethod("second");
                            CustomItemOptions options = (CustomItemOptions) getFirstMethod.invoke(customItemOption);

                            String identifier = ((ItemDefinition) getSecondMethod.invoke(customItemOption)).getIdentifier();

                            if (identifier.equals(customBlock.identifier())) {
                                // Set the new pair with reflection
                                iter.set(createPair(options, definition));
                            }
                        }
                    } catch (Exception e) {
                        logger().error("Error during reflection usage: ", e);
                    }
                }

                try {
                    Method getItemDefinitionsMethod = value.getClass().getDeclaredMethod("getItemDefinitions");
                    Map<Integer, ItemDefinition> registry = (Map<Integer, ItemDefinition>) getItemDefinitionsMethod.invoke(value);

                    for (int i : registry.keySet()) {
                        ItemDefinition item = registry.get(i);
                        if (item.getIdentifier().equals(customBlock.identifier())) {
                            if (item.getRuntimeId() != definition.getRuntimeId()) {
                                registry.remove(i);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger().error("Error during reflect: ", e);
                }
            }
        }
        logger().info("Done");
    }

    private Object createPair(CustomItemOptions first, ItemDefinition second) {
        try {
            PlatformType type = GeyserImpl.getInstance().platformType();

            String prefix = (type == PlatformType.STANDALONE || type == PlatformType.VELOCITY || type == PlatformType.BUNGEECORD || type == PlatformType.SPIGOT) ? "" : "org.geysermc.geyser.platform." + type.platformName().toLowerCase() + ".shaded.";

            Class<?> pairClass = Class.forName(prefix + "it.unimi.dsi.fastutil.Pair");
            Method ofMethod = pairClass.getDeclaredMethod("of", Object.class, Object.class);
            return ofMethod.invoke(null, first, second);
        } catch (Exception e) {
            logger().error("Could not create Pair: ", e);
            return null;
        }
    }
}
