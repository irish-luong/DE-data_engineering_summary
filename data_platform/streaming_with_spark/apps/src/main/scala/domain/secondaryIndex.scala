package domain


class SecondaryIndexTest extends FunSuite with Matchers {

  private def footballersTable(indexStrategy: SecondaryIndexStorageStrategy): BaseTable = {
    val baseTable = new BaseTable("name", Seq("age", "position", "team"))
    baseTable.createSecondaryIndex("position", indexStrategy, Seq("name"))
    baseTable.writeValue(("name", "Tony Adams"), ("age", "36"), ("position", "defender"), ("team", "Arsenal"))
    baseTable.writeValue(("name", "Martin Keown"), ("age", "36"), ("position", "defender"), ("team", "Arsenal"))
    baseTable.writeValue(("name", "Ryan Giggs"), ("age", "29"), ("position", "midfielder"), ("team", "Manchester United"))
    baseTable
  }

  test("should read entry from index with fetching strategy") {
    val baseTable = footballersTable(FetchSecondaryIndexStorageStrategy)

    val defenders = baseTable.readFromSecondaryIndex("position", "defender")

    val names = defenders.map(row => row.attributes("name"))
    names should have size 2
    names should contain allOf("Tony Adams", "Martin Keown")
    val ages = defenders.map(row => row.attributes("age"))
    ages should have size 2
    ages(0) shouldEqual "36"
    ages(1) shouldEqual "36"
  }

  test("should read entry from index with copy strategy") {
    val baseTable = footballersTable(CopySecondaryIndexStorageStrategy)

    val defenders = baseTable.readFromSecondaryIndex("position", "defender")

    val names = defenders.map(row => row.attributes("name"))
    names should have size 2
    names should contain allOf("Tony Adams", "Martin Keown")
  }

  test("should create secondary index after adding some values") {
    val baseTable = new BaseTable("name", Seq("age", "position", "team"))
    baseTable.writeValue(("name", "Jerzy Dudek"), ("age", "29"), ("position", "goalkeeper"), ("team", "Liverpool FC"))
    baseTable.createSecondaryIndex("position", CopySecondaryIndexStorageStrategy, Seq("name"))

    val goalkeepers = baseTable.readFromSecondaryIndex("position", "goalkeeper")

    val names = goalkeepers.map(row => row.attributes("name"))
    names should have size 1
    names(0) shouldEqual "Jerzy Dudek"
  }

}


class BaseTable(val partitionKey: String, attributes: Seq[String]) {

  private val secondaryIndexes: mutable.Map[String, SecondaryIndex] = new mutable.HashMap[String, SecondaryIndex]()

  private val data = new mutable.HashMap[String, Row]()

  def createSecondaryIndex(indexColumn: String, indexStrategy: SecondaryIndexStorageStrategy, attributes: Seq[String]) = {
    val secondaryIndex = new SecondaryIndex(indexColumn, indexStrategy, attributes, this)
    secondaryIndexes.put(indexColumn, secondaryIndex)

    if (data.nonEmpty) {
      data.foreach(entry => writeValueToSecondaryIndex(secondaryIndex, entry._2))
    }
  }

  def writeValue(keyValuePairs: (String, String)*) = {
    val attributes = Map(keyValuePairs.map(pair => pair._1 -> pair._2): _*)
    assert(attributes.contains(partitionKey), "Invalid entry - it doesn't contain partition key")

    val row = new Row(attributes)
    data.put(attributes(partitionKey), row)
    secondaryIndexes.foreach(entry => writeValueToSecondaryIndex(entry._2, row))
  }

  def readFromSecondaryIndex(key: String, value: String): Seq[Row] = {
    secondaryIndexes(key).readValue(value)
  }

  private def writeValueToSecondaryIndex(secondaryIndex: SecondaryIndex, row: Row): Unit = {
    secondaryIndex.writeValue(row)
  }

  def readBatch(keys: Seq[String]): Seq[Row] = {
    println(s"Fetching rows for keys ${keys}")
    val dataToReturn = new mutable.ListBuffer[Row]()
    keys.foreach(key => {
      val dataValue = data.get(key)
      if (dataValue.isDefined) {
        dataToReturn.append(dataValue.get)
      }
    })
    dataToReturn
  }

}

class SecondaryIndex(indexKey: String, indexStrategy: SecondaryIndexStorageStrategy, val attributes: Seq[String],
                     val baseTable: BaseTable) {

  private val inverseIndex = new mutable.HashMap[String, Seq[Row]]()

  def readValue(partitionKey: String): Seq[Row] = {
    val rows = inverseIndex(partitionKey)
    indexStrategy.read(this, rows)
  }

  def writeValue(row: Row) = {
    val inverseIndexKey = row.attributes(indexKey)
    val currentValues = inverseIndex.getOrElse(inverseIndexKey, Seq.empty)
    val newValues = currentValues :+ indexStrategy.mutateToWritableRow(this, row)
    inverseIndex.put(inverseIndexKey, newValues)
  }

}

// For the simplicity reason [[keyValuePairs]] accepts only String values
class Row(val attributes: Map[String, String]) {

  override def toString: String = s"Row ${attributes}"

}

trait SecondaryIndexStorageStrategy {
  def mutateToWritableRow(secondaryIndex: SecondaryIndex, row: Row): Row = {
    val attributesToSave = row.attributes.filter(entry => secondaryIndex.attributes.contains(entry._1))
    new Row(attributesToSave)
  }

  def read(secondaryIndex: SecondaryIndex, rows: Seq[Row]): Seq[Row]
}

object FetchSecondaryIndexStorageStrategy extends SecondaryIndexStorageStrategy {
  override def read(secondaryIndex: SecondaryIndex, rows: Seq[Row]): Seq[Row] = {
    val keys = rows.map(row => row.attributes.head._2)
    secondaryIndex.baseTable.readBatch(keys)
  }
}
object CopySecondaryIndexStorageStrategy extends SecondaryIndexStorageStrategy {
  override def read(secondaryIndex: SecondaryIndex, rows: Seq[Row]): Seq[Row] = rows
}

