package org.clulab.reach.focusedreading.agents

import com.typesafe.scalalogging.LazyLogging
import org.clulab.processors.bionlp.BioNLPProcessor
import org.clulab.reach.focusedreading.FillBlanks.logger
import org.clulab.reach.focusedreading.ie.{REACHIEStrategy, SQLIteIEStrategy}
import org.clulab.reach.focusedreading.ir.QueryStrategy._
import org.clulab.reach.focusedreading.ir.{LuceneIRStrategy, Query, SQLIRStrategy}
import org.clulab.reach.focusedreading.reinforcement_learning.Actions
import org.clulab.reach.focusedreading.models._
import org.clulab.reach.focusedreading.reinforcement_learning.states._
import org.clulab.reach.focusedreading.reinforcement_learning.policies.Policy
import org.clulab.reach.focusedreading.{Connection, ExploreExploitParticipantsStrategy, MostConnectedParticipantsStrategy, Participant}
import org.clulab.reach.grounding.ReachKBUtils

import scala.collection.mutable
import scala.io.Source

/**
  * Created by enrique on 18/02/17.
  */
class LuceneReachSearchAgent(participantA:Participant, participantB:Participant) extends SimplePathAgent(participantA, participantB)
  with MostConnectedParticipantsStrategy
  with LuceneIRStrategy
  with REACHIEStrategy {


  //override val model:Model = Graph[Participant, LDiEdge](participantA, participantB) // Directed graph with the model.
  override val model:SearchModel = new GFSModel(participantA, participantB) // Directed graph with the model.



  override def choseQuery(source: Participant,
                          destination: Participant,
                          model: SearchModel) = Query(Cascade, source, Some(destination))


}

class SQLiteSearchAgent(participantA:Participant, participantB:Participant) extends SimplePathAgent(participantA, participantB)
  with MostConnectedParticipantsStrategy
  with SQLIRStrategy
  with SQLIteIEStrategy {


//  override val model:Model = Graph[Participant, LDiEdge](participantA, participantB) // Directed graph with the model.
  override val model:SearchModel = new GFSModel(participantA, participantB) // Directed graph with the model.



  override def choseQuery(source: Participant,
                          destination: Participant,
                          model: SearchModel) = Query(Cascade, source, Some(destination))

}

class SQLiteMultiPathSearchAgent(participantA:Participant, participantB:Participant) extends MultiplePathsAgent(participantA, participantB)
  with MostConnectedParticipantsStrategy
  with SQLIRStrategy
  with SQLIteIEStrategy {


  //  override val model:Model = Graph[Participant, LDiEdge](participantA, participantB) // Directed graph with the model.
  override val model:SearchModel = new GFSModel(participantA, participantB) // Directed graph with the model.



  override def choseQuery(source: Participant,
                          destination: Participant,
                          model: SearchModel) = Query(Cascade, source, Some(destination))


}


class PolicySearchAgent(participantA:Participant, participantB:Participant, val policy:Policy) extends SimplePathAgent(participantA, participantB)
  //with ExploreExploitParticipantsStrategy
  with MostConnectedParticipantsStrategy
  with SQLIRStrategy
  with SQLIteIEStrategy {

  val queryLog = new mutable.ArrayBuffer[(Participant, Participant)]
  val introductions = new mutable.HashMap[Participant, Int]

  introductions += participantA -> 0
  introductions += participantB -> 0

  override def choseEndPoints(source: Participant, destination: Participant, previouslyChosen: Set[(Participant, Participant)], model: SearchModel): (Participant, Participant) = {
    super.choseEndPoints(source, destination, previouslyChosen, model)
  }

  //  override val model:Model = Graph[Participant, LDiEdge](participantA, participantB) // Directed graph with the model.
  override val model:SearchModel = new GFSModel(participantA, participantB) // Directed graph with the model.



  override def choseQuery(source: Participant,
                          destination: Participant,
                          model: SearchModel) = {

    queryLog += Tuple2(source, destination)


    // Create state
    val state = this.observeState

    // Query the policy
    val action = policy.selectAction(state)



    // UNCOMMENT for policy learnt query strategy
    action match {
      case Actions.Conjunction =>
        Query(Conjunction, source, Some(destination))
      case Actions.Disjunction =>
        Query(Disjunction, source, Some(destination))
    }
    ///////////

    // UNCOMMENT for deterministic query strategy
    //Query(Cascade, source, Some(destination))
    //////////


  }

  def observeState:State = {
    if(queryLog.length > 0)
      fillState(this.model, iterationNum, queryLog, introductions)
    else{
      fillState(this.model, iterationNum, Seq((participantA, participantB)), introductions)
    }

  }

  private def fillState(model:SearchModel, iterationNum:Int, queryLog:Seq[(Participant, Participant)], introductions:mutable.Map[Participant, Int]):State = {

    val (a, b) = queryLog.last
    val log = queryLog flatMap (l => Seq(l._1, l._2))
    val paQueryLogCount = log.count(p => p == a)
    val pbQueryLogCount = log.count(p => p == b)

    val compA = model.getConnectedComponentOf(a).get
    val compB = model.getConnectedComponentOf(b).get

    val sameComponent = compA == compB

    val paIntro = introductions(a)
    val pbIntro = introductions(b)

    val ranks:Map[Participant, Int] = model.rankedNodes

    val paRank = getRank(a, ranks)
    val pbRank = getRank(b, ranks)

    val paLemmas = PolicySearchAgent.getParticipantLemmas(a)
    val pbLemmas = PolicySearchAgent.getParticipantLemmas(b)

    FocusedReadingState(RankBin.First, RankBin.Bottom, iterationNum, paQueryLogCount,pbQueryLogCount,sameComponent,paIntro,pbIntro, paLemmas, pbLemmas)
  }

  private def getRank(p:Participant, ranks:Map[Participant, Int]):RankBin.Value = {
    val rank = ranks(p)
    if(rank == 0)
      RankBin.First
    else{
      val size = ranks.size
      if(size < 3)
        RankBin.Upper
      else{
        val stride = size/3
        val cutPoints = 1.to(3).map(i => i*stride).reverse

        var ret =RankBin.Bottom

        val bins = Seq(RankBin.Bottom, RankBin.Mid, RankBin.Upper)

        for((point, i) <- cutPoints.zipWithIndex){
          if(rank <= point)
            ret = bins(i)
        }

        ret
      }

    }
  }

  override def reconcile(connections: Iterable[Connection]): Unit = {
    // Count the introductions
    for(f <- connections){
      val x = f.controller
      val y = f.controlled


      if(!introductions.contains(x))
        introductions += (x -> iterationNum)

      if(!introductions.contains(y))
        introductions += (y -> iterationNum)


    }

    super.reconcile(connections)
  }

}

object PolicySearchAgent{

  val processor = new BioNLPProcessor(withChunks=false, withCRFNER=false, withRuleNER=false, withContext=false)
  val vocLines = Source.fromFile("pubmedix.txt").getLines().toList
  val vocIx:Map[String, Int] = vocLines.map{
    s =>
      val t = s.split("\t")
      t.head -> t.last.toInt
  }.toMap

//  logger.info("Loading KBs...")
  var lines = ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("uniprot-proteins.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("GO-subcellular-locations.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("ProteinFamilies.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("PubChem.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("PFAM-families.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("bio_process.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("ProteinFamilies.tsv.gz")).getLines.toSeq
  lines ++= ReachKBUtils.sourceFromResource(ReachKBUtils.makePathInKBDir("hgnc.tsv.gz")).getLines.toSeq

  val dict: Map[String, Seq[String]] = lines.map{ l => val t = l.split("\t"); (t(1), t(0)) }.groupBy(t=> t._1).mapValues(l => l.map(_._2).distinct)

  val lemmasCache = new mutable.WeakHashMap[Participant, Seq[Seq[Int]]]//new mutable.HashMap[Participant, Seq[Seq[Int]]]

  def getParticipantLemmas(participant: Participant):Seq[Seq[Int]] = {
    if(lemmasCache.contains(participant))
      lemmasCache(participant)
    else {
      val synonyms = dict.get(participant.id)
      val ret =
        (synonyms match {
          case Some(syns) =>
            val doc = processor.annotateFromSentences(syns)
            val indices =
              for (sen <- doc.sentences) yield {
                val lemmas = sen.lemmas.get
                lemmas.map(vocIx.lift).collect { case Some(i) => i }.toSeq
              }
            indices.toSeq filter (_.nonEmpty)
          case None =>
            Seq.empty[Seq[Int]]
        })

      lemmasCache += (participant -> ret)
      ret
    }

  }
}


