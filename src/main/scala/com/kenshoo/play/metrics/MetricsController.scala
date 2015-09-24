/*
* Copyright 2013 Kenshoo.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.kenshoo.play.metrics

import java.io.StringWriter
import javax.inject.Inject

import play.api.{Application, Play}
import play.api.mvc.{Action, Controller}

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectWriter, ObjectMapper}


trait MetricsController {
  self: Controller =>

  def registry: MetricRegistry

  def metricsHolderOption: Option[MetricsHolderInterface]

  def serialize(mapper: ObjectMapper) = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, registry)
    Ok(stringWriter.toString).as("application/json").withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
  }

  def metrics = Action {
    metricsHolderOption match {
      case Some(holder) =>
        if (holder.enabled)
          serialize(holder.mapper)
        else
          InternalServerError("metrics plugin not enabled")
      case None => InternalServerError("metrics plugin is not found")
    }
  }

}

object MetricsController extends Controller with MetricsController {
  def registry = MetricsRegistry.defaultRegistry
  def metricsHolderOption = Play.current.plugin[MetricsPlugin]
}

class MetricsController2 @Inject() (metricsRegistry: MetricsRegistryInterface, metricsHolder: MetricsHolderInterface) extends Controller with MetricsController {
  def registry = metricsRegistry.defaultRegistry
  def metricsHolderOption: Option[MetricsHolderInterface] = Some(metricsHolder)

  override def metrics = Action {
    metricsHolderOption match {
      case Some(holder) =>
        if (holder.enabled)
          serialize(holder.mapper)
        else
          InternalServerError("metrics plugin not enabled")
      case None => InternalServerError("metrics plugin is not found")
    }
  }
}
