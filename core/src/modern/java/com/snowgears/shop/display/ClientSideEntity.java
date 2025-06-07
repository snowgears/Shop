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
    }

    public ClientSideEntity(EntityType entityType, Location location, ItemStack itemStack, BlockFace facing) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = itemStack;
        this.facing = facing;
        this.armorStandData = null;
        this.displayText = null;
    }

    private ClientSideEntity(EntityType entityType, Location location, ItemStack itemStack, BlockFace facing, ArmorStandData armorStandData, String displayText) {
        this.entityId = getNextEntityId();
        this.entityType = entityType;
        this.location = location;
        this.itemStack = itemStack;
        this.facing = facing;
        this.armorStandData = armorStandData;
        this.displayText = displayText;
    }

    public static ClientSideEntity createItemFrame(Location location, ItemStack itemStack, BlockFace facing, boolean isGlowing) {
        EntityType entityType = isGlowing ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME;
        return new ClientSideEntity(entityType, location, itemStack, facing);
    }

    public static ClientSideEntity createArmorStand(Location location, ArmorStandData armorStandData, String text) {
        return new ClientSideEntity(EntityType.ARMOR_STAND, location, null, null, armorStandData, text);
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

    public int getEntityId() { return entityId; }

    /**
     * Spawns the client-side entity for the specified player.
     * Sends all necessary packets to properly display and configure the entity.
     */
    public void spawn(Player player) {
        // Create and send all required packets in correct order
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
        byte yaw = 0;
        if (entityType == EntityType.ARMOR_STAND && armorStandData != null) {
            // Convert yaw from degrees to protocol units: yaw * 256 / 360
            yaw = (byte) ((int) (armorStandData.getYaw() * 256.0F / 360.0F));
        }
        
        spawn.getBytes()
                .write(0, (byte) 0)  // Index 0: Pitch (Angle) - Vertical rotation (0 = level/horizontal)
                .write(1, yaw)       // Index 1: Yaw (Angle) - Horizontal rotation 
                .write(2, yaw);      // Index 2: Head Yaw (Angle) - Head rotation (same as body yaw)

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