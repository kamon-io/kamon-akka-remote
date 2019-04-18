package akka.kamon.instrumentation.cluster

import akka.cluster.sharding.ShardRegion

trait ShardedType {
  def typeName: String
  def setTypeName(typeName: String)

  def idExtractor: ShardRegion.ExtractEntityId
  def setIdExtractor(extractor: ShardRegion.ExtractEntityId): Unit
}

class InjectedShardedType extends ShardedType {
  var typeName: String = _
  var idExtractor: ShardRegion.ExtractEntityId = _

  override def setTypeName(identity: String): Unit = this.typeName = identity
  override def setIdExtractor(extractor: ShardRegion.ExtractEntityId): Unit =
    this.idExtractor = extractor
}