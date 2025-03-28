package org.winlogon.infohub.utils

import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.software.os.OperatingSystem
import org.bukkit.command.CommandSender
import java.util.concurrent.TimeUnit

data class SystemSpecs(
    val operatingSystem: String,
    val processor: String,
    val physicalCores: Int,
    val logicalCores: Int,
    val totalMemory: Int,
    val availableMemory: Int
)

object ServerStats {
    /**
     * Get the specs of the server, such as processor, number of cores, and memory
     *
     * @return SystemSpecs the specs of the server
     */
    fun getSystemSpecs(): SystemSpecs {
        val systemInfo = SystemInfo()
        val hardware = systemInfo.hardware
        val processor: CentralProcessor = hardware.processor
        val memory: GlobalMemory = hardware.memory

        return SystemSpecs(
            getOperatingSystem(systemInfo),
            processor.processorIdentifier.name,
            processor.physicalProcessorCount,
            processor.logicalProcessorCount,
            (memory.total / 1e9).toInt(),
            (memory.available / 1e9).toInt()
        )
    }

    private fun getOperatingSystem(systemInfo: SystemInfo): String {
        val os: OperatingSystem = systemInfo.operatingSystem
        val osName = os.family
        val version = os.versionInfo.version
        return "<dark_aqua>$osName<gray> version <dark_aqua>$version<gray>"
    }

    /**
     * Get the uptime of the server based on the time that passed from onEnable
     *
     * @param start The start time of the server, from when this plugin is enabled
     * @param end The moment the command is run
     */
    fun getUptime(start: Long, end: Long): String {
        val nanos = end - start
        val days = TimeUnit.NANOSECONDS.toDays(nanos)
        val hours = TimeUnit.NANOSECONDS.toHours(nanos) % 24
        val minutes = TimeUnit.NANOSECONDS.toMinutes(nanos) % 60
        val seconds = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60

        return "Server has been up for: <dark_aqua>${days}d ${hours}h ${minutes}m ${seconds}s"
    }
}
