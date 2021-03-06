package edu.umd.mith.woodchipper.util

import scala.io.Source
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import net.liftweb.json._

import bootstrap.liftweb.Boot

import edu.umd.mith.woodchipper.model._

/*class JsonImporter(path: String) {
  private val files = (new File(path)).listFiles.filter(_.getName.endsWith(".json"))

  def importAll {
    this.files.toIterator.map((file: File) => new BufferedReader(new FileReader(file))).foreach { reader =>
      val doc = JsonParser.parse(reader)
      val metadata = doc \ "metadata"
      val JField(_, JString(collection)) = metadata \ "collection"
      val JField(_, JString(textId)) = metadata \ "textid"
      val JField(_, JString(title)) = metadata \ "title"
      val JField(_, JString(author)) = metadata \ "author"
      val JField(_, JInt(year)) = metadata \ "date"

      val JField(_, JArray(chunks)) = doc \ "chunks"

      val text = Text.add(collection, textId, title, author, year.toInt)

      chunks.map { chunk =>
        val JField(_, JString(chunkId)) = chunk \ "metadata" \ "chunkid"
        val JField(_, JString(plain)) = chunk \ "representations" \ "plain"
        val JField(_, JString(html)) = chunk \ "representations" \ "html"
        val JField(_, JArray(topics)) = chunk \ "features" \ "topics-01"

        if (plain.trim.size > 32) {
          val document = Document.add(text, chunkId, plain, html)
          val features = topics.map {
            case JDouble(value) => value
            case JInt(value) => value.toDouble
            case _ => 0.0
          }.toArray
          document.setFeatures(features)
        }
      }
    }
  }
}*/

class TDists(f: String, n: Int) {
  val s = io.Source.fromFile(f)
  val m = s.getLines.drop(1).map(_.split("\\s+").grouped(2).toList).foldLeft(Map.empty[String, Array[Double]]) {
    case (m, Array(_, h) :: fs) =>
      val a = Array.ofDim[Double](n)
      fs.foreach { case Array(k, v) => a(k.toInt) = v.toDouble }
      m + (h -> a)
  }
}

object SimpleImporter {
  def main(args: Array[String]) {
    val boot = new bootstrap.liftweb.Boot
    boot.boot

    val meta = Source.fromFile(args(2)).getLines.map { line =>
      val fields = line.split("\\t")
      (fields(0), (fields(2), fields(1), fields(3).toInt))
    }

    val LinePattern = """(\S+-\d\d\d\d)\s+_\s+(.*)""".r

    val docs = io.Source.fromFile(args(0)).getLines.map { line =>
      val LinePattern(h, b) = line
      (h, b.trim)
    }.toSeq

    val features = new TDists(args(1), 100)
    //val title = args(2)
    //val author = args(3)
    //val year = args(4).toInt
    //val text = Text.add("pg", title.replaceAll(" ", "_").toLowerCase, title, author, year)
    //docs.foreach { case (h, b) =>
    //  val document = Document.add(text, h, b, b)
    //  document.setFeatures(features.m(h))
    //}
    meta.foreach { case (bId, (title, author, year)) =>
      val text = Text.add("pg", title.replaceAll(" ", "_").toLowerCase, title, author, year)
      docs.filter { case (h, _) => 
        val f = h.split("-")(0)
        f == bId
      }.foreach { case (h, b) =>
        val document = Document.add(text, h, b, b)
        document.setFeatures(features.m(h))
      }
    }
    //features.m.foreach { case (k, v) => println(k + ": " + v.mkString(" ")) }
  }
}

