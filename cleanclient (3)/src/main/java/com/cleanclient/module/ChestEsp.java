package com.cleanclient.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

/** Draws through-wall outlines around storage blocks. */
public class ChestEsp extends Module {
    private static final int MAX_CHUNK_RADIUS = 8;

    public ChestEsp() {
        super("Chest ESP", "Outlines storage blocks through walls");
    }

    public void render(WorldRenderContext ctx) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        ClientWorld world = mc.world;
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();

        int radius = Math.min(mc.options.getViewDistance().getValue(), MAX_CHUNK_RADIUS);
        int ccx = (int) Math.floor(cam.x) >> 4;
        int ccz = (int) Math.floor(cam.z) >> 4;

        // --- GL state: no depth test so outlines show through walls ---
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.rotate(camera.getRotation().conjugate(new Quaternionf())); // world -> view

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean any = false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = world.getChunk(ccx + dx, ccz + dz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int[] rgb = colorFor(be);
                    if (rgb == null) continue;
                    BlockPos p = be.getPos();
                    drawBox(buf,
                            (float) (p.getX() + 0.03 - cam.x), (float) (p.getY() + 0.0 - cam.y), (float) (p.getZ() + 0.03 - cam.z),
                            (float) (p.getX() + 0.97 - cam.x), (float) (p.getY() + 0.94 - cam.y), (float) (p.getZ() + 0.97 - cam.z),
                            rgb[0], rgb[1], rgb[2], 230);
                    any = true;
                }
            }
        }

        if (any) BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        mv.popMatrix();
    }

    private static int[] colorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return new int[]{255, 70, 70};   // trapped = red
        if (be instanceof ChestBlockEntity)        return new int[]{255, 190, 40};  // chest = gold
        if (be instanceof EnderChestBlockEntity)   return new int[]{170, 80, 255};  // ender = purple
        if (be instanceof BarrelBlockEntity)       return new int[]{190, 130, 70};  // barrel = brown
        if (be instanceof ShulkerBoxBlockEntity)   return new int[]{255, 120, 220}; // shulker = pink
        return null;
    }

    private static void drawBox(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
                                int r, int g, int bl, int a) {
        // bottom
        line(b, x1,y1,z1, x2,y1,z1, r,g,bl,a); line(b, x2,y1,z1, x2,y1,z2, r,g,bl,a);
        line(b, x2,y1,z2, x1,y1,z2, r,g,bl,a); line(b, x1,y1,z2, x1,y1,z1, r,g,bl,a);
        // top
        line(b, x1,y2,z1, x2,y2,z1, r,g,bl,a); line(b, x2,y2,z1, x2,y2,z2, r,g,bl,a);
        line(b, x2,y2,z2, x1,y2,z2, r,g,bl,a); line(b, x1,y2,z2, x1,y2,z1, r,g,bl,a);
        // verticals
        line(b, x1,y1,z1, x1,y2,z1, r,g,bl,a); line(b, x2,y1,z1, x2,y2,z1, r,g,bl,a);
        line(b, x2,y1,z2, x2,y2,z2, r,g,bl,a); line(b, x1,y1,z2, x1,y2,z2, r,g,bl,a);
    }

    private static void line(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
                             int r, int g, int bl, int a) {
        b.vertex(x1, y1, z1).color(r, g, bl, a);
        b.vertex(x2, y2, z2).color(r, g, bl, a);
    }
}
