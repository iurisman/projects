package wpbackup

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupElement
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

import java.nio.file.{Files, Path, Paths}
import java.time.Instant
import java.io.File

object Main {

  def usage(myname: String) = s"Usage: $myname <URL>"

  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      System.err.println(usage("TBD"))
      System.exit(2)
    }

    // Validate the URL given on the command line
    val url: java.net.URL = {
      var candidate = args(0)
      var result = Option.empty[java.net.URL]
      while (result.isEmpty) {
        try {
          result = Some(new java.net.URL(candidate))
        } catch {
          case muex: java.net.MalformedURLException if muex.getMessage.contains("no protocol") =>
            candidate = "http://" + candidate
        }
      }
      result.get
    }

    val tempDir = Files.createTempDirectory("WPBackup")
    val browser = JsoupBrowser()
    val home = browser.parseInputStream(url.openStream())
    val links = home >> extractor("h2.post-title a")
    val fileNames = links
      .map { link =>
        val href = link.attr("href")
        val post = browser.get(href)
        val title = {post >> extractor("h1.post-title")}.head.text
        val body = {post >> extractor(".post-content")}.head.innerHtml
        val path = Paths.get(tempDir.toString, title)
        Files.write(path, body.getBytes)
        path.toString
      }
    val zipArchiveName = s"./wp-backup.${Instant.now()}.zip"
    zip(zipArchiveName, fileNames)
  }

  def zip(out: String, files: Iterable[String]): Unit = {
    import java.io.{ BufferedInputStream, FileInputStream, FileOutputStream }
    import java.util.zip.{ ZipEntry, ZipOutputStream }

    val zip = new ZipOutputStream(new FileOutputStream(out))

    files.foreach { qualifiedFileName =>
      val shortFileName = new File(qualifiedFileName).getName
      zip.putNextEntry(new ZipEntry(shortFileName))
      val in = new BufferedInputStream(new FileInputStream(qualifiedFileName))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
    zip.close()
  }
}

