package org.clulab.reach.focusedreading.reinforcement_learning.exec.focused_reading

import breeze.linalg.{DenseVector, linspace}
import breeze.plot.{Figure, plot}
import org.clulab.reach.focusedreading.Participant
import org.clulab.reach.focusedreading.reinforcement_learning.environment.{Environment, SimplePathEnvironment}
import org.clulab.reach.focusedreading.reinforcement_learning.policies.{EpGreedyPolicy, LinearApproximationValues, ProxyValues, TabularValues}
import org.clulab.reach.focusedreading.reinforcement_learning.policy_iteration.td.{DQN, SARSA}

/**
  * Created by enrique on 31/03/17.
  */

object TabularSARSA extends App {
  // The first argument is the input filed
  val dataSet:Iterator[Tuple2[String, String]] = Iterator.continually(io.Source.fromFile(args(0)).getLines
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
  val initialPolicy = new EpGreedyPolicy(0.1, qFunction)

  val learntPolicy = policyIteration.iteratePolicy(initialPolicy)

  // Store the policy somewhere
  // Serializer.save(learntPolicy, "learnt_policy.ser")
}

object LinearSARSA extends App {
  // The first argument is the input file
  // The first argument is the input file
  val dataSet:Iterator[Tuple2[String, String]] = Iterator.continually(io.Source.fromFile(args(0)).getLines
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

  val policyIteration = new DQN(focusedReadingFabric, 2000, 30)
  val qFunction = new ProxyValues("http://localhost:5000")
//  val qFunction = new LinearApproximationValues()
  val initialPolicy = new EpGreedyPolicy(0.1, qFunction)

  val learntPolicy = policyIteration.iteratePolicy(initialPolicy)

  // Store the policy somewhere
  // Serializer.save(learntPolicy, "learnt_policy.ser")
  learntPolicy.save("learnt_policy.json")

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
}

