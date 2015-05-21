package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
object Transformer {

  def transformMap[O, N](transform: O => N)(oldMap: java.util.Map[String, O]): Map[String, N] = {
    if (oldMap == null) Map.empty
    else {
      oldMap.asScala.foldLeft[Map[String, N]](Map.empty) { (aggr, el) =>
        val (key, value) = el
        aggr + (key -> transform(value))
      }
    }
  }

}