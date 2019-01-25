package com.variant.proj.bench

import java.util.concurrent.Executors

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.util.Success
import scala.util.Failure
import java.util.concurrent.TimeoutException
import ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import com.variant.proj.aws.SQS
import com.variant.client.VariantClient
import com.variant.client.test.SessionIdTrackerHeadless
import com.variant.client.test.TargetingTrackerHeadless
import com.variant.client.Session
import com.variant.client.Connection
import com.variant.core.util.StringUtils

object Main extends App {

	val AWS_SQS_BENCHMARK_URL = "https://sqs.us-east-2.amazonaws.com/071311804336/benchmark"
	
	val OP_CREATE_SESSION = "Create Session"
	val OP_TARGET_SESSION = "Target Session"
	val OP_COMMIT_REQUEST = "Commit State Request"
	
	val rand = new java.util.Random(System.currentTimeMillis)

	// Externalize these
	val maxWaitSecs = 5   // Max time to block for futures at each step
  val stepsToRun = 10   // How many steps to run? Each step is given 1 sec.
  val parallelism = 1   // Degree of parallelism at each step.
  
  val log = LoggerFactory.getLogger(getClass)
  
  val results = new Results()
	
  log.debug("Available processors: " + Runtime.getRuntime.availableProcessors())
  
  // Connect to the benchmark schema.
  val variant = new VariantClient.Builder()
			.withSessionIdTrackerClass(classOf[SessionIdTrackerHeadless])
			.withTargetingTrackerClass(classOf[TargetingTrackerHeadless])
			.build();    
	
	// The steps array must contain as many sequences as the degree of
	// parallelism. The sizes of sequences can and should differ.
	// Steps are pipelined, i.e. result of previous steps becmes the input of the next.
	// Each step takes one param (the result of the previous step) and returns a double,
	// whose first elem is the result of this step and the second elem is the name of
	// this step's operation under which its elapsed time will be stored in the results table.
	val steps: Array[Array[AnyRef => (AnyRef, String)]] =  
		Array(
				Array(
					ref => {
						log.debug("Getting session")
						val conn = ref.asInstanceOf[Connection]
						val ssn = conn.getOrCreateSession(StringUtils.random128BitString(rand))
						(ssn, OP_CREATE_SESSION)
					},
					ref => {
						log.debug("Targeting session for state1")
						val ssn = ref.asInstanceOf[Session]
						val state1 = ssn.getSchema.getState("state1").get
						ssn.targetForState(state1)
						(ssn, OP_TARGET_SESSION)
					},
					ref => {
						log.debug("Committing request for state1")
						val ssn = ref.asInstanceOf[Session]
						val state1 = ssn.getSchema.getState("state1").get
						ssn.getStateRequest.get.commit()
						(ssn, OP_COMMIT_REQUEST)
					},
					ref => {
						log.debug("Targeting session for state2")
						val ssn = ref.asInstanceOf[Session]
						val state2 = ssn.getSchema.getState("state2").get
						ssn.targetForState(state2)
						(ssn, OP_TARGET_SESSION)
					},
					ref => {
						log.debug("Committing request for state2")
						val ssn = ref.asInstanceOf[Session]
						val state1 = ssn.getSchema.getState("state1").get
						ssn.getStateRequest.get.commit()
						(ssn, OP_COMMIT_REQUEST)
					},
				)
		)
	
	val conn = variant.connectTo("variant://localhost:5377/benchmark")

	log.debug("Connected to Variant schema" + conn.getSchemaName);

	// Steps are initialized with the connection
	val batons = new Array[AnyRef](parallelism)
	for (i <- 0 until batons.length) batons(i) = conn
	
	// Futures array
  val futures = new Array[Future[Unit]](parallelism)
  	
  // Do the steps
	for (step <- 0 until stepsToRun) {

    //val start = System.currentTimeMillis
    
    for (i <- 0 until parallelism) {
      futures(i) = Future {
      	blocking {
      		
      		val nextStepIx = step % steps(i).length 
	        
	        // Do the step
      		val start = System.currentTimeMillis
	        val (baton, op) = steps(i)(nextStepIx)(batons(i))
	        val elapsed = System.currentTimeMillis - start
	        
	        // If step took too long, cancel everything. 
	        if (elapsed > maxWaitSecs* 1000) {
      		// Cancel the run...
	        	throw new RuntimeException("Method took too long")
	        }

      		//  If we're circling back to step 0, re-initialize the baton to connection.
      		batons(i) = if ((nextStepIx + 1) == steps(i).length) conn else baton

      		results.add(op, elapsed.toInt)
      	}
      }
    }
  
    // Wait for all concurrent calls to complete before proceeding to the next step.
    val start = System.currentTimeMillis
    futures.foreach { f =>
    	try {

    		Await.ready(f, maxWaitSecs seconds)
    		
				val elapsedMillis = System.currentTimeMillis() - start

				f.value.get match {
      		case Success(_) => 
      			//println("**** Success after " + elapsedMillis + " millis")
  	    	
      		case Failure(t) => 
      			log.error("Failure in future", t)
      			// cancel run!
   	  	}
    	}
    	catch {
    		case tex: TimeoutException => cancelRun
    	}
    	
    }
      
  }

  results.print  			

  /**
   * 
   */
  def cancelRun {
  	SQS.enqueue(AWS_SQS_BENCHMARK_URL, "CANCEL")
  }
}