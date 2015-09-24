package com.kenshoo.play.metrics

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, MemoryUsageGaugeSet, GarbageCollectorMetricSet}
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{SharedMetricRegistries, JvmAttributeGaugeSet, MetricRegistry}
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.{Environment, Configuration, Logger}
import play.api.inject.{Module, ApplicationLifecycle}

import scala.concurrent.Future

@Singleton
class PlayModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[MetricsHolderInterface].to[MetricsHolder].eagerly,
      bind[MetricsRegistryInterface].to[MetricsRegistry2].eagerly
    )
  }
}

trait MetricsRegistryInterface {

  def defaultRegistry: MetricRegistry
}

trait MetricsHolderInterface {

  def enabled: Boolean

  def registryName: String

  val mapper: ObjectMapper
}

@Singleton
class MetricsRegistry2 @Inject() (metricsHolder: MetricsHolderInterface) extends MetricsRegistryInterface {

  def defaultRegistry = SharedMetricRegistries.getOrCreate(metricsHolder.registryName)
}

@Singleton
class MetricsHolder @Inject() (lifecycle: ApplicationLifecycle, configuration: Configuration) extends MetricsHolderInterface {

  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val mapper: ObjectMapper = new ObjectMapper()

  def registryName = configuration.getString("metrics.name").getOrElse("default")

  implicit def stringToTimeUnit(s: String) : TimeUnit = TimeUnit.valueOf(s)

  def onStart() {
    def setupJvmMetrics(registry: MetricRegistry) {
      val jvmMetricsEnabled = configuration.getBoolean("metrics.jvm").getOrElse(true)
      if (jvmMetricsEnabled) {
        registry.register("jvm.attribute", new JvmAttributeGaugeSet())
        registry.register("jvm.gc", new GarbageCollectorMetricSet())
        registry.register("jvm.memory", new MemoryUsageGaugeSet())
        registry.register("jvm.threads", new ThreadStatesGaugeSet())
      }
    }

    def setupLogbackMetrics(registry: MetricRegistry) = {
      val logbackEnabled = configuration.getBoolean("metrics.logback").getOrElse(true)
      if (logbackEnabled) {
        val appender: InstrumentedAppender = new InstrumentedAppender(registry)

        val logger: classic.Logger = Logger.logger.asInstanceOf[classic.Logger]
        appender.setContext(logger.getLoggerContext)
        appender.start()
        logger.addAppender(appender)
      }
    }

    if (enabled) {
      val registry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)
      val rateUnit     = configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")
      val durationUnit = configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")
      val showSamples  = configuration.getBoolean("metrics.showSamples").getOrElse(false)

      setupJvmMetrics(registry)
      setupLogbackMetrics(registry)

      val module = new MetricsModule(rateUnit, durationUnit, showSamples)
      mapper.registerModule(module)
    }
  }

  def onStop() {
    if (enabled) {
      SharedMetricRegistries.remove(registryName)
    }
  }

  def enabled = configuration.getBoolean("metrics.enabled").getOrElse(true)

  onStart()
  lifecycle.addStopHook(() => Future.successful{ onStop() })
}
