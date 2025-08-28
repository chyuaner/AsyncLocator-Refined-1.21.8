package brightspark.asynclocator;

import brightspark.asynclocator.platform.Services;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.text.NumberFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AsyncLocator {
	private static volatile ExecutorService LOCATING_EXECUTOR_SERVICE = null;
	private static final AtomicInteger POOL_COUNTER = new AtomicInteger(1);

	private AsyncLocator() {}

	// Initializes the singleton executor for locating tasks
	public static void setupExecutorService() {
		synchronized (AsyncLocator.class) {
			shutdownExecutorService();

			int threads = Services.CONFIG.locatorThreads();
			if (threads <= 0) {
				ALConstants.logWarn("Configured locatorThreads <= 0 ({}). Falling back to 1 thread", threads);
				threads = 1;
			}
			ALConstants.logInfo("Starting locating executor service with thread pool size of {}", threads);

			final String namePrefix = ALConstants.MOD_ID + "-" + POOL_COUNTER.getAndIncrement() + "-thread-";
			final AtomicInteger threadNum = new AtomicInteger(1);

			LOCATING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
				threads,
				r -> {
					Thread t = new Thread(r, namePrefix + threadNum.getAndIncrement());
					t.setDaemon(true);
					t.setUncaughtExceptionHandler((th, e) -> ALConstants.logError(e, "Uncaught exception in locating thread {}", th.getName()));
					return t;
				}
			);
		}
	}

	public static void shutdownExecutorService() {
		synchronized (AsyncLocator.class) {
			if (LOCATING_EXECUTOR_SERVICE != null) {
				ALConstants.logInfo("Shutting down locating executor service");
				LOCATING_EXECUTOR_SERVICE.shutdown();
				try {
					if (!LOCATING_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS)) { // i think 5 seconds is better
						LOCATING_EXECUTOR_SERVICE.shutdownNow();
					}
				} catch (InterruptedException ie) {
					LOCATING_EXECUTOR_SERVICE.shutdownNow();
					Thread.currentThread().interrupt();
				}
				LOCATING_EXECUTOR_SERVICE = null;
			}
		}
	}

	public static boolean isExecutorActive() {
		ExecutorService es = LOCATING_EXECUTOR_SERVICE;
		return es != null && !es.isShutdown() && !es.isTerminated();
	}

	/**
	 * Queues a task to locate a feature using {@link ServerLevel#findNearestMapStructure(TagKey, BlockPos, int, boolean)}
	 * and returns a {@link LocateTask} with the futures for it.
	 */
	public static LocateTask<BlockPos> locate(
		ServerLevel level,
		TagKey<Structure> structureTag,
		BlockPos pos,
		int searchRadius,
		boolean skipKnownStructures
	) {
		ALConstants.logDebug(
			"Creating locate task for {} in {} around {} within {} chunks",
			structureTag, level, pos, searchRadius
		);

		if (!isExecutorActive()) {
			ALConstants.logWarn("Locating executor service not initialized or not active: creating lazily");
			setupExecutorService();
		}

		CompletableFuture<BlockPos> completableFuture = new CompletableFuture<>();
		Future<?> future = LOCATING_EXECUTOR_SERVICE.submit(
			() -> doLocateLevel(completableFuture, level, structureTag, pos, searchRadius, skipKnownStructures)
		);
		return new LocateTask<>(level.getServer(), completableFuture, future);
	}

	/**
	 * Queues a task to locate a feature using
	 * {@link ChunkGenerator#findNearestMapStructure(ServerLevel, HolderSet, BlockPos, int, boolean)} and returns a
	 * {@link LocateTask} with the futures for it.
	 */
	public static LocateTask<Pair<BlockPos, Holder<Structure>>> locate(
		ServerLevel level,
		HolderSet<Structure> structureSet,
		BlockPos pos,
		int searchRadius,
		boolean skipKnownStructures
	) {
		ALConstants.logDebug(
			"Creating locate task for {} in {} around {} within {} chunks",
			structureSet, level, pos, searchRadius
		);

		if (!isExecutorActive()) {
			ALConstants.logWarn("Locating executor service not initialized or not active: creating lazily");
			setupExecutorService();
		}

		CompletableFuture<Pair<BlockPos, Holder<Structure>>> completableFuture = new CompletableFuture<>();
		Future<?> future = LOCATING_EXECUTOR_SERVICE.submit(
			() -> doLocateChunkGenerator(completableFuture, level, structureSet, pos, searchRadius, skipKnownStructures)
		);
		return new LocateTask<>(level.getServer(), completableFuture, future);
	}

	private static void doLocateLevel(
		CompletableFuture<BlockPos> completableFuture,
		ServerLevel level,
		TagKey<Structure> structureTag,
		BlockPos pos,
		int searchRadius,
		boolean skipExistingChunks
	) {
		try {
			ALConstants.logDebug(
				"Trying to locate {} in {} around {} within {} chunks",
				structureTag, level, pos, searchRadius
			);
			long start = System.nanoTime();
			BlockPos foundPos = level.findNearestMapStructure(structureTag, pos, searchRadius, skipExistingChunks);
			String time = NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
			if (foundPos == null)
				ALConstants.logInfo("No {} found (took {}ms)", structureTag, time);
			else
				ALConstants.logInfo("Found {} at {} (took {}ms)", structureTag, foundPos, time);
			completableFuture.complete(foundPos);
		} catch (Throwable t) {
			ALConstants.logError(t, "Exception while locating {} around {}", structureTag, pos);
			try {
				completableFuture.complete(null);
			} catch (Throwable ignore) {
			}
		}
	}

	private static void doLocateChunkGenerator(
		CompletableFuture<Pair<BlockPos, Holder<Structure>>> completableFuture,
		ServerLevel level,
		HolderSet<Structure> structureSet,
		BlockPos pos,
		int searchRadius,
		boolean skipExistingChunks
	) {
		try {
			ALConstants.logDebug(
				"Trying to locate {} in {} around {} within {} chunks",
				structureSet, level, pos, searchRadius
			);
			long start = System.nanoTime();
			Pair<BlockPos, Holder<Structure>> foundPair = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, structureSet, pos, searchRadius, skipExistingChunks);
			String time = NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
			if (foundPair == null)
				ALConstants.logInfo("No {} found (took {}ms)", structureSet, time);
			else
				ALConstants.logInfo("Found {} at {} (took {}ms)",
					foundPair.getSecond().value().getClass().getSimpleName(), foundPair.getFirst(), time
				);
			completableFuture.complete(foundPair);
		} catch (Throwable t) {
			ALConstants.logError(t, "Exception while locating {} around {}", structureSet, pos);
			try {
				completableFuture.complete(null);
			} catch (Throwable ignore) {
			}
		}
	}

	/**
	 * Holder of the futures for an async locate task as well as providing some helper functions.
	 * The completableFuture will be completed once the call to
	 * {@link ServerLevel#findNearestMapStructure(TagKey, BlockPos, int, boolean)} has completed, and will hold the
	 * result of it.
	 * The taskFuture is the future for the {@link Runnable} itself in the executor service.
	 */
	public record LocateTask<T>(MinecraftServer server, CompletableFuture<T> completableFuture, Future<?> taskFuture) {
		/**
		 * Helper function that calls {@link CompletableFuture#thenAccept(Consumer)} with the given action.
		 * Bear in mind that the action will be executed from the task's thread. If you intend to change any game data,
		 * it's strongly advised you use {@link #thenOnServerThread(Consumer)} instead so that it's queued and executed
		 * on the main server thread instead.
		 */
		public LocateTask<T> then(Consumer<T> action) {
			completableFuture.thenAccept(action);
			return this;
		}

		/**
		 * Helper function that calls {@link CompletableFuture#thenAccept(Consumer)} with the given action on the server
		 * thread.
		 */
		public LocateTask<T> thenOnServerThread(Consumer<T> action) {
			completableFuture.thenAccept(pos -> server.submit(() -> action.accept(pos)));
			return this;
		}

		/**
		 * Helper function that cancels both completableFuture and taskFuture.
		 */
		public void cancel() {
			taskFuture.cancel(true);
			completableFuture.cancel(false);
		}
	}
}
