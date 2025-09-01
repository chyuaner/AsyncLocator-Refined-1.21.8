package brightspark.asynclocator;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public class AsyncLocatorConfigNeoForge {

	private static final int DEFAULT_THREADS = 1;
	private static final int MAX_THREADS = 64;

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
					"There's an upper limit of 64. This should only be increased if you're experiencing",
					"simultaneous location lookups causing issues AND you have the hardware capable of handling",
					"the extra possible threads.",
					"The default of 1 should be suitable for most users.",
					"This value must not exceed 64."
					)
					.define("asyncLocatorThreads", DEFAULT_THREADS, value -> {

						if (!(value instanceof Integer)) {
							return false;
						}
						Integer intValue = (Integer) value;
				
						// Check bounds
						if (intValue < 1 || intValue > MAX_THREADS) {
							return false;
						}
				
						return true;
					});
			
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

	// Add validation method
	public static void validateConfig() {
		// This method can be used for additional runtime checks if needed
		int threads = LOCATOR_THREADS.get();
		if (threads < 1 || threads > MAX_THREADS) {
			// This shouldn't happen with the validator, but just in case
			ALConstants.logError(
				"Invalid locatorThreads value ({}). Must be between 1-64. Resetting to default ({}).",
				threads
			);
			LOCATOR_THREADS.set(DEFAULT_THREADS);
			LOCATOR_THREADS.save();
		}
	}
	}
