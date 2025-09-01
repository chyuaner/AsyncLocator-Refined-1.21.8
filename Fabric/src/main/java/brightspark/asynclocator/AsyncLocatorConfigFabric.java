package brightspark.asynclocator;

import brightspark.asynclocator.SparkConfig.Category;
import brightspark.asynclocator.SparkConfig.Config;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsyncLocatorConfigFabric {

	private static final int DEFAULT_THREADS = 1;
	private static final int MAX_THREADS = 64;

	@Config(
		value = "asyncLocatorThreads",
		comment = """
			The maximum number of threads in the async locator thread pool.
			There's an upper limit of 64. This should only be increased if you're experiencing
			simultaneous location lookups causing issues AND you have the hardware capable of handling
			the extra possible threads.
			The default of 1 should be suitable for most users.
			This value must not exceed 64.
			""",
		min = DEFAULT_THREADS,
		max = MAX_THREADS // Practically in no case will you need the maximum amount
	)
	public static int LOCATOR_THREADS = 1;
	@Config(
		value = "removeMerchantInvalidMapOffer",
		comment = """
			When a merchant's treasure map offer ends up not finding a feature location,
			whether the offer should be removed or marked as out of stock.
			"""
	)
	public static boolean REMOVE_OFFER = false;

	@Category("Feature Toggles")
	public static class FeatureToggles {
		@Config(
			value = "dolphinTreasureEnabled",
			comment = "If true, enables asynchronous locating of structures for dolphin treasures."
		)
		public static boolean DOLPHIN_TREASURE_ENABLED = true;
		@Config(
			value = "eyeOfEnderEnabled",
			comment = "If true, enables asynchronous locating of structures when Eyes Of Ender are thrown."
		)
		public static boolean EYE_OF_ENDER_ENABLED = true;
		@Config(
			value = "explorationMapEnabled",
			comment = "If true, enables asynchronous locating of structures for exploration maps found in chests."
		)
		public static boolean EXPLORATION_MAP_ENABLED = true;
		@Config(
			value = "locateCommandEnabled",
			comment = "If true, enables asynchronous locating of structures for the locate command."
		)
		public static boolean LOCATE_COMMAND_ENABLED = true;
		@Config(
			value = "villagerTradeEnabled",
			comment = "If true, enables asynchronous locating of structures for villager trades."
		)
		public static boolean VILLAGER_TRADE_ENABLED = true;
	}

	private AsyncLocatorConfigFabric() {}

	//Helper method
	private static void resetToDefaults() {
		LOCATOR_THREADS = DEFAULT_THREADS;
		REMOVE_OFFER = false;
		FeatureToggles.DOLPHIN_TREASURE_ENABLED = true;
		FeatureToggles.EYE_OF_ENDER_ENABLED = true;
		FeatureToggles.EXPLORATION_MAP_ENABLED = true;
		FeatureToggles.LOCATE_COMMAND_ENABLED = true;
		FeatureToggles.VILLAGER_TRADE_ENABLED = true;
	}

	public static void init() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ALConstants.MOD_ID + ".properties");

		if (Files.exists(configFile)) {
			ALConstants.logInfo("Config file found");
			try {
				SparkConfig.read(configFile, AsyncLocatorConfigFabric.class);

				// in case of manual edits
				if (LOCATOR_THREADS > MAX_THREADS || LOCATOR_THREADS < 1) {
					ALConstants.logError(
							"Invalid locatorThreads value ({}). Must be between 1-64. Resetting to default ({}).",
							LOCATOR_THREADS, DEFAULT_THREADS
					);
					LOCATOR_THREADS = DEFAULT_THREADS;

					try {
						SparkConfig.write(configFile, AsyncLocatorConfigFabric.class);
						ALConstants.logInfo("Config file rewritten with default threads value");
					} catch (IOException | IllegalAccessException writeError) {
						ALConstants.logError(writeError, "Failed to rewrite config file");
					}
				}

			} catch (IllegalStateException e) {
				// SparkConfig throws this when value is out of range
				if (e.getMessage().contains("greater than the maximum") ||
						e.getMessage().contains("less than the minimum")) {
					ALConstants.logError(
							"Invalid config value detected: {}. Resetting to defaults and recreating config.",
							e.getMessage()
					);

					LOCATOR_THREADS = DEFAULT_THREADS;


					// Rewrite config with defaults
					try {
						SparkConfig.write(configFile, AsyncLocatorConfigFabric.class);
						ALConstants.logInfo("Config file rewrite with default values");
					} catch (IOException | IllegalAccessException writeError) {
						ALConstants.logError(writeError, "Failed to rewrite cpnfig file");
					}
				} else {
					ALConstants.logError(e, "Failed to read config file, using defaults");
					resetToDefaults();
				}
			} catch (IOException | IllegalAccessException e) {
				ALConstants.logError(e, "Failed to read config file {}, using defaults", configFile);
				resetToDefaults();
			}
		} else {
			ALConstants.logInfo("No config file found - creating it");
			try {
				SparkConfig.write(configFile, AsyncLocatorConfigFabric.class);
			} catch (IOException | IllegalAccessException e) {
				ALConstants.logError(e, "Failed to write config file {}", configFile);
			}
		}
	}
}
