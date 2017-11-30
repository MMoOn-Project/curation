import ProtegePostprocess.{mmoonRepoIriMapper, openBantuDocs}
import better.files._
import org.aksw.owlpod.OwlpodRunner
import org.aksw.owlpod.config._
import org.aksw.owlpod.execution.FailOnEverythingPolicy
import org.aksw.owlpod.postprocessing._
import org.aksw.owlpod.preprocessing._
import org.aksw.owlpod.reporting._
import org.aksw.owlpod.serialisation.OWLFormat._
import org.aksw.owlpod.serialisation.outputconfigs._
import org.aksw.owlpod.tasks.ImportConfig._
import org.aksw.owlpod.tasks._
import org.aksw.owlpod.util._
import org.semanticweb.owlapi.util.CommonBaseIRIMapper

object ProtegePostprocess extends OwlpodRunner with CommonRunConfig {

  lazy val setups = Seq(
    CurationSetup(
      name = "MMoOn OpenGerman Protege Post-Processing",
      ontDocSets = Seq(openGermanDocs),
      tasks = Seq(),
      postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle),
      outputConfig = ReplaceSources(),
      iriMappings = Seq(mmoonRepoIriMapper)
    ),
    CurationSetup(
      name = "MMoOn OpenBantu Protege Post-Processing",
      ontDocSets = Seq(openBantuDocs),
      tasks = Seq(RemoveImports("urn://www.mmoon.org/fix/bantulm/ext-decl/", "http://mmoon.org/core/")),
      preprocessors = Seq(AddImports("urn://www.mmoon.org/fix/bantulm/ext-decl/", "http://mmoon.org/core/")),
      postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle),
      outputConfig = ReplaceSources(),
      iriMappings = Seq(mmoonRepoIriMapper),
    )
  )
}

object Jenkins {

  def runner(targetDir: String, prefixesToStrip: Int = 0) = new CommonRunConfig with OwlpodRunner {

    lazy val setups = Seq(
      CurationSetup(
        name = s"Jenkins - MMoOn Core & OpenGerman: Format multiplexing to $targetDir and plain Blazegraph import",
        ontDocSets = Seq(coreDocs, openGermanDocs),
        tasks = Seq(
          RemoveExternalAxioms(),
          LoadIntoBlazeGraph("mmoon", NoInferenceQuads(fulltextIndexing = false, "http://mmoon.org/fallback/"))
        ),
        postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle),
        outputConfig = MultipleFormats(
          Set(Turtle, NTriples, OWLXML, RDFXML, Manchester, Functional),
          PreserveRelativePaths(mmoonRoot.pathAsString, targetDir, prefixesToStrip),
          overwriteExisting = true
        ),
        iriMappings = Seq(mmoonRepoIriMapper)
      ),
      PublicationSetup(
        name = s"Jenkins - MMoOn Core & OpenGerman: OWLAPI Inferences to Blazegraph",
        ontDocSets = Seq(coreDocs, openGermanDocs),
        tasks = Seq(
          RemoveExternalAxioms(), AddInferences(),
          LoadIntoBlazeGraph("mmoon-inf", NoInferenceQuads(fulltextIndexing = false, "http://mmoon.org/fallback/"))
        ),
        iriMappings = Seq(mmoonRepoIriMapper)
      ),
      CurationSetup(
        name = s"Jenkins - OpenBantu: format multiplexing to $targetDir",
        ontDocSets = Seq(openBantuDocs),
        tasks = Seq(RemoveImports("urn://www.mmoon.org/fix/bantulm/ext-decl/", "http://mmoon.org/core/")),
        preprocessors = Seq(AddImports("urn://www.mmoon.org/fix/bantulm/ext-decl/", "http://mmoon.org/core/")),
        postprocessors = Seq(TrimComments(), NormalizeBlankLinesForTurtle),
        outputConfig = MultipleFormats(
          Set(Turtle, NTriples, OWLXML, RDFXML, Manchester, Functional),
          PreserveRelativePaths(mmoonRoot.pathAsString, targetDir, prefixesToStrip),
          overwriteExisting = true
        ),
        iriMappings = Seq(mmoonRepoIriMapper),
      )
    )
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
    ontIRI2ShortPath foreach { case (iri, sp) => im.addMapping(iri.toIRI, sp) }
    im
  }

  protected lazy val ontIRI2ShortPath = Map(
    "http://mmoon.org/core/" -> "MMoOn/core.ttl",
    "http://mmoon.org/core/v1.0.0/" -> "MMoOn/core.ttl",
    "http://mmoon.org/deu/schema/og/" -> "OpenGerman/deu/schema/og.ttl",
    "http://mmoon.org/deu/inventory/og/" -> "OpenGerman/deu/inventory/og.ttl",
    "http://mmoon.org/lang/heb/schema/oh/" -> "OpenHebrew/heb/schema/oh.ttl",
    "http://mmoon.org/lang/heb/inventory/oh/" -> "OpenHebrew/heb/inventory/oh.ttl",
    "http://www.mmoon.org/bnt/schema/bantulm/" -> "OpenBantu/bnt/schema/bantulm.ttl",
    "urn://www.mmoon.org/fix/bantulm/ext-decl/" -> "OpenBantu/external-declarations.ttl"
  )

  def main(args: Array[String]) {
    runSetups()
  }
}
