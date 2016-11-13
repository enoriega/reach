package org.clulab.reach.grounding

import org.clulab.odin._
import org.clulab.reach.grounding.ReachKBKeyTransforms._

/**
  * Class implementing alternate key mention lookups for cell types which can be inferred
  * from the organ name and specific contextual suffixes.
  *   Written by Tom Hicks. 12/20/2015.
  *   Last Modified: Rename class. Shorten organ vals.
  */
class IMKBOrganMentionLookup (

  /** The in-memory knowledge base that all lookups will work against. */
  memoryKB: InMemoryKB = new InMemoryKB()

) extends IMKBMentionLookup (memoryKB) {

  /** Resolve the given Mention to an optional entry in a knowledge base.
    * Return a resolution for the entry, if any found.
    */
  override def resolve (mention:Mention): Resolutions =
    resolveAlt(mention.text, organKeyTransforms)

  /** Resolve the given Mention to an optional entry in a knowledge base,
    * for the single named species.
    * Return a resolution for the entry, if any found.
    */
  override def resolveByASpecies (mention:Mention, species:String): Resolutions =
    resolveByASpeciesAlt(mention.text, species, organKeyTransforms)

  /** Resolve the given Mention to an optional group of entries in a knowledge base,
    * returning resolutions for all species entries found in the KB.
    */
  override def resolveBySpecies (mention:Mention, speciesSet:SpeciesNameSet): Resolutions =
    resolveBySpeciesAlt(mention.text, speciesSet, organKeyTransforms)

  /** Resolve the given Mention to an optional entry in a knowledge base,
    * failing if the entry is not for humans.
    * Return a resolution for a human entry, if any found.
    */
  override def resolveHuman (mention:Mention): Resolutions =
    resolveHumanAlt(mention.text, organKeyTransforms)

  /** Resolve the given Mention to an optional entry in a knowledge base which
    * explicitly does not have an associated species. Fail if all entries have species.
    * Return a resolution for the entry, if any found.
    */
  override def resolveNoSpecies (mention:Mention): Resolutions =
    resolveNoSpeciesAlt(mention.text, organKeyTransforms)

}