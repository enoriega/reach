package org.clulab.reach.focusedreading.executable

import com.typesafe.scalalogging.LazyLogging
import org.clulab.reach.focusedreading.{Connection, Participant}
import org.clulab.reach.focusedreading.agents._
import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import org.json4s.JsonDSL._
import org.json4s._

import scala.collection.JavaConverters._
import org.json4s.native.JsonMethods._
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.clulab.reach.focusedreading.tracing.AgentRunTrace

import scala.collection.mutable

/**
  * Created by enrique on 12/03/17.
  */
object SimplePath extends App with LazyLogging{

  val conf = ConfigFactory.load()
  val inputPath = conf.getString("DyCE.Testing.file")

    def getParticipants(path:List[Connection]):List[String] = {
      path match {
        case h::t =>
          h.controller.id :: (if(t == Nil) List(h.controlled.id) else getParticipants(t))
        case Nil => Nil
      }
    }

    def serializeItem(path:Seq[Connection], groundTruth:Seq[String], matchType:String, agent:SearchAgent, file:File): Unit ={
      val pathInfo = ("ground_truth" -> groundTruth) ~
        ("match_type" -> matchType) ~
        ("path" -> (path map (c => s"${c.controller} ${if(c.sign) "+" else "-"}> ${c.controlled}"))) ~
        ("evicende" -> {
          path map {
            c =>
              agent.getEvidence(c)
          }
        })

      val model = agent.model
      val participants = path flatMap (c => Set(c.controller, c.controlled))


      val jsonObj = ("info" -> pathInfo) ~ ("degrees" -> participants.map(d => (d.toString -> model.degree(d))).toMap)

      val json = pretty(render(jsonObj))

      FileUtils.writeLines(file, Seq(json).asJavaCollection)
    }

    // The first argument is the input file
    val dataSet:Iterable[Seq[String]] = io.Source.fromFile(inputPath).getLines
      .map{
        s =>
          val t = s.split("\t").toSeq
          //(t(0), t(1), t(2))
          t
      }.toSeq

    var (successes, failures, hits) = (0, 0, 0)
    val pathLengths = new mutable.ArrayBuffer[Int]
    val successIterations = new mutable.ArrayBuffer[Int]
    val failureIterations = new mutable.ArrayBuffer[Int]

    logger.info(s"About to do ${dataSet.size} searches ...")
    logger.info(s"")


    /***
      * Prints the sentences that back the evidence found
      * @param path
      */
    def printEvidence(path: Seq[Connection], agent:SearchAgent) = {
      val evidence:Seq[Iterable[String]] = path map agent.getEvidence

      for((c, e) <- path zip evidence){
        logger.info("")
        logger.info(s"Evidence for connection $c")
        logger.info("")
        for(s <- e){
          logger.info(s)
        }
      }
    }

    val times = new mutable.ArrayBuffer[Long]
    val papers = new mutable.ArrayBuffer[String]
    var numQueries = 0

    val bootstrap = new mutable.HashMap[Int, (Boolean, Int, String)]() // (Success, # queries, papers)

    for((datum, ix) <- dataSet.zipWithIndex){

      val start = System.nanoTime()

      logger.info(s"Searching for path: ${datum.mkString(" - ")}")

      val participantA =  Participant("", datum.head)
      val participantB = Participant("", datum.last)

      logger.info(s"About to start a focused search $ix of ${dataSet.size}")

      //val agent = new LuceneReachSearchAgent(participantA, participantB)
      val agent = new SQLiteSearchAgent(participantA, participantB)
      // val agent = new SQLiteMultiPathSearchAgent(participantA, participantB)
      agent.focusedSearch(participantA, participantB)


      val recoveredPath = agent.successStopCondition(participantA, participantB, agent.model) match {
        case Some(paths) =>
          successes += 1
          successIterations += agent.iterationNum
          logger.info("Success!!")

          val path = paths.head

          logger.info("")
          logger.info("Path: " + path.mkString(" || "))


          // Analysis
          val participants = getParticipants(path.toList)
          val groundTruth = datum.toList
          logger.info("GT: " + groundTruth.mkString(" || "))

          assert(participants.head == groundTruth.head)
          assert(participants.last == groundTruth.last)

          val relevantParticipants = participants filter (p => groundTruth.contains(p))
          val matchType = if(relevantParticipants == groundTruth) {
            hits += 1
            logger.info(s"Type: Exact")
            "exact"
          }
          else{
            // Get the length of the paths
            pathLengths += participants.length
            logger.info(s"Type: Alternative")
            "alternative"
          }

          serializeItem(path, groundTruth, matchType, agent, new File(s"hits/hit_$ix.json"))


          logger.info("End Success")

          Some(path)

        case None =>
          failures += 1
          failureIterations += agent.iterationNum
          logger.info("Failure")
          None

      }

      // Store the trace for analysis
      val trace = AgentRunTrace(participantA, participantB,
        agent.trace, recoveredPath, Some(datum))

      val tracePath = AgentRunTrace.getFileName(datum)

      var success = true
      recoveredPath match {
        case Some(_) =>
          AgentRunTrace.save(trace, Paths.get("traces", "successes", tracePath))
          success = true
        case None =>
          AgentRunTrace.save(trace, Paths.get("traces", "failures", tracePath))
          success = false
      }

      numQueries += agent.iterationNum
      val end = System.nanoTime()

      times += (end - start)
      papers ++= agent.papersRead

      bootstrap += (ix -> (success, agent.iterationNum, agent.papersRead.mkString(",")))

      logger.info("")
    }
    logger.info(s"Finished attaining $successes successes and $failures failures")
    logger.info("")
    logger.info("Postmortem analysis:")
    logger.info(s"Exact matches: $hits\t Alternative matches: ${successes-hits}")
    logger.info("")
    // Length counts
    val lengths = pathLengths.groupBy(identity).mapValues(_.size)
    logger.info(s"Alternate path lengths:")
    val keys = lengths.keys.toSeq.sortBy(k => lengths(k))
    for(l <- keys){
      logger.info(s"\tLength $l: ${lengths(l)}")
    }

    logger.info("")
    logger.info("Iteration counts for successes")
    val sIterations = successIterations.groupBy(identity).mapValues(_.size)
    for(l <- sIterations.keys.toSeq.sorted){
      logger.info(s"\t$l iterations: ${sIterations(l)}")
    }

    logger.info("")
    logger.info("Iteration counts for failures")
    val fIterations = failureIterations.groupBy(identity).mapValues(_.size)
    for(l <- fIterations.keys.toSeq.sorted){
      logger.info(s"\t$l iterations: ${fIterations(l)}")
    }


    val averageRuntime = (times.sum / times.size)

    logger.info(s"Average running time: $averageRuntime")
    logger.info(s"Unique papers read: ${papers.toSet.size}")
    logger.info(s"# of queries: $numQueries")

  val bootstrapLines = bootstrap.map {
    case (ix, v) =>
      s"$ix\t${v._1}\t${v._2}\t${v._3}\n"
  }

  val osw = new BufferedWriter(new FileWriter("baseline_bootstrap.txt"))

  bootstrapLines foreach osw.write

  osw.close
}

