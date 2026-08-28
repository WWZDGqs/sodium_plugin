package net.minecraft.entity;

/**
 * 编译期桩：仅用于让 Mixin 能解析 `AbstractClientPlayerEntity.isSprinting()` 的调用。
 * 该类不会进入 mod jar（编译期 classpath 专用），运行时由游戏真实的
 * net.minecraft.entity.Entity 遮蔽，无任何冲突风险。
 */
public abstract class Entity {
    public boolean isSprinting() {
        return false;
    }
}
