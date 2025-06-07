package com.snowgears.shop.display;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.snowgears.shop.util.ArmorStandData;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.BlockFace;
import org.bukkit.util.EulerAngle;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSideEntity {

    private static final WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.getItemStackSerializer(false);
    private static final FieldAccessor entityIdCounter = Accessors.getFieldAccessor(MinecraftReflection.getEntityClass(), AtomicInteger.class, true);

    private static int getNextEntityId() {
        return ((AtomicInteger) entityIdCounter.get(null)).incrementAndGet();
    }

    public ClientSideEntity(Location location, ItemStack itemStack) {
        // EntityType.ITEM corresponds to entity ID 35 in Minecraft 1.21.5 (protocol 770)
        // Note: Was DROPPED_ITEM in older versions, now unified as ITEM
        // https://wiki.vg/Protocol#Entity_Type
        this.entityType = EntityType.ITEM;

        this.entityId = getNextEntityId();
        this.location = location;
        this.itemStack = itemStack;
        this.facing = null;
        this.armorStandData = null;
        this.displayText = null;
        this.textDisplayOptions = null;
        this.yaw = 0.0f; // Default yaw for items
    }

    public ClientSideEntity(EntityType entityType, Location location, ItemStack itemStack, BlockFace facing) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = itemStack;
        this.facing = facing;
        this.armorStandData = null;
        this.displayText = null;
        this.textDisplayOptions = null;
        this.yaw = 0.0f; // Default yaw for item frames
    }

    private ClientSideEntity(EntityType entityType, Location location, ItemStack itemStack, BlockFace facing, ArmorStandData armorStandData, String displayText) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = itemStack;
        this.facing = facing;
        this.armorStandData = armorStandData;
        this.displayText = displayText;
        this.textDisplayOptions = null;
        this.yaw = armorStandData != null ? (float) armorStandData.getYaw() : 0.0f; // Use armor stand yaw or default
    }

    private ClientSideEntity(EntityType entityType, Location location, String displayText, TextDisplayOptions textDisplayOptions) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = null;
        this.facing = null;
        this.armorStandData = null;
        this.displayText = displayText;
        this.textDisplayOptions = textDisplayOptions;
        this.yaw = 0.0f; // Default yaw for text displays
    }

    private ClientSideEntity(EntityType entityType, Location location, String displayText, TextDisplayOptions textDisplayOptions, float yaw) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = null;
        this.facing = null;
        this.armorStandData = null;
        this.displayText = displayText;
        this.textDisplayOptions = textDisplayOptions;
        this.yaw = yaw;
    }

    public static ClientSideEntity createItemFrame(Location location, ItemStack itemStack, BlockFace facing, boolean isGlowing) {
        EntityType entityType = isGlowing ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
        return new ClientSideEntity(entityType, location, itemStack, facing);
    }

    public static ClientSideEntity createArmorStand(Location location, ArmorStandData armorStandData, String text) {
        return new ClientSideEntity(EntityType.ARMOR_STAND, location, null, null, armorStandData, text);
    }

    /**
     * Creates a Text Display entity for versions 1.19.4+
     * @param location The location to spawn the text display
     * @param text The text to display (supports JSON text components)
     * @param options Configuration options for the text display
     * @param yaw The rotation in degrees (0 = south, 90 = west, 180 = north, 270 = east)
     * @return A new ClientSideEntity configured as a Text Display
     */
    public static ClientSideEntity createTextDisplay(Location location, String text, TextDisplayOptions options, float yaw) {
        // Check if TEXT_DISPLAY EntityType is available
        try {
            EntityType.valueOf("TEXT_DISPLAY");
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] TEXT_DISPLAY EntityType not found! Server may not support Text Display entities.");
            throw new UnsupportedOperationException("TEXT_DISPLAY EntityType not available. Requires Minecraft 1.19.4+", e);
        }
        
        return new ClientSideEntity(EntityType.TEXT_DISPLAY, location, text, options != null ? options : new TextDisplayOptions(), yaw);
    }

    /**
     * Creates a Text Display entity with specified rotation and default options
     */
    public static ClientSideEntity createTextDisplay(Location location, String text, float yaw) {
        return createTextDisplay(location, text, new TextDisplayOptions(), yaw);
    }

    /**
     * Creates a Text Display entity with default options and no rotation
     */
    public static ClientSideEntity createTextDisplay(Location location, String text) {
        return createTextDisplay(location, text, new TextDisplayOptions(), 0.0f);
    }

    /**
     * Configuration options for Text Display entities
     */
    public static class TextDisplayOptions {
        private int lineWidth = 255;
        private int backgroundColor = 0x40000000; // Default background color
        private byte textOpacity = -1; // Fully opaque
        private boolean hasShadow = false;
        private boolean seeThrough = false;
        private boolean useDefaultBackground = false;
        private TextAlignment alignment = TextAlignment.CENTER;

        public int getLineWidth() { return lineWidth; }
        public TextDisplayOptions setLineWidth(int lineWidth) { this.lineWidth = lineWidth; return this; }

        public int getBackgroundColor() { return backgroundColor; }
        public TextDisplayOptions setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; return this; }

        public byte getTextOpacity() { return textOpacity; }
        public TextDisplayOptions setTextOpacity(byte textOpacity) { this.textOpacity = textOpacity; return this; }

        public boolean hasShadow() { return hasShadow; }
        public TextDisplayOptions setHasShadow(boolean hasShadow) { this.hasShadow = hasShadow; return this; }

        public boolean isSeeThrough() { return seeThrough; }
        public TextDisplayOptions setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; return this; }

        public boolean useDefaultBackground() { return useDefaultBackground; }
        public TextDisplayOptions setUseDefaultBackground(boolean useDefaultBackground) { this.useDefaultBackground = useDefaultBackground; return this; }

        public TextAlignment getAlignment() { return alignment; }
        public TextDisplayOptions setAlignment(TextAlignment alignment) { this.alignment = alignment; return this; }
    }

    public enum TextAlignment {
        CENTER(0), LEFT(1), RIGHT(2);
        
        private final int value;
        TextAlignment(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    /**
     * Destroys the specified entities for the given player.
     * @param player The player to destroy the entities for.
     * @param entityIds The IDs of the entities to destroy.
     */
    public static void destroyEntities(Player player, ArrayList<Integer> entityIds) {
        var destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, entityIds);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyPacket);
    }

    private final UUID uuid = UUID.randomUUID();
    private final int entityId;
    private final Location location;
    private final ItemStack itemStack;
    private final BlockFace facing;
    private final EntityType entityType;
    private final ArmorStandData armorStandData;
    private final String displayText;
    private final TextDisplayOptions textDisplayOptions;
    private final float yaw;

    public int getEntityId() { return entityId; }

    /**
     * Spawns the client-side entity for the specified player.
     * Sends all necessary packets to properly display and configure the entity.
     */
    public void spawn(Player player) {
        // Create and send all required packets in correct order
        try {
            var spawnPacket = createSpawnEntityPacket();
            var metadataPacket = createEntityMetadataPacket();

            // Send spawn and metadata packets
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawnPacket);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadataPacket);
            
            // Send equipment packet for armor stands with items
            if (entityType == EntityType.ARMOR_STAND && armorStandData != null && armorStandData.getEquipment() != null) {
                var equipmentPacket = createEquipmentPacket();
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, equipmentPacket);
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to spawn " + entityType + " entity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the spawn entity packet with all necessary entity configuration.
     * Equivalent to ClientboundAddEntityPacket from old implementation.
     */
    private PacketContainer createSpawnEntityPacket() {
        // Create SPAWN_ENTITY packet (0x01) - used to spawn a new entity on the client
        // Protocol Reference: https://wiki.vg/Protocol#Spawn_Entity
        var spawn = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);

        // === INTEGER FIELDS ===
        // ProtocolLib maps packet integers in order: [Entity ID, Velocity X, Velocity Y, Velocity Z]
        spawn.getIntegers()
                // Index 0: Entity ID (VarInt in protocol) - Unique identifier for this entity instance
                .write(0, entityId)
                // Index 1-3: Velocity components (Short in protocol, but ProtocolLib exposes as int)
                // All set to 0 for stationary entities (no initial velocity)
                // Velocity is measured in 1/8000 of a block per tick
                .write(1, 0)  // Velocity X: 0 = no horizontal movement (east/west)
                .write(2, 0)  // Velocity Y: 0 = no vertical movement (up/down) 
                .write(3, 0); // Velocity Z: 0 = no horizontal movement (north/south)
        
        
        // Item frames need to be facing a direction
        if (facing != null) { 
            // Index 4: data (varint in protocol) - controls the direction for item frames and paintings
            spawn.getIntegers().write(4, getDirectionInt(facing)); 
        }

        // === UUID FIELDS ===
        // Index 0: Entity UUID (UUID in protocol) - Unique identifier across all entities
        // Used for entity tracking and client-side management
        spawn.getUUIDs().write(0, uuid);

        // === ENTITY TYPE FIELDS ===
        // Index 0: Entity Type (VarInt in protocol) - Specifies what type of entity this is
        spawn.getEntityTypeModifier().write(0, entityType);

        // === BYTE FIELDS (ROTATION VALUES) ===
        // ProtocolLib maps packet bytes in order: [Pitch, Yaw, Head Yaw]
        // All rotation values are in protocol units: 256 units = 360 degrees, so 1 unit ≈ 1.40625°
        byte yawByte = 0;
        if (entityType == EntityType.ARMOR_STAND && armorStandData != null) {
            // Convert yaw from degrees to protocol units: yaw * 256 / 360
            yawByte = (byte) ((int) (armorStandData.getYaw() * 256.0F / 360.0F));
        } else if (entityType == EntityType.TEXT_DISPLAY) {
            // Convert yaw from degrees to protocol units for Text Display
            yawByte = (byte) ((int) (this.yaw * 256.0F / 360.0F));
        }
        
        spawn.getBytes()
                .write(0, (byte) 0)  // Index 0: Pitch (Angle) - Vertical rotation (0 = level/horizontal)
                .write(1, yawByte)   // Index 1: Yaw (Angle) - Horizontal rotation 
                .write(2, yawByte);  // Index 2: Head Yaw (Angle) - Head rotation (same as body yaw)

        // === DOUBLE FIELDS (POSITION COORDINATES) ===
        // ProtocolLib maps packet doubles in order: [X, Y, Z]
        // Coordinates are absolute world positions in blocks
        spawn.getDoubles()
                .write(0, location.getX())  // Index 0: X coordinate (Double) - East/West position
                .write(1, location.getY())  // Index 1: Y coordinate (Double) - Up/Down position  
                .write(2, location.getZ()); // Index 2: Z coordinate (Double) - North/South position

        return spawn;
    }

    /**
     * Creates entity metadata packet with comprehensive entity configuration.
     */
    private PacketContainer createEntityMetadataPacket() {
        // === ENTITY METADATA PACKET ===
        // Create ENTITY_METADATA packet (0x58) - used to send entity-specific data to the client
        // Protocol Reference: https://wiki.vg/Protocol#Set_Entity_Metadata
        var meta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

        // === INTEGER FIELDS ===
        // Index 0: Entity ID (VarInt in protocol) - Links this metadata to the spawned entity
        // Must match the Entity ID from the SPAWN_ENTITY packet above
        meta.getIntegers().write(0, entityId);

        // === DATA VALUE COLLECTION ===
        // Each entry contains: Index (Unsigned Byte), Type (VarInt), Value (varies by type)
        List<WrappedDataValue> metadataValues = new ArrayList<>();

        if (this.entityType == EntityType.ITEM) {
            // Index 5: No Gravity (Boolean) - Prevents the item from falling
            metadataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), true));
        }

        if (this.entityType == EntityType.ARMOR_STAND && this.armorStandData != null) {
            metadataValues.addAll(createArmorStandMetadata());
        }

        if (this.entityType == EntityType.TEXT_DISPLAY && this.displayText != null && this.textDisplayOptions != null) {
            metadataValues.addAll(createTextDisplayMetadata());
        }

        if (this.itemStack != null && (this.entityType == EntityType.ITEM || this.entityType.toString().contains("ITEM_FRAME"))) {
            metadataValues.add(
                // === ITEM ENTITY METADATA ===
                // Official Entity Metadata Reference: https://wiki.vg/Entity_metadata#Item_Entity
                // Index 8: "Item" field for Item entities (Entity ID 35 in protocol 770)
                // Type: Slot (ItemStack) - The item that this entity represents
                // This is the ONLY required metadata for Item entities to display properly
                new WrappedDataValue(
                    8,          // Metadata Index: Item entities use index 8 for their ItemStack
                    serializer, // Serializer: Converts Bukkit ItemStack to protocol format
                    MinecraftReflection.getMinecraftItemStack(itemStack) // Value: The actual item data
                )
            );
        }

        // === DATA VALUE COLLECTION ===
        // Index 0: Metadata array (Array of all the metadata entries in protocol)
        meta.getDataValueCollectionModifier()
            .write(0, metadataValues);

        return meta;
    }

    /**
     * Creates metadata values specific to Armor Stand entities
     */
    private List<WrappedDataValue> createArmorStandMetadata() {
        List<WrappedDataValue> metadataValues = new ArrayList<>();

        // Entity base flags (index 0) - Set invisible flag
        byte entityFlags = 0x20; // 0x20 = invisible flag
        metadataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), entityFlags));
        
        // Living Entity metadata - Health (index 9)
        metadataValues.add(new WrappedDataValue(9, WrappedDataWatcher.Registry.get(Float.class), 20.0f));
        
        // Armor Stand flags (index 15)
        byte armorStandFlags = 0x10; // Marker flag (no collision, small hitbox)
        if (armorStandData.isSmall()) armorStandFlags |= 0x01; // Small flag
        metadataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), armorStandFlags));
        
        // Right arm pose (index 19) if specified  
        if (armorStandData.getRightArmPose() != null) {
            EulerAngle pose = armorStandData.getRightArmPose();
            // Create proper Minecraft Rotations object - ProtocolLib requires exact NMS type
            Object rotations = createRotations(
                (float) Math.toDegrees(pose.getX()),
                (float) Math.toDegrees(pose.getY()), 
                (float) Math.toDegrees(pose.getZ())
            );
            metadataValues.add(new WrappedDataValue(19, 
                WrappedDataWatcher.Registry.get(MinecraftReflection.getMinecraftClass("core.Rotations")), 
                rotations));
        }
        
        // Custom name for text display (index 2 & 3)
        if (displayText != null && !displayText.trim().isEmpty()) {
            metadataValues.add(new WrappedDataValue(2, 
                WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                Optional.of(WrappedChatComponent.fromText(displayText).getHandle())));
            metadataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), true));
        }

        return metadataValues;
    }

    /**
     * Creates metadata values specific to Text Display entities
     */
    private List<WrappedDataValue> createTextDisplayMetadata() {
        List<WrappedDataValue> metadataValues = new ArrayList<>();

        // Add base entity metadata that might be required
        // Index 0: Entity flags - ensure entity is visible
        metadataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
        
        // Index 2: Custom name (empty for text displays)
        metadataValues.add(new WrappedDataValue(2, 
            WrappedDataWatcher.Registry.getChatComponentSerializer(true),
            Optional.empty()));
        
        // Index 3: Custom name visible (false for text displays)
        metadataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), false));

        // Display entity base metadata - Add minimal required values
        // Index 8: Interpolation delay
        metadataValues.add(new WrappedDataValue(8, WrappedDataWatcher.Registry.get(Integer.class), 0));
        
        // Index 9: Transformation interpolation duration  
        metadataValues.add(new WrappedDataValue(9, WrappedDataWatcher.Registry.get(Integer.class), 0));
        
        // Index 10: Position/Rotation interpolation duration
        metadataValues.add(new WrappedDataValue(10, WrappedDataWatcher.Registry.get(Integer.class), 0));

        // Text Display specific metadata
        // Index 23: Text content - Text Component (this is the most critical one)
        try {
            // Try different approaches to create the text component
            Object textComponent;
            if (displayText.trim().startsWith("{") && displayText.trim().endsWith("}")) {
                // JSON text component
                textComponent = WrappedChatComponent.fromJson(displayText).getHandle();
            } else {
                // Plain text component
                textComponent = WrappedChatComponent.fromText(displayText).getHandle();
            }
            
            metadataValues.add(new WrappedDataValue(23, 
                WrappedDataWatcher.Registry.getChatComponentSerializer(false),
                textComponent));
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to create text component for Text Display: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple text
            metadataValues.add(new WrappedDataValue(23, 
                WrappedDataWatcher.Registry.getChatComponentSerializer(false),
                WrappedChatComponent.fromText(displayText).getHandle()));
        }
        
        // Index 24: Line width - VarInt
        metadataValues.add(new WrappedDataValue(24, 
            WrappedDataWatcher.Registry.get(Integer.class), 
            textDisplayOptions.getLineWidth()));
        
        // Index 25: Background color - VarInt  
        metadataValues.add(new WrappedDataValue(25, 
            WrappedDataWatcher.Registry.get(Integer.class), 
            textDisplayOptions.getBackgroundColor()));
        
        // Index 26: Text opacity - Byte
        metadataValues.add(new WrappedDataValue(26, 
            WrappedDataWatcher.Registry.get(Byte.class), 
            textDisplayOptions.getTextOpacity()));
        
        // Index 27: Style flags - Byte bit mask
        byte styleFlags = 0;
        if (textDisplayOptions.hasShadow()) styleFlags |= 0x01;
        if (textDisplayOptions.isSeeThrough()) styleFlags |= 0x02;
        if (textDisplayOptions.useDefaultBackground()) styleFlags |= 0x04;
        // Alignment: bits 3-4, but we need to handle the encoding properly
        int alignment = textDisplayOptions.getAlignment().getValue();
        if (alignment == 1 || alignment == 3) { // LEFT
            styleFlags |= 0x08;
        } else if (alignment == 2) { // RIGHT  
            styleFlags |= 0x10;
        }
        // CENTER is 0, so no additional bits needed
        
        metadataValues.add(new WrappedDataValue(27, 
            WrappedDataWatcher.Registry.get(Byte.class), 
            styleFlags));

        return metadataValues;
    }
    
    /**
     * Creates equipment packet for armor stands to display items
     */
    private PacketContainer createEquipmentPacket() {
        var equipment = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        
        // Set entity ID
        equipment.getIntegers().write(0, entityId);

        // iterate through the equipment slots and add them to the equipment list
        EquipmentSlot slot = armorStandData.getEquipmentSlot();
        EnumWrappers.ItemSlot itemSlot;
        if (slot == EquipmentSlot.HAND) { itemSlot = EnumWrappers.ItemSlot.MAINHAND; } 
        else if (slot == EquipmentSlot.OFF_HAND) { itemSlot = EnumWrappers.ItemSlot.OFFHAND; } 
        else { itemSlot = EnumWrappers.ItemSlot.valueOf(slot.name()); }
        
        // Create equipment slot list - mainhand slot (0) with the item
        List<com.comphenix.protocol.wrappers.Pair<EnumWrappers.ItemSlot, ItemStack>> equipmentList = new ArrayList<>();
        equipmentList.add(new com.comphenix.protocol.wrappers.Pair<>(
            itemSlot, 
            armorStandData.getEquipment()
        ));
        
        // Set the equipment list
        equipment.getSlotStackPairLists().write(0, equipmentList);
        
        return equipment;
    }
    
    private int getDirectionInt(BlockFace facing){
        // https://minecraft.wiki/w/Java_Edition_protocol/Object_data#Item_Frame
        switch (facing){
            case DOWN:
                return 0;
            case UP:
                return 1;
            case NORTH:
                return 2;
            case SOUTH:
                return 3;
            case WEST:
                return 4;
            case EAST:
                return 5;
            default:
                return 2; // North
        }
    }

    /**
     * Creates a Minecraft Rotations object using minimal reflection.
     * This is necessary because ProtocolLib requires the exact NMS type for metadata serialization.
     */
    private Object createRotations(float x, float y, float z) {
        try {
            Class<?> rotationsClass = MinecraftReflection.getMinecraftClass("core.Rotations");
            return rotationsClass.getConstructor(float.class, float.class, float.class).newInstance(x, y, z);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Rotations object", e);
        }
    }
} 