package net.minecraft.entity;

public class LivingEntity extends Entity {
    public boolean isInvisibleTo(net.minecraft.entity.player.PlayerEntity viewer) {
        return false;
    }
}
