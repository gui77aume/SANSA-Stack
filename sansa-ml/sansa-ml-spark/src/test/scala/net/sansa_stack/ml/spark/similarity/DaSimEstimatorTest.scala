package net.sansa_stack.ml.spark.similarity

import com.holdenkarau.spark.testing.SharedSparkContext
import com.holdenkarau.spark.testing.DataFrameSuiteBase
import net.sansa_stack.ml.spark.similarity.similarityEstimationModels._
import net.sansa_stack.ml.spark.utils.{FeatureExtractorModel, SimilarityExperimentMetaGraphFactory}
import net.sansa_stack.rdf.spark.io._
import net.sansa_stack.rdf.spark.model.TripleOperations
import org.apache.jena.graph
import org.apache.jena.graph.Triple
import org.apache.jena.riot.Lang
import org.apache.jena.sys.JenaSystem
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite

class DaSimEstimatorTest extends FunSuite with SharedSparkContext {

  System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  System.setProperty("spark.kryo.registrator", "net.sansa_stack.rdf.spark.io.JenaKryoRegistrator")

  lazy val spark = SparkSession.builder()
    .appName(s"SimE4KG Unit Test")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // we need Kryo serialization enabled with some custom serializers
    .config("spark.kryo.registrator", String.join(
      ", ",
      "net.sansa_stack.rdf.spark.io.JenaKryoRegistrator",
      "net.sansa_stack.query.spark.sparqlify.KryoRegistratorSparqlify"))
    .config("spark.sql.crossJoin.enabled", true)
    .getOrCreate()

  private val dataPath = this.getClass.getClassLoader.getResource("similarity/sampleMovieDB.nt").getPath
  private def getData() = {
    import net.sansa_stack.rdf.spark.io._
    import net.sansa_stack.rdf.spark.model._

    val df: DataFrame = spark.read.rdf(Lang.TURTLE)(dataPath).cache()
    val dataset = df.toDS()
    dataset
  }

  // define inputpath if it is not parameter
  // private val inputPath = this.getClass.getClassLoader.getResource("similarity/sampleMovieDB.nt").getPath

  // var triplesDf: DataFrame = spark.read.rdf(Lang.NTRIPLES)(inputPath).cache()

  // for value comparison we want to allow some minor differences in number comparison
  val epsilon = 1e-4f

  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)

  override def beforeAll(): Unit = {
    super.beforeAll()
    JenaSystem.init()
    spark.sparkContext.setLogLevel("ERROR")
  }

  test("Test DaSimEstimator Modules") {

    /* val lang = Lang.TURTLE
    val originalDataRDD = spark.rdf(lang)("similarity/sampleMovieDB.nt").persist()

    val dataset: Dataset[Triple] = originalDataRDD
      .toDS()
      .cache() */

    val dataset: Dataset[graph.Triple] = getData()

    val dse = new DaSimEstimator()
      // .setSparqlFilter("SELECT ?o WHERE { ?s <https://sansa.sample-stack.net/genre> ?o }")
      .setObjectFilter("http://data.linkedmdb.org/movie/film")
      .setDistSimFeatureExtractionMethod("os")
      .setSimilarityValueStreching(false)
      .setImportance(Map("initial_release_date_sim" -> 0.2, "rdf-schema#label_sim" -> 0.0, "runtime_sim" -> 0.2, "writer_sim" -> 0.1, "22-rdf-syntax-ns#type_sim" -> 0.0, "actor_sim" -> 0.3, "genre_sim" -> 0.2))

    val resultSimDf = dse
      .transform(dataset)

    // resultSimDf.show(false)

    val metagraph: RDD[Triple] = dse.semantification(resultSimDf)
      /* entityCols = resultSimDf.columns.slice(0, 2),
      finalValCol = "overall_similarity_score",
      similarityCols = resultSimDf.columns.slice(2, resultSimDf.columns.length - 1),
      availability = dse.pAvailability,
      reliability = dse.pReliability,
      importance = dse.pImportance,
      distSimFeatureExtractionMethod = dse._pDistSimFeatureExtractionMethod,
      initialFilter = if (dse._pInitialFilterByObject != null) dse._pInitialFilterByObject else dse._pInitialFilterBySPARQL,
      featureExtractionMethod = if (dse.pSparqlFeatureExtractionQuery != null) dse.pSparqlFeatureExtractionQuery else "SmartFeatureExtractor"


    ) */
    metagraph.foreach(println(_))
  }
}