package wpbackup

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WordPressBackupTest extends AnyWordSpec with Matchers {
  val url = "pastforward.us"
  s"${Main.getClass.getSimpleName}" should {
    "parse" in {
      Main.main(Array(url))
    }
  }
}
