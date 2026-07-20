package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class ZonedThermalVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final double baseY;
    private final double height;
    private final float attenuation;
    private double currentY;

    public ZonedThermalVertexConsumer(VertexConsumer delegate, double baseY, double height, float attenuation) {
        this.delegate = delegate;
        this.baseY = baseY;
        this.height = height;
        this.attenuation = attenuation;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.currentY = y;
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        double percent = (this.currentY - baseY) / height;
        
        int outR, outG, outB;
        if (percent < 0.45) {
            outR = 0; outG = 255; outB = 255;
        } else if (percent < 0.8) {
            outR = 255; outG = 64; outB = 0;
        } else {
            outR = 255; outG = 160; outB = 0;
        }
        
        int outA = 255; // Attenuation is applied via RenderSystem.setShaderColor in ThermalEntityPainter
        delegate.color(outR, outG, outB, outA);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }

    @Override
    public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }

    @Override
    public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }

    @Override
    public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }

    @Override
    public void endVertex() { delegate.endVertex(); }

    @Override
    public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }

    @Override
    public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}
