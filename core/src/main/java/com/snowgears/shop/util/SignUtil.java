package com.snowgears.shop.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public class SignUtil {
    public static void setFacing(Block signBlock, BlockFace clickedFace) {
        if (!MCVersion.atLeast("1.13")) {
            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signBlock.getState().getData();
            sign.setFacingDirection(clickedFace);
            BlockState blockstate = signBlock.getState();
            MaterialData blockdata = blockstate.getData();
            blockdata.setData((byte)sign.getData());
            blockstate.update();
            return;
        }

        // if (MCVersion.atLeast("1.13")) {
        //     Directional wallSignData = (Directional) signBlock.getBlockData();
        //     wallSignData.setFacing(clickedFace);
        //     signBlock.setBlockData(wallSignData);
        // }
    }
}
