package net.minecraft.entity.player;

/**
 * 编译期桩：仅用于 Mixin 中 `instanceof PlayerEntity` 的类型判断与
 * `isInvisibleTo` 的参数类型。不会进入 mod jar，运行时由游戏真实的
 * net.minecraft.entity.player.PlayerEntity 遮蔽。
 */
public class PlayerEntity {
}
