package org.apache.s2graph.core.storage.hbase

import org.apache.s2graph.core.storage.serde.Deserializable
import org.apache.s2graph.core.{IndexEdge, S2Graph, S2Vertex, SnapshotEdge}
import org.apache.s2graph.core.storage.{StorageIO, StorageSerDe, serde}

class AsynchbaseStorageSerDe(val graph: S2Graph) extends StorageSerDe {
  import org.apache.s2graph.core.types.HBaseType._

  /**
    * create serializer that knows how to convert given snapshotEdge into kvs: Seq[SKeyValue]
    * so we can store this kvs.
    *
    * @param snapshotEdge : snapshotEdge to serialize
    * @return serializer implementation for StorageSerializable which has toKeyValues return Seq[SKeyValue]
    */
  override def snapshotEdgeSerializer(snapshotEdge: SnapshotEdge) = {
    snapshotEdge.schemaVer match {
      //      case VERSION1 |
      case VERSION2 => new serde.snapshotedge.wide.SnapshotEdgeSerializable(snapshotEdge)
      case VERSION3 | VERSION4 => new serde.snapshotedge.tall.SnapshotEdgeSerializable(snapshotEdge)
      case _ => throw new RuntimeException(s"not supported version: ${snapshotEdge.schemaVer}")
    }
  }

  /**
    * create serializer that knows how to convert given indexEdge into kvs: Seq[SKeyValue]
    *
    * @param indexEdge : indexEdge to serialize
    * @return serializer implementation
    */
  override def indexEdgeSerializer(indexEdge: IndexEdge) = {
    indexEdge.schemaVer match {
      //      case VERSION1
      case VERSION2 | VERSION3 => new serde.indexedge.wide.IndexEdgeSerializable(indexEdge)
      case VERSION4 => new serde.indexedge.tall.IndexEdgeSerializable(indexEdge)
      case _ => throw new RuntimeException(s"not supported version: ${indexEdge.schemaVer}")
    }
  }

  /**
    * create serializer that knows how to convert given vertex into kvs: Seq[SKeyValue]
    *
    * @param vertex : vertex to serialize
    * @return serializer implementation
    */
  override def vertexSerializer(vertex: S2Vertex) = new serde.vertex.wide.VertexSerializable(vertex)

  /**
    * create deserializer that can parse stored CanSKeyValue into snapshotEdge.
    * note that each storage implementation should implement implicit type class
    * to convert storage dependent dataType into common SKeyValue type by implementing CanSKeyValue
    *
    * ex) Asynchbase use it's KeyValue class and CanSKeyValue object has implicit type conversion method.
    * if any storaage use different class to represent stored byte array,
    * then that storage implementation is responsible to provide implicit type conversion method on CanSKeyValue.
    **/
  private val snapshotEdgeDeserializable = new serde.snapshotedge.tall.SnapshotEdgeDeserializable(graph)
  override def snapshotEdgeDeserializer(schemaVer: String) = snapshotEdgeDeserializable

  /** create deserializer that can parse stored CanSKeyValue into indexEdge. */
  private val indexEdgeDeserializer = new serde.indexedge.tall.IndexEdgeDeserializable(graph)
  override def indexEdgeDeserializer(schemaVer: String) = indexEdgeDeserializer

  /** create deserializer that can parser stored CanSKeyValue into vertex. */
  private val vertexDeserializer = new serde.vertex.wide.VertexDeserializable(graph)
  override def vertexDeserializer(schemaVer: String): Deserializable[S2Vertex] = vertexDeserializer
}
