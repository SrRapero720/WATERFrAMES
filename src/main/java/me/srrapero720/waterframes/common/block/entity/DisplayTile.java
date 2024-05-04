package me.srrapero720.waterframes.common.block.entity;

import me.srrapero720.waterframes.client.display.TextureDisplay;
import me.srrapero720.waterframes.common.block.DisplayBlock;
import me.srrapero720.waterframes.common.block.data.DisplayData;
import me.srrapero720.waterframes.common.network.DisplayNetwork;
import me.srrapero720.waterframes.common.network.packets.*;
import me.srrapero720.watermedia.api.image.ImageAPI;
import me.srrapero720.watermedia.api.image.ImageCache;
import me.srrapero720.watermedia.api.math.MathAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;

import static me.srrapero720.waterframes.WaterFrames.LOGGER;

public abstract class DisplayTile extends BlockEntity {
    public final DisplayData data;
    @OnlyIn(Dist.CLIENT) public ImageCache imageCache;
    @OnlyIn(Dist.CLIENT) public TextureDisplay display;
    @OnlyIn(Dist.CLIENT) private boolean isReleased;

    public abstract boolean canHideModel();
    public abstract boolean canRenderBackside();
    public abstract boolean canProject();
    public abstract boolean canResize();
    public DisplayTile(DisplayData data, BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.data = data;
    }

    @OnlyIn(Dist.CLIENT)
    public TextureDisplay requestDisplay() {
        if (!this.data.active || (this.data.url.isEmpty() && display != null)) {
            this.cleanDisplay();
            return null;
        }

        if (this.isReleased) {
            this.imageCache = null;
            return null;
        }

        if (this.imageCache == null || !this.imageCache.url.equals(this.data.url)) {
            this.imageCache = ImageAPI.getCache(this.data.url, Minecraft.getInstance());
            this.cleanDisplay();
        }

        switch (imageCache.getStatus()) {
            case LOADING, FAILED, READY -> {
                if (this.display != null) return this.display;
                return this.display = new TextureDisplay(this);
            }

            case WAITING -> {
                this.cleanDisplay();
                this.imageCache.load();
                return display;
            }

            case FORGOTTEN -> {
                LOGGER.warn("Cached picture is forgotten, cleaning and reloading");
                this.imageCache = null;
                return null;
            }

            default -> {
                LOGGER.warn("WATERMeDIA Behavior is modified, this shouldn't be executed");
                return null;
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        this.data.save(nbt, this);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.data.load(nbt, this);
        super.load(nbt);
    }

    @OnlyIn(Dist.CLIENT)
    private void cleanDisplay() {
        if (this.display != null) {
            this.display.release();
            this.display = null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void release() {
        this.cleanDisplay();
        this.isReleased = true;
    }

    @OnlyIn(Dist.CLIENT)
    public AlignedBox getRenderBox() {
        final var facing = Facing.get(getDirection());
        final var box = new AlignedBox();

        if (facing.positive) box.setMax(facing.axis, this.data.projectionDistance);
        else box.setMin(facing.axis, 1 - this.data.projectionDistance);

        Axis one = facing.one();
        Axis two = facing.two();

        if (facing.axis != Axis.Z) {
            one = facing.two();
            two = facing.one();
        }

        box.setMin(one, this.data.min.x);
        box.setMax(one, this.data.max.x);

        box.setMin(two, this.data.min.y);
        box.setMax(two, this.data.max.y);

        if (canProject() && (facing.toVanilla() == Direction.NORTH || facing.toVanilla() == Direction.EAST)) {
            switch (this.data.getPosX()) {
                case LEFT -> {
                    box.setMin(one, 1 - this.data.getWidth());
                    box.setMax(one, 1);
                }
                case RIGHT -> {
                    box.setMax(one, this.data.getWidth());
                    box.setMin(one, 0f);
                }
            }
        }

        if (!canProject() && (facing.toVanilla() == Direction.WEST || facing.toVanilla() == Direction.SOUTH)) {
            switch (this.data.getPosX()) {
                case LEFT -> {
                    box.setMin(one, 1 - this.data.getWidth());
                    box.setMax(one, 1);
                }
                case RIGHT -> {
                    box.setMax(one, this.data.getWidth());
                    box.setMin(one, 0f);
                }
            }
        }
        return box;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return this.getRenderBox().getBB(this.getBlockPos());
    }

    public abstract boolean flip3DFace();
    public abstract float growSize();

    @Override
    public void setRemoved() {
        if (this.isClient()) this.release();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (this.isClient()) this.release();
        super.onChunkUnloaded();
    }

    public void setActive(boolean clientSide, boolean mode) {
        if (clientSide) DisplayNetwork.sendServer(new ActivePacket(this.worldPosition, mode, true));
        else            DisplayNetwork.sendClient(new ActivePacket(this.worldPosition, mode, true), this);
    }

    public void setMute(boolean clientSide, boolean mode) {
        if (clientSide) DisplayNetwork.sendServer(new MutePacket(this.worldPosition, mode, true));
        else            DisplayNetwork.sendClient(new MutePacket(this.worldPosition, mode, true), this);
    }

    public void setPause(boolean clientSide, boolean pause) {
        if (clientSide) DisplayNetwork.sendServer(new PausePacket(this.worldPosition, pause, this.data.tick, true));
        else            DisplayNetwork.sendClient(new PausePacket(this.worldPosition, pause, this.data.tick, true), this);
    }

    public void setStop(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new PausePacket(this.worldPosition, true, 0, true));
        else            DisplayNetwork.sendClient(new PausePacket(this.worldPosition, true, 0, true), this);
    }

    public void volumeUp(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new VolumePacket(this.worldPosition, this.data.volume + 5, true));
        else            DisplayNetwork.sendClient(new VolumePacket(this.worldPosition, this.data.volume + 5, true), this);
    }

    public void volumeDown(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new VolumePacket(this.worldPosition, this.data.volume - 5, true));
        else            DisplayNetwork.sendClient(new VolumePacket(this.worldPosition, this.data.volume - 5, true), this);
    }

    public void fastFoward(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.worldPosition, Math.min(data.tick + MathAPI.msToTick(5000), this.data.tickMax), this.data.tickMax, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.worldPosition, Math.min(data.tick + (5000 / 50), this.data.tickMax), this.data.tickMax, true), this);
    }

    public void rewind(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.worldPosition, Math.max(data.tick - MathAPI.msToTick(5000), 0), this.data.tickMax, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.worldPosition, Math.max(data.tick - (5000 / 50), 0), this.data.tickMax, true), this);
    }

    public void syncTime(boolean clientSide, long tick, long maxTick) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.worldPosition, tick, maxTick, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.worldPosition, tick, maxTick, true), this);
    }

    public void loop(boolean clientSide, boolean loop) {
        if (clientSide) DisplayNetwork.sendServer(new LoopPacket(this.worldPosition, loop, true));
        else            DisplayNetwork.sendClient(new LoopPacket(this.worldPosition, loop, true), this);
    }

    /* SPECIAL TICKS */
    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        if (be instanceof DisplayTile tile) {
            if (level.isClientSide) {
                TextureDisplay display = tile.requestDisplay();
                if (display != null && display.canTick()) display.tick(pos);
            }
            if (!tile.data.paused && tile.data.active) {
                if ((tile.data.tick <= tile.data.tickMax) || tile.data.tickMax == -1) {
                    tile.data.tick++;
                } else {
                    if (tile.data.loop) tile.data.tick = 0;
                }
            }

            int redstoneOutput = 0;
            if (tile.data.tick != -1 && tile.data.tickMax != -1 && tile.data.tick <= tile.data.tickMax && tile.data.active) {
                redstoneOutput = Math.round(((float) tile.data.tick / (float) tile.data.tickMax) * (BlockStateProperties.MAX_LEVEL_15 - 1)) + 1;
            }

            if (state.getValue(DisplayBlock.POWER) != redstoneOutput) {
                level.setBlock(pos, state.setValue(DisplayBlock.POWER, redstoneOutput), 3);
            }

            if (state.hasProperty(DisplayBlock.VISIBLE) && state.getValue(DisplayBlock.VISIBLE) != tile.data.frameVisibility) {
                level.setBlock(pos, state.setValue(DisplayBlock.VISIBLE, tile.data.frameVisibility), 0);
            }
        }
    }

    public boolean isClient() {
        return this.level != null && this.level.isClientSide;
    }

    public boolean isServer() {
        return !isClient();
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(this.getDisplayBlock().getFacing());
    }

    public DisplayBlock getDisplayBlock() {
        return (DisplayBlock) this.getBlockState().getBlock();
    }

    public boolean isPowered() {
        return this.getBlockState().getValue(DisplayBlock.POWERED);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.data.load(tag, this);
        this.setDirty();
    }

    public void setDirty() {
        if (this.level != null) {
            this.level.blockEntityChanged(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        } else {
            LOGGER.warn("Cannot be stored block data, level is NULL");
        }
    }

    @Override public @NotNull CompoundTag getUpdateTag() { return this.saveWithFullMetadata(); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
