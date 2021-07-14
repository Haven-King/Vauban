package dev.hephaestus.vauban.item;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class OxidizableBlockItem extends BlockItem {
    public OxidizableBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text name = super.getName(stack);

        NbtCompound tag = stack.getTag();

        if (tag != null && tag.contains("BlockStateTag", NbtType.COMPOUND)) {
            NbtCompound blockTag = tag.getCompound("BlockStateTag");

            if (blockTag.contains("oxidization", NbtType.STRING)) {
                name = new TranslatableText("vauban.oxidization." + blockTag.getString("oxidization"), name);
            }

            if (blockTag.contains("waxed") && blockTag.getString("waxed").equals("true")) {
                name = new TranslatableText("vauban.waxed", name);
            }
        }

        return name;
    }
}
