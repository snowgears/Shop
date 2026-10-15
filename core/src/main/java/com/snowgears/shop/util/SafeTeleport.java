package com.snowgears.shop.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SafeTeleport {

    // Names instead of Material constants so older servers missing some of them still load this class
    private static final Set<String> HAZARDOUS_BODY = new HashSet<>(Arrays.asList(
            "LAVA", "FIRE", "SOUL_FIRE", "COBWEB", "SWEET_BERRY_BUSH", "WITHER_ROSE", "POWDER_SNOW",
            "NETHER_PORTAL", "END_PORTAL", "END_GATEWAY"));
    private static final Set<String> HAZARDOUS_GROUND = new HashSet<>(Arrays.asList(
            "MAGMA_BLOCK", "CAMPFIRE", "SOUL_CAMPFIRE", "CACTUS", "POINTED_DRIPSTONE"));

    private static final int[] NEARBY_FEET_OFFSETS = {0, -1, -2, 1, 2};

    private SafeTeleport() {}

    /**
     * Finds a spot in front of a shop sign where a player can stand without suffocating or taking damage,
     * looking at the sign. Returns null if there is none.
     */
    public static Location findStandingSpot(Block sign, BlockFace facing, Block chest) {
        List<Block> columns = new ArrayList<>();
        if (facing == BlockFace.NORTH || facing == BlockFace.EAST || facing == BlockFace.SOUTH || facing == BlockFace.WEST) {
            Block front = sign.getRelative(facing);
            columns.add(front);
            columns.add(front.getRelative(rotateClockwise(facing)));
            columns.add(front.getRelative(rotateClockwise(facing).getOppositeFace()));
            columns.add(front.getRelative(facing));
        } else {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                columns.add(sign.getRelative(face));
            }
        }

        for (int dy : NEARBY_FEET_OFFSETS) {
            for (Block column : columns) {
                Block feet = column.getRelative(0, dy, 0);
                if (canStandIn(feet))
                    return lookingAt(feet, sign);
            }
        }

        List<Block> fallbackColumns = new ArrayList<>();
        if (chest != null)
            fallbackColumns.add(chest);
        fallbackColumns.add(columns.get(0));
        for (Block column : fallbackColumns) {
            int maxY = column.getWorld().getMaxHeight();
            for (Block feet = column.getRelative(BlockFace.UP); feet.getY() < maxY - 1; feet = feet.getRelative(BlockFace.UP)) {
                if (canStandIn(feet))
                    return lookingAt(feet, sign);
            }
        }
        return null;
    }

    private static boolean canStandIn(Block feet) {
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        return isClear(feet) && isClear(head)
                && ground.isSolid() && !HAZARDOUS_GROUND.contains(ground.getType().name());
    }

    private static boolean isClear(Block block) {
        return !block.isSolid() && !HAZARDOUS_BODY.contains(block.getType().name());
    }

    private static Location lookingAt(Block feet, Block target) {
        Location loc = feet.getLocation().add(0.5, 0, 0.5);
        Location eye = loc.clone().add(0, 1.62, 0);
        loc.setDirection(target.getLocation().add(0.5, 0.5, 0.5).subtract(eye).toVector());
        return loc;
    }

    private static BlockFace rotateClockwise(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.EAST;
            case EAST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.WEST;
            default: return BlockFace.NORTH;
        }
    }
}
