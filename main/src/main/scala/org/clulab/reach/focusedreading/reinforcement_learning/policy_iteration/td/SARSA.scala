package org.clulab.reach.focusedreading.reinforcement_learning.policy_iteration.td

import org.clulab.reach.focusedreading.reinforcement_learning.policies._
import com.typesafe.scalalogging.LazyLogging
import org.clulab.reach.focusedreading.reinforcement_learning.environment._
import org.clulab.reach.focusedreading.reinforcement_learning.Actions

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
  * Created by enrique on 26/03/17.
  */
class SARSA(environmentFabric:() => Option[Environment], episodeBound:Int, burnInEpisodes:Int, alpha:Double = 0.01, gamma:Double = 0.8) extends LazyLogging {

  val actionLog = new ListBuffer[Actions.Value]
  var stable = true
  var episodeCount = 0
  var controlCount = 0

  val alphaDecrease = alpha/episodeBound
  val alphas = (0 to episodeBound).toStream.map(i => alpha-(i*alphaDecrease)).iterator
  var changes:List[Boolean] = Nil
  var observedRewards = mutable.ArrayBuffer[mutable.ArrayBuffer[Double]]()
  var currentlyIteratedPolicy:Option[EpGreedyPolicy] = None

  def getGreedyPolicy:Option[GreedyPolicy] =  currentlyIteratedPolicy  match {
    case Some(policy) => Some(policy.makeGreedy)
    case None => None
  }


  def iteratePolicy(policy:EpGreedyPolicy):Policy = {

    // Initialize the policy we will learn online
    //val policy = new EpGreedyPolicy(.05)

    currentlyIteratedPolicy = Some(policy)
    var episode = environmentFabric()

    do {
      stable = true

      episode match {
        case Some(environment) =>

          val localRewards = new mutable.ArrayBuffer[Double]()

          val currentAlpha = alphas.next

          // Observe the initial state
          var currentState = environment.observeState

          // Evaluate the policy
          var currentAction = policy.selectAction(currentState)

          // Enter into the episode loop
          while(!environment.finishedEpisode){
            // Execute chosen action and observe reward
            val reward = environment.executePolicy(currentAction)
            localRewards += reward

            // Observe the new state after executing the action
            val nextState = environment.observeState

            // Chose a new action
            val nextAction = policy.selectAction(nextState)


            // Perform the update
            val actionValues = policy.values
            val changed = actionValues.tdUpdate((currentState, currentAction), (nextState, nextAction), reward, currentAlpha, gamma)
            actionValues match {
              case v:ProxyValues =>
                val loss = v.loss
                loss match {
                  case Some(l) =>
                    logger.info(s"Loss: $l")
                  case None => ()
                }
              case _ => ()
            }

            changes = changed::changes


            // Keep track of the fluctuations of the values
            if(changes.size < 10 || changes.take(10).contains(true))
              stable = false
            else {
              val x = 1
            }


            // Update the state and action
            currentState = nextState
            currentAction = nextAction


            controlCount += 1
          }

          episodeCount += 1
          observedRewards += localRewards

          if(episodeCount % 10 == 0)
            logger.info(s"Episode $episodeCount")

          if (episodeCount % 10 == 0 && episodeCount > 1){
            policy.save(Some(s"model_${episodeCount}.pt"), s"partial_policy_${episodeCount}.json")
          }

        case None => Unit
      }

      episode = environmentFabric()


    }while(episode != None && (!stable || episodeCount <= burnInEpisodes) && episodeCount <= episodeBound)

    if(stable)
      logger.info(s"Converged on $episodeCount episodes")
    else
      logger.info(s"Didn't converge")

    policy
  }

}