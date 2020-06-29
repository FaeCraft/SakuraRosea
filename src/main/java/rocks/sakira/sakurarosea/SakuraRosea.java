package rocks.sakira.sakurarosea;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.sakira.sakurarosea.common.Materials;
import rocks.sakira.sakurarosea.common.block.Blocks;
import rocks.sakira.sakurarosea.common.entities.Entities;
import rocks.sakira.sakurarosea.common.entities.renderers.SakuraBoatEntityRenderer;
import rocks.sakira.sakurarosea.common.event.BlockClickedEventHandler;
import rocks.sakira.sakurarosea.common.feature.Features;
import rocks.sakira.sakurarosea.common.item.Items;
import rocks.sakira.sakurarosea.common.tileentities.TileEntities;
import rocks.sakira.sakurarosea.common.tileentities.renderers.SakuraChestTileEntityRenderer;
import rocks.sakira.sakurarosea.common.tileentities.renderers.SakuraSignTileEntityRenderer;
import rocks.sakira.sakurarosea.common.wood.FaeWoodType;
import rocks.sakira.sakurarosea.common.world.biome.Biomes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

import static rocks.sakira.sakurarosea.Constants.MOD_ID;

@Mod(MOD_ID)
public class SakuraRosea {
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        Atlases.SIGN_MATERIALS.put(FaeWoodType.SAKURA, new RenderMaterial(Atlases.SIGN_ATLAS, new ResourceLocation(MOD_ID, "entity/signs/sakura")));
    }

    public SakuraRosea() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.register(this);
        eventBus.register(BlockClickedEventHandler.class);

        Biomes.REGISTER.register(eventBus);
        Blocks.REGISTER.register(eventBus);
        Entities.REGISTER.register(eventBus);
        Features.REGISTER.register(eventBus);
        Items.REGISTER.register(eventBus);
        TileEntities.REGISTER.register(eventBus);
    }

    @SubscribeEvent
    public void setup(final FMLCommonSetupEvent event) throws NoSuchFieldException, IllegalAccessException {
        Biomes.addFeatures();
        Biomes.registerSpawn();
        Biomes.registerTypes();
        Biomes.registerEntries();

        // This block is pretty hacky, but I can't think of a better way to do this.
        // This is likely to break between major Forge versions.
        Field f;

        try {
            // Attempt to grab a reference to the validBlocks set and make it available.
            f = TileEntityType.SIGN.getClass().getDeclaredField("field_223046_I");
        } catch (NoSuchFieldException e) {
            // We may be in a development environment
            f = TileEntityType.SIGN.getClass().getDeclaredField("validBlocks");
        }

        try {
            f.setAccessible(true);  // Bypass `private` access modifier.

            // Create a list based on the current allowed blocks set, so we can add ours.
            Set<Block> allowedBlocks = (Set<Block>) f.get(TileEntityType.SIGN);
            ArrayList<Block> blocks = new ArrayList<>(allowedBlocks);

            // Add our blocks to the list.
            blocks.add(Blocks.SAKURA_SIGN_BLOCK.get());
            blocks.add(Blocks.SAKURA_WALL_SIGN_BLOCK.get());

            // Copy our blocks into a set, as required by TileEntityType.
            Set<Block> allAllowedBlocks = ImmutableSet.copyOf(blocks);

            // Finally, update the reference within the SIGN TileEntityType to the set we just made.
            f.set(TileEntityType.SIGN, allAllowedBlocks);
        } catch (IllegalAccessException e) {
            // If it didn't work, we should log the problem and then re-throw the exception.
            // If this fails, signs won't work - it's better to crash the game than allow it
            // to run with broken blocks or tile entities.
            LOGGER.error("Failed to set up allowable sign block types", e);
            throw e;
        }
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(Blocks.SAKURA_SAPLING_BLOCK.get(), RenderType.getCutout());

        ClientRegistry.bindTileEntityRenderer(
                TileEntities.SAKURA_CHEST_ENTITY.get(),
                SakuraChestTileEntityRenderer::new
        );

        ClientRegistry.bindTileEntityRenderer(
                TileEntities.SAKURA_SIGN_ENTITY.get(),
                SakuraSignTileEntityRenderer::new
        );

        Minecraft.getInstance().getRenderManager().register(
                Entities.SAKURA_BOAT_ENTITY.get(),
                new SakuraBoatEntityRenderer(Minecraft.getInstance().getRenderManager())
        );
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void textureStitchPre(final TextureStitchEvent.Pre event) {
        if (event.getMap().getTextureLocation().equals(Atlases.CHEST_ATLAS)) {
            event.addSprite(Materials.SAKURA_CHEST_MATERIAL.getTextureLocation());
            event.addSprite(Materials.SAKURA_CHEST_LEFT_MATERIAL.getTextureLocation());
            event.addSprite(Materials.SAKURA_CHEST_RIGHT_MATERIAL.getTextureLocation());
        }

        if (event.getMap().getTextureLocation().equals(Atlases.SIGN_ATLAS)) {
            event.addSprite(Materials.SAKURA_SIGN_MATERIAL.getTextureLocation());
        }

        if (event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
            event.addSprite(Materials.SAKURA_SHIELD_BASE.getTextureLocation());
            event.addSprite(Materials.SAKURA_SHIELD_NO_PATTERN.getTextureLocation());
        }
    }
}