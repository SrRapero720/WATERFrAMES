package me.srrapero720.waterframes.common.block.entity;

import me.srrapero720.waterframes.WFConfig;
import me.srrapero720.waterframes.WaterFrames;
import me.srrapero720.waterframes.client.display.TextureDisplay;
import me.srrapero720.waterframes.common.block.DisplayBlock;
import me.srrapero720.waterframes.common.block.data.DisplayCaps;
import me.srrapero720.waterframes.common.block.data.DisplayData;
import me.srrapero720.waterframes.common.network.DisplayNetwork;
import me.srrapero720.waterframes.common.network.packets.*;
import me.srrapero720.watermedia.api.image.ImageAPI;
import me.srrapero720.watermedia.api.image.ImageCache;
import me.srrapero720.watermedia.api.math.MathAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;

import static me.srrapero720.waterframes.WaterFrames.LOGGER;

@EventBusSubscriber(modid = WaterFrames.ID, bus = EventBusSubscriber.Bus.GAME)
public class DisplayTile extends BlockEntity {
    private static long lagTickTime;

    public final DisplayData data;
    public final DisplayCaps caps;
    @OnlyIn(Dist.CLIENT) public ImageCache imageCache;
    @OnlyIn(Dist.CLIENT) public TextureDisplay display;
    @OnlyIn(Dist.CLIENT) private boolean isReleased;

    public DisplayTile(DisplayData data, DisplayCaps caps, BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.data = data;
        this.caps = caps;
    }

    public static void setLagTickTime(long ltt) {
        if (ltt < 60000) {
            lagTickTime = ltt / 50L;
        } else {
            LOGGER.warn("Rejected tick correction of {}ms, overpass watchdog time", ltt);
        }
    }

    public static void clearLagTickTime() {
        lagTickTime = 0;
    }

    @SubscribeEvent
    public static void onTickLast(ServerTickEvent.Post e) {
        clearLagTickTime();
    }

    @OnlyIn(Dist.CLIENT)
    public TextureDisplay activeDisplay() {
        return display;
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
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        this.data.save(nbt, this);
        super.saveAdditional(nbt, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        this.data.load(nbt, this);
        super.loadAdditional(nbt, registries);
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
        return this.caps.getBox(this, getDirection(), getAttachedFace(), true);
    }

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
        if (clientSide) DisplayNetwork.sendServer(new ActivePacket(this.getBlockPos(), mode, true));
        else            DisplayNetwork.sendClient(new ActivePacket(this.getBlockPos(), mode, true), this);
    }

    public void setMute(boolean clientSide, boolean mode) {
        if (clientSide) DisplayNetwork.sendServer(new MutePacket(this.getBlockPos(), mode, true));
        else            DisplayNetwork.sendClient(new MutePacket(this.getBlockPos(), mode, true), this);
    }

    public void setPause(boolean clientSide, boolean pause) {
        if (clientSide) DisplayNetwork.sendServer(new PausePacket(this.getBlockPos(), pause, this.data.tick, true));
        else            DisplayNetwork.sendClient(new PausePacket(this.getBlockPos(), pause, this.data.tick, true), this);
    }

    public void setStop(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new PausePacket(this.getBlockPos(), true, 0, true));
        else            DisplayNetwork.sendClient(new PausePacket(this.getBlockPos(), true, 0, true), this);
    }

    public void volumeUp(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new VolumePacket(this.getBlockPos(), this.data.volume + 5, true));
        else            DisplayNetwork.sendClient(new VolumePacket(this.getBlockPos(), this.data.volume + 5, true), this);
    }

    public void volumeDown(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new VolumePacket(this.getBlockPos(), this.data.volume - 5, true));
        else            DisplayNetwork.sendClient(new VolumePacket(this.getBlockPos(), this.data.volume - 5, true), this);
    }

    public void fastFoward(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.getBlockPos(), Math.min(data.tick + MathAPI.msToTick(5000), this.data.tickMax), this.data.tickMax, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.getBlockPos(), Math.min(data.tick + (5000 / 50), this.data.tickMax), this.data.tickMax, true), this);
    }

    public void rewind(boolean clientSide) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.getBlockPos(), Math.max(data.tick - MathAPI.msToTick(5000), 0), this.data.tickMax, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.getBlockPos(), Math.max(data.tick - (5000 / 50), 0), this.data.tickMax, true), this);
    }

    public void syncTime(boolean clientSide, long tick, long maxTick) {
        if (clientSide) DisplayNetwork.sendServer(new TimePacket(this.getBlockPos(), tick, maxTick, true));
        else            DisplayNetwork.sendClient(new TimePacket(this.getBlockPos(), tick, maxTick, true), this);
    }

    public void loop(boolean clientSide, boolean loop) {
        if (clientSide) DisplayNetwork.sendServer(new LoopPacket(this.getBlockPos(), loop, true));
        else            DisplayNetwork.sendClient(new LoopPacket(this.getBlockPos(), loop, true), this);
    }

    public void tick(BlockPos pos, BlockState state) {
        if (this.data.tickMax == -1 || this.data.tick < 0) this.data.tick = 0;

        if (!this.data.paused && this.data.active) {
            if (this.data.tick < this.data.tickMax) {
                this.data.tick++;
                if (lagTickTime != 0 && this.isServer()) {
                    long ticks = this.data.tick + lagTickTime;
                    while (ticks > this.data.tickMax) {
                        ticks -= this.data.tickMax;
                    }
                    this.data.tick = ticks;
                    this.setDirty();
                }
            } else {
                if (this.data.loop || this.data.tickMax == -1) this.data.tick = 0;
            }
        }

        boolean updateBlock = false;
        int redstoneOutput = 0;

        if (this.data.tickMax > 0 && this.data.active) {
            redstoneOutput = Math.round(((float) this.data.tick / (float) this.data.tickMax) * (BlockStateProperties.MAX_LEVEL_15 - 1)) + 1;
        }

        boolean lightOnPlay = WFConfig.useLightOnPlay();
        boolean lit = state.getValue(DisplayBlock.LIT);
        if (lightOnPlay && lit == (this.data.url.isEmpty())) {
            state = state.setValue(DisplayBlock.LIT, !this.data.url.isEmpty());
            updateBlock = true;
        } else if (!lightOnPlay && lit) {
            state = state.setValue(DisplayBlock.LIT, false);
            updateBlock = true;
        }

        if (state.getValue(DisplayBlock.POWER) != redstoneOutput) {
            state = state.setValue(DisplayBlock.POWER, redstoneOutput);
            updateBlock = true;
        }

        if (updateBlock) {
            level.setBlock(pos, state, DisplayBlock.UPDATE_ALL);
        }

        if (this.isClient()) {
            TextureDisplay display = this.requestDisplay();
            if (display != null && display.canTick()) display.tick();
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

    public Direction getAttachedFace() {
        return this.getBlockState().getValue(DisplayBlock.ATTACHED_FACE);
    }

    public boolean canHideModel() {
        return this.getBlockState().hasProperty(DisplayBlock.VISIBLE);
    }

    public boolean isVisible() {
        return this.getBlockState().getValue(DisplayBlock.VISIBLE);
    }

    public void setVisibility(boolean visible) {
        this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(DisplayBlock.VISIBLE, visible), DisplayBlock.UPDATE_CLIENTS);
    }

    public DisplayBlock getDisplayBlock() {
        return (DisplayBlock) this.getBlockState().getBlock();
    }

    public boolean isPowered() {
        return this.getBlockState().getValue(DisplayBlock.POWERED);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.data.load(tag, this);
        this.setDirty();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return super.saveWithFullMetadata(pRegistries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setDirty() {
        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), DisplayBlock.UPDATE_ALL);
        } else {
            LOGGER.warn("Cannot be stored block data, level is NULL");
        }
    }

    public static AlignedBox getBasicBox(DisplayTile tile) {
        final var facing = Facing.get(tile.getDirection());
        final var box = new AlignedBox();

        if (facing.positive) box.setMax(facing.axis, tile.data.projectionDistance);
        else box.setMin(facing.axis, 1 - tile.data.projectionDistance);

        Axis one = facing.one();
        Axis two = facing.two();

        if (facing.axis != Axis.Z) {
            one = facing.two();
            two = facing.one();
        }

        box.setMin(one, tile.data.min.x);
        box.setMax(one, tile.data.max.x);

        box.setMin(two, tile.data.min.y);
        box.setMax(two, tile.data.max.y);

        if (tile.caps.projects() && (facing.toVanilla() == Direction.NORTH || facing.toVanilla() == Direction.EAST)) {
            switch (tile.data.getPosX()) {
                case LEFT -> {
                    box.setMin(one, 1 - tile.data.getWidth());
                    box.setMax(one, 1);
                }
                case RIGHT -> {
                    box.setMax(one, tile.data.getWidth());
                    box.setMin(one, 0f);
                }
            }
        }

        if (!tile.caps.projects() && (facing.toVanilla() == Direction.WEST || facing.toVanilla() == Direction.SOUTH)) {
            switch (tile.data.getPosX()) {
                case LEFT -> {
                    box.setMin(one, 1 - tile.data.getWidth());
                    box.setMax(one, 1);
                }
                case RIGHT -> {
                    box.setMax(one, tile.data.getWidth());
                    box.setMin(one, 0f);
                }
            }
        }
        return box;
    }
}