package edu.arizona.sista.reach.grounding

import edu.arizona.sista.odin._
import edu.arizona.sista.reach.grounding.ReachKBConstants._

/**
  * A collection of classes which provide mappings of Mentions to identifiers
  * using an encapsulated, locally-sourced knowledge base.
  *   Written by: Tom Hicks. 10/28/2015.
  *   Last Modified: Update for refactored IMKB lookups.
  */

//
// Subcellular Location Accessors
//

/** KB accessor to resolve subcellular location names via KBs generated from the BioPax model. */
class GendCellLocationKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), GendCellLocationFilename)
}

/** KB accessor to resolve subcellular location names via manually maintained KBs. */
class ManualCellLocationKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), ManualCellLocationFilename)
}

/** KB mention lookup to resolve subcellular location names via static KBs. */
class StaticCellLocationKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/go/", "go", "MIR:00000022"),
                   StaticCellLocationFilename)
}

/** KB mention lookup to resolve alternate subcellular location names via static KBs. */
class StaticCellLocationKBML2 extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/uniprot/", "uniprot", "MIR:00000005"),
                   StaticCellLocation2Filename)
}


//
// Small Molecule (Chemical and Metabolite) Accessors
//

/** KB accessor to resolve small molecule (chemical) names via KBs generated from the BioPax model. */
class GendChemicalKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), GendChemicalFilename)
}

/** KB accessor to resolve small molecule (chemical) names via manually maintained KBs. */
class ManualChemicalKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), ManualChemicalFilename)
}

/** KB accessor to resolve small molecule (metabolite) names via static KBs. */
class StaticMetaboliteKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/hmdb/", "hmdb", "MIR:00000051"),
                   StaticMetaboliteFilename)
}

/** KB accessor to resolve small molecule (chemical) names via static KBs. */
class StaticChemicalKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/chebi/", "chebi", "MIR:00100009"),
                   StaticChemicalFilename)
}


//
// Protein Accessors
//

/** KB accessor to resolve protein names via KBs generated from the BioPax model. */
class GendProteinKBML extends IMKBProteinMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), GendProteinFilename)
}

/** KB accessor to resolve protein names via manually maintained KBs. */
class ManualProteinKBML extends IMKBProteinMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), ManualProteinFilename)
}

/** KB accessor to resolve protein names via static KBs. */
class StaticProteinKBML extends IMKBProteinMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/uniprot/", "uniprot", "MIR:00100164"),
                   StaticProteinFilename, true) // true = has species
}


//
// Protein Family Accessors
//   These extend from LocalAltKBMentionLookup because protein & protein family
//   alternate lookups are the same for now.
//

/** KB accessor to resolve protein family names via KBs generated from the BioPax model. */
class GendProteinFamilyKBML extends IMKBFamilyMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), GendProteinFilename)
}

/** KB accessor to resolve protein names via manually maintained KBs. */
class ManualProteinFamilyKBML extends IMKBFamilyMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo(), ManualProteinFilename)
}

/** KB accessor to resolve protein family names via static KBs. */
class StaticProteinFamilyKBML extends IMKBFamilyMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/interpro/", "interpro", "MIR:00000011"),
                   StaticProteinFamilyFilename, true) // true = has species
}


//
// Tissue Type Accessor
//

/** KB accessor to resolve tissue type names via static KBs. */
class StaticTissueTypeKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(
    new KBMetaInfo("http://identifiers.org/uniprot/", "uniprot", "MIR:00000005"),
                   StaticTissueTypeFilename)
}


//
// Failsafe Accessor
//

/** KB accessor implementation which always resolves each mention with a local, fake ID. */
class AzFailsafeKBML extends IMKBMentionLookup {
  val memoryKB = new InMemoryKB(new KBMetaInfo()) // no external KB file to load!

  private val idCntr = new IncrementingCounter() // counter sequence class

  // base resolve of text string which does all the work for this class
  override def resolve (text:String): Option[KBResolution] = {
    val key = makeCanonicalKey(text)
    val entry = memoryKB.lookupNoSpecies(key)   // look for an existing entry
    if (entry.isDefined)                        // if KB entry is already defined
      return memoryKB.newResolution(entry)      // create/wrap return value
    else {                                      // else no existing entry, so
      val refId = "UAZ%05d".format(idCntr.next) // create a new reference ID
      val kbe = new KBEntry(text, key, refId)   // create a new KB entry
      memoryKB.insertOrUpdateEntry(kbe)         // insert the new KB entry
      return Some(memoryKB.newResolution(kbe))  // wrap return value in optional
    }
  }

  // implementations which ignore the given species and defer to the base text resolve
  override def resolveHuman (text:String): Option[KBResolution] = resolve(text)
  override def resolveByASpecies (text:String, species:String): Option[KBResolution] = resolve(text)
  override def resolveBySpecies (text:String, speciesSet:SpeciesNameSet): Option[Iterable[KBResolution]] = Some(Iterable(resolve(text).get))
  override def resolveNoSpecies (text:String): Option[KBResolution] = resolve(text)

  // mention resolves which also ignore the given species and defer to the base text resolve
  override def resolve (mention:Mention): Option[KBResolution] = resolve(mention.text)
  override def resolveHuman (mention:Mention): Option[KBResolution] = resolve(mention.text)
  override def resolveByASpecies (mention:Mention, species:String): Option[KBResolution] =
    resolve(mention.text)
  override def resolveBySpecies (mention:Mention, speciesSet:SpeciesNameSet): Option[Iterable[KBResolution]] = resolveBySpecies(mention.text, speciesSet)
  override def resolveNoSpecies (mention:Mention): Option[KBResolution] = resolve(mention.text)
}