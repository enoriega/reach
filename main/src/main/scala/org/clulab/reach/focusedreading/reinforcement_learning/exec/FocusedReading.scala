package org.clulab.reach.focusedreading.reinforcement_learning.exec.focused_reading

import java.io.PrintWriter

import breeze.linalg.{DenseVector, linspace}
import breeze.plot.{Figure, plot}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.clulab.reach.focusedreading.Participant
import org.clulab.reach.focusedreading.reinforcement_learning.environment.{Environment, SimplePathEnvironment}
import org.clulab.reach.focusedreading.reinforcement_learning.policies.{EpGreedyPolicy, LinearApproximationValues, ProxyValues, TabularValues}
import org.clulab.reach.focusedreading.reinforcement_learning.policy_iteration.td.{DQN, SARSA}
import org.clulab.reach.focusedreading.reinforcement_learning.Actions

import scala.collection.mutable

/**
  * Created by enrique on 31/03/17.
  */

object TabularSARSA extends App with LazyLogging {

  val conf = ConfigFactory.load()
  val inputPath = conf.getString("DyCE.Training.file")
  val jsonPath = conf.getString("DyCE.Training.policyFile")
  val endPoint = conf.getString("DyCE.endpoint")

  // The first argument is the input filed
  val dataSet:Iterator[Tuple2[String, String]] = Iterator.continually(io.Source.fromFile(inputPath).getLines
    .map{
      s =>
        val t = s.split("\t").toSeq
        //(t(0), t(1), t(2))
        (t.head, t.last)
    }
  ).flatten

  def focusedReadingFabric():Option[Environment] = {
    if(dataSet.hasNext){
      val episode = dataSet.next
      val participantA = Participant("", episode._1)
      val participantB = Participant("", episode._2)

      Some(new SimplePathEnvironment(participantA, participantB))
    }
    else
      None
  }

  val policyIteration = new SARSA(focusedReadingFabric, 10000, 30, 0.005)
  val qFunction = new TabularValues(0)
  val initialPolicy = new EpGreedyPolicy(Stream.continually(0.1).iterator, qFunction)

  val learntPolicy = policyIteration.iteratePolicy(initialPolicy)

  // Store the policy somewhere
  // Serializer.save(learntPolicy, "learnt_policy.ser")
}

object LinearSARSA extends App with LazyLogging {

  val conf = ConfigFactory.load()
  val inputPath = conf.getString("DyCE.Training.file")
  val jsonPath = conf.getString("DyCE.Training.policyFile")
  val endPoint = conf.getString("DyCE.endpoint")
  val architecture = conf.getString("DyCE.architecture")
  val epsilon = conf.getDouble("DyCE.Training.epsilon")
  val epochs= conf.getInt("DyCE.Training.epochs")
  val lr = conf.getDouble("DyCE.Training.lr")
  val burn_in = conf.getInt("DyCE.Training.burn_in")
  val epochRewards = new mutable.ArrayBuffer[mutable.ArrayBuffer[mutable.ArrayBuffer[Double]]]()

  // The first argument is the input file
  val dataSet:List[Tuple2[String, String]] = io.Source.fromFile(inputPath).getLines.toList
    .map{
      s =>
        val t = s.split("\t").toSeq
        //(t(0), t(1), t(2))
        (t.head, t.last)
    }

  var dataSetIterator = dataSet.iterator

  def focusedReadingFabric():Option[Environment] = {

    if(dataSetIterator.isEmpty) {
      logger.info(s"Finished epoch of ${dataSet.size} instances")
      dataSetIterator = dataSet.iterator
      epochRewards += policyIteration.observedRewards
      policyIteration.observedRewards = new mutable.ArrayBuffer[mutable.ArrayBuffer[Double]]()
    }

    val episode = dataSetIterator.next
    val participantA = Participant("", episode._1)
    val participantB = Participant("", episode._2)

    Some(new SimplePathEnvironment(participantA, participantB))
  }

  val episodeBound = epochs
  val policyIteration = new SARSA(focusedReadingFabric, episodeBound, burn_in, alpha = lr)
  val qFunction = new ProxyValues(endPoint, architecture)
//  val qFunction = new LinearApproximationValues()
  val first_epsilon = epsilon
  val epsilonDecrease = first_epsilon/(episodeBound/2)
  val epsilons = ((0 to (episodeBound/2)).toStream.map(i => first_epsilon - (i*epsilonDecrease)).iterator) ++ Stream.continually(0.0)

  val initialPolicy = new EpGreedyPolicy(epsilons, qFunction)

  val learntPolicy = policyIteration.iteratePolicy(initialPolicy)

  // Fetch the observed rewards
  def mk_reward_strings() = {
    epochRewards.map{
      episodesRewards =>
        episodesRewards.map{
          episodeRewards =>
            episodeRewards.mkString(" ") + "\n"
        }.mkString("")
    }.mkString("\n")
  }

  val pw = new PrintWriter("epoch_rewards.txt")
  val line = mk_reward_strings()
  pw.write(line)
  pw.close()

  // Store the policy somewhere
  learntPolicy.save(jsonPath)

}

