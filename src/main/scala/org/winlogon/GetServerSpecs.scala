package org.winlogon

import oshi.SystemInfo
import oshi.hardware.{CentralProcessor, GlobalMemory}

import scala.collection.JavaConverters._

case class SystemSpecs(
  processor: String,
  physicalCores: Int,
  logicalCores: Int,
  totalMemory: Int,
  availableMemory: Int,
)

/**
  * Get the specs of the server, such as processor, number of cores, and memory
  *
  * @return SystemSpecs the specs of the server
  */
def getSystemSpecs(): SystemSpecs = {
  val systemInfo = new SystemInfo()
  val hardware = systemInfo.getHardware
  val processor: CentralProcessor = hardware.getProcessor
  val memory: GlobalMemory = hardware.getMemory

  SystemSpecs(
    processor.getProcessorIdentifier.getName,
    processor.getPhysicalProcessorCount,
    processor.getLogicalProcessorCount,
    (memory.getTotal / 1e9).toInt,
    (memory.getAvailable / 1e9).toInt,
  )
}
