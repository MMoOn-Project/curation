import org.aksw.owlpod.{OwlpodRunner, _}
import org.aksw.owlpod.config._
import org.aksw.owlpod.reporting._
import org.aksw.owlpod.serialisation._
import org.aksw.owlpod.serialisation.OWLFormat._
import org.aksw.owlpod.serialisation.outputconfigs._
import org.aksw.owlpod.tasks._
import org.aksw.owlpod.tasks.ImportConfig._
import org.aksw.owlpod.util._
import org.semanticweb.owlapi.util.CommonBaseIRIMapper
import better.files._

object ProtegePostprocess extends OwlpodRunner with CommonRunConfig {

  lazy val setups = Seq(
    CurationSetup(
      name = "MMoOn OpenGerman Protege Post-Processing",
      ontDocSets = Seq(coreDocs, openGermanDocs, openBantuDocs),
      tasks = Seq(RemoveExternalAxioms()),
      outputConfig = ReplaceSources(
        postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle)),
      iriMappings = Seq(mmoonRepoIriMapper)
    )
  )
}

object Jenkins {

  def runner(targetDir: String, prefixesToStrip: Int = 0) = new CommonRunConfig with OwlpodRunner {

    lazy val setups = Seq(
      CurationSetup(
        name = s"Jenkins load to BG (w/o inf) and format multiplexing to $targetDir",
        ontDocSets = Seq(coreDocs, openGermanDocs),
        tasks = Seq(
          RemoveExternalAxioms(),
          LoadIntoBlazeGraph("mmoon", NoInferenceQuads(false, "http://mmoon.org/fallback/"))
        ),
        outputConfig = MultipleFormats(
          Set(Turtle, NTriples, OWLXML, RDFXML, Manchester, Functional),
          PreserveRelativePaths(mmoonRoot.pathAsString, targetDir, prefixesToStrip),
          postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle),
          overwriteExisting = true
        ),
        iriMappings = Seq(mmoonRepoIriMapper)
      ))
  }

  def main(args: Array[String]): Unit = args.toList match {

    case targetDir :: Nil => runner(targetDir).runSetups()
    case targetDir :: prefixesToStrip :: Nil => runner(targetDir, prefixesToStrip.toInt).runSetups()
    case _ => sys.error("expecting one or two command line arguments: target directory and optionally which number " +
      "of prefix segments to strip from the relative paths")
  }
}

trait CommonRunConfig { this: OwlpodRunner =>

  lazy val defaults = SetupDefaults(
    reporter = LogReporter(printStacktraces = true),
    executionPolicy = FailOnEverythingPolicy
  )

  lazy val mmoonRoot: File = File(".").path.toAbsolutePath

  lazy val coreDocs = OntologyDocumentList.relative("MMoOn/core.ttl")

  lazy val openGermanDocs = OntologyDocumentList.relative(
    "OpenGerman/deu/schema/og.ttl",
    "OpenGerman/deu/inventory/og.ttl"
  )

  lazy val openHebrewDocs = OntologyDocumentList.relative(
    "OpenHebrew/heb/schema/oh.ttl",
    "OpenHebrew/heb/inventory/oh.ttl"
  )

  lazy val openBantuDocs = OntologyDocumentList.relative("OpenBantu/bnt/schema/bantulm.ttl")

  lazy val mmoonRepoIriMapper: CommonBaseIRIMapper = {

    val im = new CommonBaseIRIMapper(mmoonRoot.uri)
    ontIRI2ShortPath foreach { case (iri, sp) => im.addMapping(iri, sp) }
    im
  }

  protected lazy val ontIRI2ShortPath = Map(
    "http://mmoon.org/core/".toIRI -> "MMoOn/core.ttl",
    "http://mmoon.org/core/v1.0.0/".toIRI -> "MMoOn/core.ttl",
    "http://mmoon.org/deu/schema/og/".toIRI -> "OpenGerman/deu/schema/og.ttl",
    "http://mmoon.org/deu/inventory/og/".toIRI -> "OpenGerman/deu/inventory/og.ttl",
    "http://mmoon.org/lang/heb/schema/oh/".toIRI -> "OpenHebrew/heb/schema/oh.ttl",
    "http://mmoon.org/lang/heb/inventory/oh/".toIRI -> "OpenHebrew/heb/inventory/oh.ttl",
    "http://www.mmoon.org/bnt/schema/bantulm/".toIRI -> "OpenBantu/bnt/schema/bantulm.ttl",
  )

  def main(args: Array[String]) {
    runSetups()
  }
}
