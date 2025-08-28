package brightspark.asynclocator.platform;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.platform.services.ExplorationMapFunctionLogicHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class FabricExplorationMapFunctionLogicHelper implements ExplorationMapFunctionLogicHelper {
	@Override
	public void invalidateMap(ItemStack mapStack, ServerLevel level, BlockPos invPos) {
		handleUpdateMapInContainer(mapStack, level, invPos, (container, slot) -> {
			ALConstants.logDebug("Invalidating map in Fabric container slot {}", slot);
			container.setItem(slot, new ItemStack(Items.MAP));
		});
	}

	@Override
	public void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationTypeHolder,
		BlockPos invPos,
		@Nullable Component displayName
	) {
		handleUpdateMapInContainer(mapStack, level, invPos, (container, slot) -> {
		ItemStack actualStack = container.getItem(slot);

		CommonLogic.finalizeMap(actualStack, level, pos, scale, destinationTypeHolder, displayName);
			ALConstants.logDebug("Updated map in Fabric container slot {}, broadcasting changes.", slot);
			container.setItem(slot, actualStack);

		CommonLogic.clearPendingState(actualStack);
		});
	}

	// Now works with any container
	private static void handleUpdateMapInContainer(
		ItemStack mapStackToFind,
		ServerLevel level,
		BlockPos inventoryPos,
		BiConsumer<Container, Integer> handleSlotFound
	) {
		BlockEntity be = level.getBlockEntity(inventoryPos);
		if (be instanceof Container container) {
			boolean found = false;
			UUID targetId = CommonLogic.getTrackingUUID(mapStackToFind);

			if (targetId != null) {
				for (int i = 0; i < container.getContainerSize(); i++) {
					ItemStack slotStack = container.getItem(i);
					UUID slotId = CommonLogic.getTrackingUUID(slotStack);
					if (targetId.equals(slotId)) {
						handleSlotFound.accept(container, i);
						CommonLogic.broadcastContainerChanges(level, be, container);
						found = true;
						break;
					}
				}
			}

			if (!found) {
				ALConstants.logWarn("Could not find map with UUID {} in container {} at {}", targetId, be.getClass().getSimpleName(), inventoryPos);
			}
		} else {
			ALConstants.logWarn(
				"No Container at inventory position {} in level {}",
				inventoryPos, level.dimension().location()
			);
		}
	}
}