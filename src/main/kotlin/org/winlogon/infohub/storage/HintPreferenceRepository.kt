package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * An interface for storing and retrieving player hint preferences.
 */
interface HintPreferenceRepository {
    /**
     * Initializes the repository.
     */
    fun init()

    /**
     * Retrieves the hint preference for a player.
     *
     * @param playerUuid The UUID of the player.
     * @return A [CompletableFuture] that will complete with the player's preference.
     */
    fun getHintPreference(playerUuid: UUID): CompletableFuture<TriState>

    /**
     * Sets the hint preference for a player.
     *
     * @param playerUuid The UUID of the player.
     * @param choice The player's preference.
     */
    fun setHintPreference(playerUuid: UUID, choice: TriState)

    /**
     * Closes the repository and releases any resources.
     */
    fun close()
}
