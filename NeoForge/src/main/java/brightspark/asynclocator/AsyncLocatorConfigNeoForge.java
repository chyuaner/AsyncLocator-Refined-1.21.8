package brightspark.asynclocator;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public class AsyncLocatorConfigNeoForge {

	public static ModConfigSpec SPEC;
	public static ConfigValue<Integer> LOCATOR_THREADS;
	public static ConfigValue<Boolean> REMOVE_OFFER;

	// Feature toggles
	public static ConfigValue<Boolean> DOLPHIN_TREASURE_ENABLED;
	public static ConfigValue<Boolean> EYE_OF_ENDER_ENABLED;
	public static ConfigValue<Boolean> EXPLORATION_MAP_ENABLED;
	public static ConfigValue<Boolean> LOCATE_COMMAND_ENABLED;
	public static ConfigValue<Boolean> VILLAGER_TRADE_ENABLED;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
			LOCATOR_THREADS = builder
				.worldRestart()
				.comment(
					"The maximum number of threads in the async locator thread pool.",
					"There's no upper bound to this, however this should only be increased if you're experiencing",
					"simultaneous location lookups causing issues AND you have the hardware capable of handling",
					"the extra possible threads.",
					"The default of 1 should be suitable for most users."
					)
					.defineInRange("asyncLocatorThreads", 1, 1, Integer.MAX_VALUE);
				REMOVE_OFFER = builder
					.comment(
						"When a merchant's treasure map offer ends up not finding a feature location,",
						"remove the offer instead of marking it out of stock."
					)
					.define("removeMerchantInvalidMapOffer", false);

				builder.push("Feature Toggles");
				DOLPHIN_TREASURE_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for dolphin treasures.")
					.define("dolphinTreasureEnabled", true);
				EYE_OF_ENDER_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures when Eyes Of Ender are thrown.")
					.define("eyeOfEnderEnabled", true);
				EXPLORATION_MAP_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for exploration maps found in chests.")
					.define("explorationMapEnabled", true);
				LOCATE_COMMAND_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for the locate command.")
					.define("locateCommandEnabled", true);
				VILLAGER_TRADE_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for villager trades.")
					.define("villagerTradeEnabled", true);
				builder.pop();
				SPEC = builder.build();
			}
	}
