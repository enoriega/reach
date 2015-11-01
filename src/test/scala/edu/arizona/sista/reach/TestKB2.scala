package edu.arizona.sista.reach

import org.scalatest.{Matchers, FlatSpec}
import TestUtils._
import edu.arizona.sista.reach.grounding2._

/**
 * Unit tests to ensure the in-memory KB is working for grounding.
 *   Written by: Tom Hicks. 10/26/2015.
 *   Last Modified: Refactor and add tests for IMKB lookup types.
 */
class TestKB2 extends FlatSpec with Matchers {

  val imkb2 = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/uazclu/", "UAZ", "MIR:00000000"),
    "uniprot-subcellular-locations.tsv")

  "InMemoryKB COL-2" should "lookup on IMKB from COL-2 TSV file" in {
    (imkb2.lookup("NOT-IN-KB").isEmpty) should be (true)
    (imkb2.lookup("dendrite").isDefined) should be (true)
  }


  val imkb3 = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/uazclu/", "UAZ", "MIR:00000000"),
                   "ProteinFamilies.tsv.gz")

  // test lookups directly in IMKB (remember all keys must be lowercased!)
  "InMemoryKB COL-3" should "lookup on IMKB from COL-3 gzipped TSV file" in {
    (imkb3.lookup("NOT-IN-KB").isDefined) should be (false) // not in KB
    (imkb3.lookup("PTHR21244").isDefined) should be (false) // uppercase
    (imkb3.lookup("not-in-kb").isDefined) should be (false) // not it KB
    (imkb3.lookup("pthr21244").isDefined) should be (false) // has a species
    (imkb3.lookup("hk").isDefined) should be (false)        // has a species
  }

  "InMemoryKB COL-3" should "lookupByASpecies on IMKB from COL-3 gzipped TSV file" in {
    (imkb3.lookupByASpecies("NOT-IN-KB", "human").isDefined) should be (false) // not in KB
    (imkb3.lookupByASpecies("PTHR21244", "human").isDefined) should be (false) // uppercase
    (imkb3.lookupByASpecies("pthr21244", "human").isDefined) should be (true)
    (imkb3.lookupByASpecies("pthr21244", "HUMAN").isDefined) should be (true)
    (imkb3.lookupByASpecies("pthr21244", "mouse").isDefined) should be (true)
    (imkb3.lookupByASpecies("pthr21244", "aardvark").isDefined) should be (false) // not in KB
    (imkb3.lookupByASpecies("hk", "saccharomyces cerevisiae").isDefined) should be (true)
  }

  "InMemoryKB COL-3" should "lookupBySpecies on IMKB from COL-3 gzipped TSV file" in {
    (imkb3.lookupBySpecies("pthr21244", Set("aardvark")).isDefined) should be (false)
    (imkb3.lookupBySpecies("pthr21244", Set("human")).isDefined) should be (true)
    val pt = imkb3.lookupBySpecies("pthr21244", Set("human", "mouse"))
    (pt.isDefined) should be (true)
    (pt.get.size == 2) should be (true)
    (imkb3.lookupBySpecies("pthr21244", Set("human","mouse","gorilla")).isDefined) should be (true)
    val pt2 = (imkb3.lookupBySpecies("pthr21244", Set("human", "mouse","gorilla")))
    (pt2.isDefined) should be (true)
    (pt2.get.size == 2) should be (true)
    (imkb3.lookupBySpecies("hk", Set("human", "mouse")).isDefined) should be (false)
    (imkb3.lookupBySpecies("hk", Set("saccharomyces cerevisiae", "ant")).isDefined) should be (true)
    (imkb3.lookupBySpecies("hk", Set("ant", "saccharomyces cerevisiae")).isDefined) should be (true)
    (imkb3.lookupBySpecies(
      "hk", Set("ant", "saccharomyces cerevisiae")).get.size == 1) should be (true)
  }

  "InMemoryKB COL-3" should "lookupHuman on IMKB from COL-3 gzipped TSV file" in {
    (imkb3.lookupHuman("hk").isDefined) should be (false) // yeast only
    (imkb3.lookupHuman("PTHR21244").isDefined) should be (false)
    (imkb3.lookupHuman("pthr21244").isDefined) should be (true)
  }
}
