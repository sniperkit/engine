package tech.sourced.api

import org.scalatest._

class DefaultSourceSpec extends FlatSpec with Matchers with BaseSivaSpec with BaseSparkSpec {
  "Default source" should "load correctly" in {
    val reposDf = ss.read.format("tech.sourced.api")
      .option("table", "repositories")
      .load(resourcePath)

    reposDf.filter("is_fork=true or is_fork is null").show()

    reposDf.filter("array_contains(urls, 'urlA')").show()

    val referencesDf = ss.read.format("tech.sourced.api")
      .option("table", "references")
      .load(resourcePath)

    referencesDf.filter("repository_id = 'ID1'").show()

    val commitsDf = ss.read.format("tech.sourced.api")
      .option("table", "commits")
      .load(resourcePath)

    commitsDf.show()

    println("Files/blobs (without commit hash filtered) at HEAD or every ref:\n")
    val filesDf = ss.read.format("tech.sourced.api")
      .option("table", "files")
      .load(resourcePath)

    filesDf.explain(true)
    filesDf.show()

    assert(filesDf.count() != 0)
  }

  "Additional methods" should "work correctly" in {
    val spark = ss

    spark.sqlContext.setConf("tech.sourced.api.repositories.path", resourcePath)

    import Implicits._
    import spark.implicits._

    val reposDf = spark.getRepositories
      .filter($"id" === "github.com/mawag/faq-xiyoulinux" || $"id" === "github.com/xiyou-linuxer/faq-xiyoulinux")
    val refsDf = reposDf.getReferences.filter($"name".equalTo("refs/heads/HEAD"))

    val commitsDf = refsDf.getCommits.select("repository_id", "reference_name", "message", "hash")
    //commitsDf.show()

    println("Files/blobs with commit hashes:\n")
    val filesDf = refsDf.getCommits.getFiles.select("repository_id", "reference_name", "path", "commit_hash", "file_hash")
    filesDf.explain(true)
    filesDf.show()

    val cnt = filesDf.count()
    println(s"Total $cnt rows")
    assert(cnt != 0)
  }


}