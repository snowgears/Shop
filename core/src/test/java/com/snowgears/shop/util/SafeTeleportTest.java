package com.snowgears.shop.util;

import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafeTeleportTest extends BaseMockBukkitTest {

    private static final int Y = 100;

    private World world;
    private Block chest;
    private Block sign;

    @BeforeEach
    void buildShop() {
        world = getServer().addSimpleWorld("world");
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                world.getBlockAt(x, Y - 1, z).setType(Material.STONE);
        chest = world.getBlockAt(0, Y, 0);
        chest.setType(Material.CHEST);
        sign = world.getBlockAt(0, Y, 1);
        sign.setType(Material.OAK_WALL_SIGN);
    }

    private Location find() {
        return SafeTeleport.findStandingSpot(sign, BlockFace.SOUTH, chest);
    }

    private void assertStandable(Location loc) {
        assertNotNull(loc);
        Block feet = loc.getBlock();
        assertFalse(feet.isSolid(), "feet in " + feet.getType());
        assertFalse(feet.getRelative(BlockFace.UP).isSolid(), "head in " + feet.getRelative(BlockFace.UP).getType());
        assertTrue(feet.getRelative(BlockFace.DOWN).isSolid(), "no ground under " + feet);
    }

    @Test
    void openFloorLandsInFrontOfSignFacingIt() {
        Location loc = find();
        assertStandable(loc);
        assertEquals(new Location(world, 0.5, Y, 2.5).toVector(), loc.toVector());
        assertEquals(180f, Math.abs(Location.normalizeYaw(loc.getYaw())), 0.01f);
    }

    @Test
    void ceilingAtHeadHeightInFrontIsAvoided() {
        world.getBlockAt(0, Y + 1, 2).setType(Material.STONE);

        Location loc = find();

        assertStandable(loc);
        assertNotEquals(new Location(world, 0.5, Y, 2.5).toVector(), loc.toVector());
    }

    @Test
    void wallInFrontOfSignLandsBesideItAtTheSameLevel() {
        world.getBlockAt(0, Y, 2).setType(Material.STONE);
        world.getBlockAt(0, Y + 1, 2).setType(Material.STONE);

        Location loc = find();

        assertStandable(loc);
        assertEquals(Y, loc.getBlockY());
        assertEquals(2, loc.getBlockZ());
    }

    @Test
    void lavaInFrontOfSignIsAvoided() {
        world.getBlockAt(0, Y, 2).setType(Material.LAVA);

        Location loc = find();

        assertStandable(loc);
        assertNotEquals(Material.LAVA, loc.getBlock().getType());
    }

    @Test
    void signAboveFloorLandsOnTheFloor() {
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                world.getBlockAt(x, Y - 1, z).setType(Material.AIR);
        world.getBlockAt(0, Y - 1, 0).setType(Material.CHEST);
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                if (x != 0 || z != 0)
                    world.getBlockAt(x, Y - 2, z).setType(Material.STONE);

        Location loc = find();

        assertStandable(loc);
        assertEquals(new Location(world, 0.5, Y - 1, 2.5).toVector(), loc.toVector());
    }

    @Test
    void enclosedSignFallsBackToTopOfTheChest() {
        for (int x = -3; x <= 3; x++)
            for (int z = 1; z <= 3; z++)
                for (int y = Y - 2; y <= Y + 2; y++)
                    if (x != 0 || z != 1 || y != Y)
                        world.getBlockAt(x, y, z).setType(Material.STONE);

        Location loc = find();

        assertStandable(loc);
        assertEquals(new Location(world, 0.5, Y + 1, 0.5).toVector(), loc.toVector());
    }
}
