package org.clulab.reach.focusedreading.reinforcement_learning.exec.focused_reading

import breeze.linalg.{DenseVector, linspace}
import breeze.plot.{Figure, plot}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.clulab.reach.focusedreading.Participant
import org.clulab.reach.focusedreading.reinforcement_learning.environment.{Environment, SimplePathEnvironment}
import org.clulab.reach.focusedreading.reinforcement_learning.policies.{EpGreedyPolicy, LinearApproximationValues, ProxyValues, TabularValues}
import org.clulab.reach.focusedreading.reinforcement_learning.policy_iteration.td.{DQN, SARSA}
import org.clulab.reach.focusedreading.reinforcement_learning.Actions

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

  // The first argument is the input file
  // The first argument is the input file
//  val dataSet:Iterator[Tuple2[String, String]] = Iterator.continually(io.Source.fromFile(inputPath).getLines
//    .map{
//      s =>
//        val t = s.split("\t").toSeq
//        //(t(0), t(1), t(2))
//        (t.head, t.last)
//    }
//  ).flatten
//
//  def focusedReadingFabric():Option[Environment] = {
//    if(dataSet.hasNext){
//      val episode = dataSet.next
//      val participantA = Participant("", episode._1)
//      val participantB = Participant("", episode._2)
//
//      Some(new SimplePathEnvironment(participantA, participantB))
//    }
//    else
//      None
//  }

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
    }

    val episode = dataSetIterator.next
    val participantA = Participant("", episode._1)
    val participantB = Participant("", episode._2)

    Some(new SimplePathEnvironment(participantA, participantB))


  }

  val episodeBound = 1000
  val policyIteration = new SARSA(focusedReadingFabric, episodeBound, episodeBound)
  val qFunction = new ProxyValues(endPoint, architecture)
//  val qFunction = new LinearApproximationValues()
  val first_epsilon = 1.0
  val epsilonDecrease = first_epsilon/(episodeBound/2)
  val epsilons = ((0 to (episodeBound/2)).toStream.map(i => first_epsilon - (i*epsilonDecrease)).iterator) ++ Stream.continually(0.0)

  val initialPolicy = new EpGreedyPolicy(epsilons, qFunction)

  val learntPolicy = policyIteration.iteratePolicy(initialPolicy)

  // Store the policy somewhere
  // Serializer.save(learntPolicy, "learnt_policy.ser")
  learntPolicy.save(jsonPath)

  /*
//  val f = Figure()
//  val p = f.subplot(0)
//  val x = linspace(0.0, policyIteration.controlCount.toDouble, policyIteration.controlCount)
//
//  val num = qFunction.coefficientsExplore.size
//  val names = qFunction.coefficientsExplore.keySet.toSeq.sorted
//  for(i <- 0 until num) {
//    val history = DenseVector(qFunction.coefficientMemoryExplore.map {
//      v =>
//        if(v.length == 0)
//          0.0
//        else
//          v(i)
//    }.toArray)
//
//    p += plot(x, history, '-', null, names(i))
//  }

//  p.legend = true
//  p.xlabel = "Update #"
//  p.ylabel = "Coef Explore value"
//
//  f.saveas("plot_explore.png")

//  val f2 = Figure()
//  val p2 = f.subplot(0)
//  val x2= linspace(0.0, policyIteration.controlCount.toDouble, policyIteration.controlCount)
//
//  val num2 = qFunction.coefficientsExploit.size
//  val names2 = qFunction.coefficientsExploit.keySet.toSeq.sorted
//  for(i <- 0 until num2) {
//    val history = DenseVector(qFunction.coefficientMemoryExploit.map {
//      v =>
//        if(v.length == 0)
//          0.0
//        else
//          v(i)
//    }.toArray)
//
//    p2 += plot(x2, history, '-', null, names(i))
//  }
//
//  p2.legend = true
//  p2.xlabel = "Update #"
//  p2.ylabel = "Coef Exploit value"
//
//  f2.saveas("plot_exploit.png")
*/

}

