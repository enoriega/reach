package org.clulab.reach.focusedreading.reinforcement_learning.policies

import java.io.{BufferedWriter, FileWriter}

import breeze.linalg._
import breeze.stats.distributions.Multinomial
import org.clulab.reach.focusedreading.reinforcement_learning.Actions
import org.clulab.reach.focusedreading.reinforcement_learning.states.State
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

/**
  * Created by enrique on 26/03/17.
  */
class EpGreedyPolicy(epsilons:Iterator[Double], val values:Values) extends Policy {

  val first_epsilon = epsilons.next()
  assert(first_epsilon <= 1 && first_epsilon >= 0, s"Invalid Epsilon value: $first_epsilon")

  override def selectAction(s: State):Actions.Value = {
    val epsilon = epsilons.next()
    val numActions = Actions.values.size
    val slice = epsilon / numActions
    val greedyProb = 1 - epsilon + slice

    val possibleActions:Seq[(State, Actions.Value)] = Actions.values.toSeq.map(a => (s, a))
    val possibleActionValues = possibleActions map (k => values(k))
    val sortedActions = possibleActions.zip(possibleActionValues).sortBy{case(sa, v) => v}.map(_._1._2).reverse
    val probs = greedyProb::List.fill(numActions-1)(slice)

    // Do a random sample from a multinomial distribution using probs as parameter
    val dist = Multinomial(DenseVector(probs.toArray))

    val choiceIx = dist.sample
    val choice = sortedActions(choiceIx)

    // Return the random sample
    choice
  }

  override def save(path: String): Unit = save(None, path)

  def save(name:Option[String], path:String): Unit ={
    val ast = {
      ("type" -> "ep_greedy") ~
      ("epsilon" -> first_epsilon) ~
        ("values" -> (name match {
          case Some(n) =>
            values.asInstanceOf[ProxyValues].toJson(n)
          case None =>
            values.toJson
        })
        )
    }

    val json = pretty(render(ast))

    val bfw = new BufferedWriter(new FileWriter(path))
    bfw.write(json)
    bfw.close()
  }

  def makeGreedy:GreedyPolicy = new GreedyPolicy(values)
}
