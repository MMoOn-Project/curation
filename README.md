## MMoOn curation tooling

This repository ties MMoOn (sub-)repositories together with a Scala/SBT-based
tool to prepare changes in the contained repositories for commits (to keep diffs
small and meaningful) and for release (for CI).

### Sub-Repositories (Submodules)

Sub-repositories to be postprocessed and/or released have been added as 
[Git submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules) that
point to the `release` branch of referred repos.

To get this repo in a working state perform init and update of these 
submodules after cloning via `git submodule update --init --remote`.

Use `git submodule foreach 'git checkout origin/release'` if you want to 
re-sync the sub-repos with the `origin` (i.e. GitHub) state later.

### OWLPod usage

[SBT](http://www.scala-sbt.org) is used to afford running various configuration
OWLPod that will reformat OWL documents, multiplex to different serialisations and
perform (depending on the chosen configuration), some other refactoring/post-processing
tasks, e.g. materialize RDFS, RDFS+ or OWL DL inferences.

#### available OWLPod configurations

`sbt run` or `sbt runMain ProtegePostprocess`:

Post processes ontology documents in place. It removed declarations to external 
ontologies referred, but not imported (which are automatically added when saving from
Protegé Desktop). For Turtle, it also removes (redundant) comment lines, making the
resulting serialisation more compact and readable.


