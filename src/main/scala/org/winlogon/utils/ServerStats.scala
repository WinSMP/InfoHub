package org.winlogon.utils

import oshi.SystemInfo
import oshi.hardware.{CentralProcessor, GlobalMemory}
import oshi.software.os.OperatingSystem

import org.bukkit.command.CommandSender

import scala.collection.JavaConverters.*
import scala.concurrent.duration.*

case class SystemSpecs(
  operatingSystem: String,
  processor: String,
  physicalCores: Int,
  logicalCores: Int,
  totalMemory: Int,
  availableMemory: Int,
)

object ServerStats {
  /**
    * Get the specs of the server, such as processor, number of cores, and memory
    *
    * @return SystemSpecs the specs of the server
    */
  def getSystemSpecs(): SystemSpecs = {
    val systemInfo = SystemInfo()
    val hardware = systemInfo.getHardware
    val processor: CentralProcessor = hardware.getProcessor
    val memory: GlobalMemory = hardware.getMemory
  
    SystemSpecs(
      getOperatingSystem(systemInfo),
      processor.getProcessorIdentifier.getName,
      processor.getPhysicalProcessorCount,
      processor.getLogicalProcessorCount,
      (memory.getTotal / 1e9).toInt,
      (memory.getAvailable / 1e9).toInt,
    )
  }

  private def getOperatingSystem(ss: SystemInfo): String = {
    val os: OperatingSystem = ss.getOperatingSystem
    val osVersionInfo = os.getVersionInfo
    val osName = os.getFamily
    val version = osVersionInfo.getVersion
    s"§3$osName§7 version §2$version§7"
  }

  /**
    * Get the uptime of the server based on the time that passed from onEnable
    *
    * @param start The start time of the server, from when this plugin is enabled
    * @param end The moment the command is run
    * @param sender The sender of the command
    */
  def getUptime(start: Long, end: Long, sender: CommandSender): String = {
    val nanos = end - start
    val duration = Duration(nanos, NANOSECONDS)
  
    val days = duration.toDays
    val hours = duration.toHours % 24
    val minutes = duration.toMinutes % 60
    val seconds = duration.toSeconds % 60
  
    val result = s"§7Server has been up for: §3${days}d ${hours}h ${minutes}m ${seconds}s"
    sender.sendMessage(result)
    result
  }

}
