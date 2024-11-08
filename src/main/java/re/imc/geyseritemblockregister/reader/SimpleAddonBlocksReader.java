package re.imc.geyseritemblockregister.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.*;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.util.CreativeCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SimpleAddonBlocksReader {

    public static final JsonMapper MAPPER = new JsonMapper();

    public List<CustomBlockData> readBlocks(Path root, ExtensionLogger logger) throws IOException {

        List<CustomBlockData> dataList = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(path -> {
                if (path.toFile().getName().endsWith(".json")) {
                    try {

                        JsonNode json = MAPPER.readTree(path.toFile()).get("minecraft:block");
                        if (json == null) {
                            return;
                        }
                        String id = json.get("description").get("identifier").asText().split(":", 2)[1];
                        JsonNode node = json.get("components");
                        CustomBlockData.Builder blobkBuilder = CustomBlockData.builder();
                        blobkBuilder.name(id)
                                .creativeCategory(CreativeCategory.NONE);

                        CustomBlockComponents.Builder componentsBuilder = CustomBlockComponents.builder();

                        componentsBuilder.placeAir(true);

                        if (node.has("minecraft:geometry")) {
                            JsonNode geometry = node.get("minecraft:geometry");
                            GeometryComponent.Builder geometryBuilder = GeometryComponent.builder();
                            geometryBuilder.identifier(geometry.asText());
                            componentsBuilder.geometry(geometryBuilder.build());
                        }

                        if (node.has("minecraft:material_instances")) {
                            JsonNode materialInstances = node.get("minecraft:material_instances");
                            if (materialInstances.isObject()) {
                                materialInstances.fields().forEachRemaining(entry -> {
                                    String key = entry.getKey();
                                    JsonNode value = entry.getValue();
                                    if (value.isObject()) {
                                        MaterialInstance materialInstance = createMaterialInstanceComponent(value);
                                        componentsBuilder.materialInstance(key, materialInstance);
                                    }
                                });
                            }
                        }

                        if (node.has("minecraft:placement_filter")) {
                            JsonNode placementFilter = node.get("minecraft:placement_filter");
                            if (placementFilter.isObject()) {
                                if (placementFilter.has("conditions")) {
                                    JsonNode conditions = placementFilter.get("conditions");
                                    if (conditions.isArray()) {
                                        List<PlacementConditions> filter = createPlacementFilterComponent(conditions);
                                        componentsBuilder.placementFilter(filter);
                                    }
                                }
                            }
                        }

                        dataList.add(blobkBuilder.components(componentsBuilder.build())
                                .build());

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }

            });
        }

        return dataList;
    }


    private MaterialInstance createMaterialInstanceComponent(JsonNode node) {
        // Set default values, and use what the user provides if they have provided something
        String texture = null;
        if (node.has("texture")) {
            texture = node.get("texture").asText();
        }

        String renderMethod = "opaque";
        if (node.has("render_method")) {
            renderMethod = node.get("render_method").asText();
        }

        boolean faceDimming = true;
        if (node.has("face_dimming")) {
            faceDimming = node.get("face_dimming").asBoolean();
        }

        boolean ambientOcclusion = true;
        if (node.has("ambient_occlusion")) {
            ambientOcclusion = node.get("ambient_occlusion").asBoolean();
        }

        return MaterialInstance.builder()
                .texture(texture)
                .renderMethod(renderMethod)
                .faceDimming(faceDimming)
                .ambientOcclusion(ambientOcclusion)
                .build();
    }
    private List<PlacementConditions> createPlacementFilterComponent(JsonNode node) {
        List<PlacementConditions> conditions = new ArrayList<>();

        // The structure of the placement filter component is the most complex of the current components
        // Each condition effectively separated into two arrays: one of allowed faces, and one of blocks/block Molang queries
        node.forEach(condition -> {
            Set<PlacementConditions.Face> faces = EnumSet.noneOf(PlacementConditions.Face.class);
            if (condition.has("allowed_faces")) {
                JsonNode allowedFaces = condition.get("allowed_faces");
                if (allowedFaces.isArray()) {
                    allowedFaces.forEach(face -> faces.add(PlacementConditions.Face.valueOf(face.asText().toUpperCase())));
                }
            }

            LinkedHashMap<String, PlacementConditions.BlockFilterType> blockFilters = new LinkedHashMap<>();
            if (condition.has("block_filter")) {
                JsonNode blockFilter = condition.get("block_filter");
                if (blockFilter.isArray()) {
                    blockFilter.forEach(filter -> {
                        if (filter.isObject()) {
                            if (filter.has("tags")) {
                                JsonNode tags = filter.get("tags");
                                blockFilters.put(tags.asText(), PlacementConditions.BlockFilterType.TAG);
                            }
                        } else if (filter.isTextual()) {
                            blockFilters.put(filter.asText(), PlacementConditions.BlockFilterType.BLOCK);
                        }
                    });
                }
            }

            conditions.add(new PlacementConditions(faces, blockFilters));
        });

        return conditions;
    }
}
