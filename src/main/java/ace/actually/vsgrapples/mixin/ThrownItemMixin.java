package ace.actually.vsgrapples.mixin;

import ace.actually.vsgrapples.VSgrapples;
import com.yyon.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import com.yyon.grapplinghook.content.registry.GrappleModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.DimensionIdProvider;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ThrownEntity.class)
public abstract class ThrownItemMixin extends Entity {

	public ThrownItemMixin(EntityType<?> type, World world) {
		super(type, world);
	}


	@Shadow public abstract boolean shouldRender(double distance);



	@Inject(at = @At("HEAD"), method = "tick")
	private void init(CallbackInfo ci) {
		if(getWorld() instanceof ServerWorld world)
		{
			if(getType()== GrappleModEntities.GRAPPLE_HOOK.get())
			{
				GrapplinghookEntity entity = (GrapplinghookEntity) (Object) this;
				if(entity.isAttachedToSurface())
				{
					NbtCompound compound = getServer().getDataCommandStorage().get(VSgrapples.HOOK_DATA);
					if(compound.contains(getUuidAsString()))
					{
						DimensionIdProvider provider = (DimensionIdProvider) world;
						NbtList v = (NbtList) compound.get(getUuidAsString());
						Vec3d b = new Vec3d(v.getDouble(0),v.getDouble(1),v.getDouble(2));
						ChunkPos chunkPos = world.getChunk(BlockPos.ofFloored(b)).getPos();
						LoadedServerShip ship = (LoadedServerShip) ValkyrienSkiesMod.getVsCore().getHooks().getCurrentShipServerWorld().getLoadedShips().getByChunkPos(chunkPos.x,chunkPos.z, provider.getDimensionId());
						Vec3d go = VSGameUtilsKt.toWorldCoordinates(ship,b);
						teleport(go.x,go.y,go.z);

					}
					else
					{

						RaycastContext context = new RaycastContext(getPos(),getPos().add(1,1,1), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,null);
						BlockHitResult result = RaycastUtilsKt.clipIncludeShips(world,context,false);

						if(VSGameUtilsKt.isBlockInShipyard(world,result.getPos()))
						{
							List<String> removable = new ArrayList<>();
							for(String hook: compound.getKeys())
							{
								if(world.getEntity(UUID.fromString(hook))==null)
								{
									removable.add(hook);
								}
							}
							removable.forEach(compound::remove);

							NbtList xyz = new NbtList();
							xyz.add(NbtDouble.of(result.getPos().x));
							xyz.add(NbtDouble.of(result.getPos().y));
							xyz.add(NbtDouble.of(result.getPos().z));
							compound.put(getUuidAsString(),xyz);


							getServer().getDataCommandStorage().set(VSgrapples.HOOK_DATA,compound);

						}
						else
						{
							context = new RaycastContext(getPos(),getPos().add(-1,-1,-1), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,null);
							result = RaycastUtilsKt.clipIncludeShips(world,context,false);
							if(VSGameUtilsKt.isBlockInShipyard(world,result.getPos()))
							{
								List<String> removable = new ArrayList<>();
								for(String hook: compound.getKeys())
								{
									if(world.getEntity(UUID.fromString(hook))==null)
									{
										removable.add(hook);
									}
								}

								NbtList xyz = new NbtList();
								xyz.add(NbtDouble.of(result.getPos().x));
								xyz.add(NbtDouble.of(result.getPos().y));
								xyz.add(NbtDouble.of(result.getPos().z));
								compound.put(getUuidAsString(),xyz);

								getServer().getDataCommandStorage().set(VSgrapples.HOOK_DATA,compound);
							}
						}

					}

				}
			}
		}

	}


}