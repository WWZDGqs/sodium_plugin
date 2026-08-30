package ws.sodiunplugin.feature;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import ws.sodiunplugin.feature.ChestEspConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class ChestEspRenderer {
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ChestEspRenderer::onRenderWorld);
    }

    private static final int RESCAN_INTERVAL = 10;
    private static final float LINE_WIDTH = 2.5F;
    private static final int HUE_CYCLE_TICKS = 50;

    private static long lastScanTime = -1L;
    private static final List<Marked> marked = new ArrayList<>();
    private static boolean loggedTrigger = false;
    private static int layerBuildFailures = 0;
    private static int drawFrames = 0;

    private static RenderLayer espLayer = null;

    public static void onRenderWorld(WorldRenderContext context) {
        if (!ChestEspConfig.getEnabled()) return;
        if (!loggedTrigger) {
            System.out.println("[ChestEsp] AFTER_ENTITIES 渲染事件已触发，功能已开启");
            loggedTrigger = true;
        }
        try {
            World world = MinecraftClient.getInstance().world;
            if (world == null) return;
            if (espLayer == null && layerBuildFailures < 3) {
                espLayer = buildLayer();
                if (espLayer == null) {
                    layerBuildFailures++;
                    System.out.println("[ChestEsp] 穿墙层构建失败，回退到 RenderLayers.lines()");
                }
            }
            RenderLayer layer = espLayer != null ? espLayer : RenderLayers.LINES;

            long now = world.getTime();
            if (lastScanTime < 0 || now - lastScanTime >= RESCAN_INTERVAL) {
                lastScanTime = now;
                scan(world);
                System.out.println("[ChestEsp] 扫描完成，标记 " + marked.size() + " 个方块");
            }

            if (marked.isEmpty()) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            float hueBase = (now % HUE_CYCLE_TICKS) / (float) HUE_CYCLE_TICKS;
            MatrixStack matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(RenderSystem.getModelViewMatrix());

            int i = 0;
            for (Marked m : marked) {
                float hue = (hueBase + m.kind() * (1.0F / 3.0F) + i * 0.013F) % 1.0F;
                if (hue < 0) hue += 1.0F;
                int color = hsvToInt(hue, 1.0F, 1.0F);
                VertexConsumer vc = consumers.getBuffer(layer);
                VertexRendering.drawOutline(matrices, vc, m.shape(), m.ox(), m.oy(), m.oz(), color, LINE_WIDTH);
                i++;
            }

            drawFrames++;
            if (drawFrames % 60 == 0) {
                System.out.println("[ChestEsp] 已向活动渲染缓冲提交 " + marked.size() + " 个线框 (累计帧 " + drawFrames + ")");
            }
        } catch (Throwable t) {
            System.out.println("[ChestEsp] 渲染异常: " + t);
            t.printStackTrace();
        }
    }

    private static void scan(World world) {
        marked.clear();
        int range = ChestEspConfig.getRange();
        if (range <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int px = (int) client.player.getX();
        int pz = (int) client.player.getZ();
        int minCx = (px - range) >> 4;
        int maxCx = (px + range) >> 4;
        int minCz = (pz - range) >> 4;
        int maxCz = (pz + range) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                WorldChunk chunk = world.getChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Block block = be.getCachedState().getBlock();
                    int kind = kindOf(block);
                    if (kind < 0) continue;
                    BlockPos pos = be.getPos();
                    double d = Math.hypot(pos.getX() - px, pos.getZ() - pz);
                    if (d > range) continue;
                    double ox = pos.getX();
                    double oy = pos.getY();
                    double oz = pos.getZ();
                    VoxelShape shape = be.getCachedState().getOutlineShape(world, pos, net.minecraft.block.ShapeContext.absent());
                    if (shape == null || shape.isEmpty()) {
                        shape = net.minecraft.util.shape.VoxelShapes.fullCube();
                    }
                    marked.add(new Marked(kind, shape, ox, oy, oz));
                }
            }
        }
    }

    private static int kindOf(Block block) {
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
            return ChestEspConfig.getShowChest() ? 0 : -1;
        }
        if (block == Blocks.ENDER_CHEST) {
            return ChestEspConfig.getShowEnderChest() ? 2 : -1;
        }
        if (isShulker(block)) {
            return ChestEspConfig.getShowShulkerBox() ? 1 : -1;
        }
        return -1;
    }

    private static boolean isShulker(Block block) {
        String name = block.getTranslationKey();
        return name.contains("shulker");
    }

    private static int hsvToInt(float h, float s, float v) {
        int hi = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        int p = (int) (v * (1 - s) * 255);
        int q = (int) (v * (1 - f * s) * 255);
        int t = (int) (v * (1 - (1 - f) * s) * 255);
        int w = (int) (v * 255);
        int r, g, b;
        switch (hi) {
            case 0: r = w; g = t; b = p; break;
            case 1: r = q; g = w; b = p; break;
            case 2: r = p; g = w; b = t; break;
            case 3: r = p; g = q; b = w; break;
            case 4: r = t; g = p; b = w; break;
            default: r = w; g = p; b = q; break;
        }
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static RenderLayer buildLayer() {
        try {
            RenderPipeline lines = RenderPipelines.LINES;
            RenderPipeline.Snippet s = new RenderPipeline.Snippet(
                    java.util.Optional.of(lines.getVertexShader()),
                    java.util.Optional.of(lines.getFragmentShader()),
                    java.util.Optional.ofNullable(lines.getShaderDefines()),
                    java.util.Optional.of(lines.getSamplers()),
                    java.util.Optional.of(lines.getUniforms()),
                    lines.getBlendFunction(),
                    java.util.Optional.of(lines.getDepthTestFunction()),
                    java.util.Optional.of(lines.getPolygonMode()),
                    java.util.Optional.of(lines.isCull()),
                    java.util.Optional.of(lines.isWriteColor()),
                    java.util.Optional.of(lines.isWriteAlpha()),
                    java.util.Optional.of(lines.isWriteDepth()),
                    java.util.Optional.ofNullable(lines.getColorLogic()),
                    java.util.Optional.of(lines.getVertexFormat()),
                    java.util.Optional.of(lines.getVertexFormatMode()));
            RenderPipeline noDepth = RenderPipeline.builder(s)
                    .withLocation(Identifier.of("sodium_plugin", "chest_esp_no_depth"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build();
            return findRenderLayerOf(noDepth);
        } catch (Throwable t) {
            System.out.println("[ChestEsp] 构建穿墙管线失败: " + t);
            return null;
        }
    }

    private static RenderLayer findRenderLayerOf(RenderPipeline pipeline) {
        for (Method m : RenderLayer.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!RenderLayer.class.isAssignableFrom(m.getReturnType())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == String.class && p[1] == RenderPipeline.class) {
                try {
                    m.setAccessible(true);
                    return (RenderLayer) m.invoke(null, "sodium_plugin:chest_esp", pipeline);
                } catch (Throwable t) {
                    System.out.println("[ChestEsp] RenderLayer.of 反射调用失败: " + t);
                    return null;
                }
            }
        }
        System.out.println("[ChestEsp] 未找到 RenderLayer.of(String, RenderPipeline)");
        return null;
    }

    private record Marked(int kind, VoxelShape shape, double ox, double oy, double oz) {
    }
}
