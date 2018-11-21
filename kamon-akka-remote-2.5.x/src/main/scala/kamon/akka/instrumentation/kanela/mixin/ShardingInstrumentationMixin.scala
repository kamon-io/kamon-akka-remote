package kamon.akka.instrumentation.kanela.mixin

import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.ExtractEntityId
import akka.kamon.instrumentation.cluster.ShardedType

class InjectedShardedTypeMixin extends ShardedType {
  var _typeName: String = _
  var _idExtractor: ShardRegion.ExtractEntityId = _


  override def setTypeName(typeName: String): Unit = this._typeName = typeName
  override def setIdExtractor(extractor: ShardRegion.ExtractEntityId): Unit =
    this._idExtractor = extractor

  override def typeName: String = this._typeName

  override def idExtractor: ExtractEntityId = this._idExtractor
}