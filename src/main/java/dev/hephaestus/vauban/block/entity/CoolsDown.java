package dev.hephaestus.vauban.block.entity;

public interface CoolsDown {
    long getLastTickTime();

    boolean needsCooldown();

    void setCooldown(int cooldown);
}
