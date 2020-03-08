package org.clulab.reach.focusedreading

import java.io.PrintWriter

import org.clulab.processors.bionlp.BioNLPProcessor

import scala.io.Source
import scala.util.{Success, Try}

object DigestW2VVoc extends App {

  val processor = new BioNLPProcessor(withChunks=false, withCRFNER=false, withRuleNER=false, withContext=false)
  val vocLines = Source.fromFile("pubmedvoc.txt").getLines().toList

  val vocIx =
    (for((line, ix) <- vocLines.zipWithIndex.par) yield {
      val doc = processor.annotateFromTokens(Seq(Seq(line)))
      Try(doc.sentences(0).lemmas.get(0) -> ix)
    }).collect{
      case Success(x) => x
    }

  val pw = new PrintWriter("pubmedix.txt")
  for((word, ix) <- vocIx.seq){
    val s = s"$word\t$ix"
    println(s)
    pw.println(s)
  }
  pw.close()
}
