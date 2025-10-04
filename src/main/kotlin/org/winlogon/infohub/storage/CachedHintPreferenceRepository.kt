package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState
import org.winlogon.infohub.cache.PlayerCache
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * A [HintPreferenceRepository] that uses a [PlayerCache] to cache preferences.
 */
class CachedHintPreferenceRepository(
    private val persistentRepository: HintPreferenceRepository,
    private val cache: PlayerCache<TriState>
) : HintPreferenceRepository {

    override fun init() {
        persistentRepository.init()
    }

    override fun getHintPreference(playerUuid: UUID): CompletableFuture<TriState> {
        return cache.get(playerUuid).thenCompose { cachedValue ->
            if (cachedValue != null) {
                CompletableFuture.completedFuture(cachedValue)
            } else {
                persistentRepository.getHintPreference(playerUuid).thenApply { persistentValue ->
                    cache.put(playerUuid, persistentValue)
                    persistentValue
                }
            }
        }
    }

    override fun setHintPreference(playerUuid: UUID, choice: TriState) {
        cache.put(playerUuid, choice)
        persistentRepository.setHintPreference(playerUuid, choice)
    }

    override fun close() {
        persistentRepository.close()
        cache.close()
    }
}
