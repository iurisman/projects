package com.variant.proj.bench

import java.util.concurrent.Executors

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

import org.slf4j.LoggerFactory

import com.variant.client.Connection
import com.variant.client.Session
import com.variant.client.VariantClient
import com.variant.client.test.SessionIdTrackerHeadless
import com.variant.client.test.TargetingTrackerHeadless
import com.variant.client.util.Timers
import com.variant.core.schema.State
import com.variant.core.util.StringUtils
import com.variant.proj.aws.SQS
import com.variant.proj.bench.JavaImplicits._

object Main extends App {

	val AWS_SQS_BENCHMARK_URL = "https://sqs.us-east-2.amazonaws.com/071311804336/benchmark"
	
	val OP_CREATE_SESSION = "Create Session"
	val OP_TARGET_SESSION = "Target Session"
	val OP_COMMIT_REQUEST = "Commit State Request"
	val OP_FAIL_REQUEST   = "Fail State Request"
	val OP_READ_ATTR      = "Read State Attribute"
	val OP_WRITE_ATTR     = "Write State Attribute"
	
	val rand = new java.util.Random(System.currentTimeMillis)

	val props = new java.util.Properties()
	props.load(getClass.getResourceAsStream("/benchmark.props"))
				
	// Externalize these
	val maxWaitMicros = 1000000 // If a step takes over a second, 
	                            //either parallelism is too high or the server isn't keeping up.
	val runLenSecs  = 30    // How many steps to run? Each step is given 1 sec.
	val parallelism = 2     // Degree of parallelism at each step.

	val log = LoggerFactory.getLogger(getClass)
    
 	// Connect to the benchmark schema.
	val variant = new VariantClient.Builder()
			.withSessionIdTrackerClass(classOf[SessionIdTrackerHeadless])
			.withTargetingTrackerClass(classOf[TargetingTrackerHeadless])
			.build();    
	
	// The steps array.  Steps are pipelined, i.e. result of previous steps becomes 
	// the input of the next.  Each step takes one param (the result of the previous step) 
	// and returns a tuple,  whose first elem is the result of this step and the second elem 
	// is the name of this step's operation under which its elapsed time will be stored in 
	// the results table.  Each thread executes the same sequences of steps in a loop.
	// There's not synchronization between threads. 
	val steps: Array[AnyRef => (AnyRef, String)] =  
		Array(
			ref => {
				// Get session
				val conn = ref.asInstanceOf[Connection]
				val ssn = conn.getOrCreateSession(StringUtils.random128BitString(rand))
				(ssn, OP_CREATE_SESSION)
			},
			ref => {
				// Target
				val ssn = ref.asInstanceOf[Session]
				ssn.targetForState(nextTargetableState(ssn))
				(ssn, OP_TARGET_SESSION)
			},
			ref => {
				// Commit request
				val ssn = ref.asInstanceOf[Session]
				ssn.getStateRequest.get.commit()
				(ssn, OP_COMMIT_REQUEST)
			},
			ref => {
				// Write ssn attribute
				val ssn = ref.asInstanceOf[Session]
				ssn.getAttributes.put("foo", "bar");
				ssn.getStateRequest.get.commit()
				(ssn, OP_WRITE_ATTR)
			},
			ref => {
				// Target
				val ssn = ref.asInstanceOf[Session]
				ssn.targetForState(nextTargetableState(ssn))
				(ssn, OP_TARGET_SESSION)
			},
				ref => {
				// Commit request
				val ssn = ref.asInstanceOf[Session]
				ssn.getStateRequest.get.commit()
				(ssn, OP_COMMIT_REQUEST)
			},
			ref => {
				// Read attribute
				val ssn = ref.asInstanceOf[Session]
				ssn.getAttributes.get("foo", "bar");
				ssn.getStateRequest.get.commit()
				(ssn, OP_READ_ATTR)
			},
			ref => {
				// Target
				val ssn = ref.asInstanceOf[Session]
				ssn.targetForState(nextTargetableState(ssn))
				(ssn, OP_TARGET_SESSION)
			},
				ref => {
				log.debug("Failing request for state3")
				val ssn = ref.asInstanceOf[Session]
				ssn.getStateRequest.get.fail()
				(ssn, OP_FAIL_REQUEST)
			},
		)
		
	var canceled = false;
	val conn = variant.connectTo("variant://localhost:5377/benchmark")

	log.debug("Connected to Variant schema" + conn.getSchemaName);

	// single threaded execution context
	val tpool = Executors.newFixedThreadPool(parallelism)
   	
	// Measurements are accumulated here.
	val results = new Measures()
	

 	// Kick off parallelism number of threads
	for (i <- 0 until parallelism) {
  	
		tpool.execute ( 
			new Runnable { 
			
				override def run {
					
					var baton: AnyRef = conn
		
			   	// Do the steps for the given period of time.
					// Both dims of the steps array are treated circularly, i.e. if parallelism is greater than
					// the length of the first dimension (outer array), the index wraps around. Likewise, if
					// the number of runs is greater than an inner array, the index wraps around and the caller
					// starts through the same steps all over.
					for (step <- 0 to Int.MaxValue if !Thread.currentThread.isInterrupted) {
		  	   	
						val stepIx = step % steps.length
		        
						try { 
							val (nextBaton: AnyRef, op: String) = steps(stepIx)(baton) 
							val elapsedLocal = Timers.localTimer.get.getAndClear
							val elapsedRemote = Timers.remoteTimer.get.getAndClear
		        
							// If step took too long, cancel everything.  Most likely we're expecting too much from the client. 
							if (elapsedLocal > maxWaitMicros) {
								throw new RuntimeException(s"Run canceled becaue local elapsed time [${elapsedLocal}] was too long")
							}
		
		      			//  If we're circling back to step 0, re-initialize the baton to connection.
			   			baton = if ((stepIx + 1) == steps.length) conn else nextBaton
		
			   			// Record results of each step.
		  		 			results.add(op, elapsedLocal.toInt, elapsedRemote.toInt)
						}
						catch {
							case t:Throwable => 
								log.error(s"Run canceled at step [${stepIx}] due to exception in Runnable [${t.getMessage}]", t)
								cancelRun
								
						}
					}
	      	}
	      }
		)
    }
	
	// 'parallelism' number of threads are running.  Block this thread for runLenSec seconds
	Thread.sleep(runLenSecs * 1000)
	
	// Interrupt the threads and compute results.
	tpool.shutdownNow()
	
	if (!canceled) writeMeasures(results)
	
	/**
	 * Compute all targetable states and pick one randomly.
	 */
	def nextTargetableState(ssn: Session): State = {
		
		// Build list of targetable states.
		val targetableStates = mutable.Set[State]()
		
		// Init with all the states.
		targetableStates ++= ssn.getSchema.getStates.asScala

		// Remove those that are not targetable.
		ssn.getLiveExperience("test2").foreach { exp =>
			if (exp.getName == "A") 
				targetableStates -= ssn.getSchema.getState("state2").get
			else if (exp.getName == "C") 
				targetableStates -= ssn.getSchema.getState("state4").get
		}
			
		ssn.getLiveExperience("test3").foreach { exp =>
			if (exp.getName == "C") 
				targetableStates -= ssn.getSchema.getState("state1").get
		}
		
		ssn.getLiveExperience("test4").foreach { exp =>
			if (exp.getName == "B") 
				targetableStates -= ssn.getSchema.getState("state4").get
		}

		ssn.getLiveExperience("test5").foreach { exp =>
			if (exp.getName == "B") 
				targetableStates -= ssn.getSchema.getState("state2").get
		}

		ssn.getLiveExperience("test6").foreach { exp =>
			if (exp.getName == "A") 
				targetableStates -= ssn.getSchema.getState("state5").get
		}

		targetableStates.toList(Random.nextInt(targetableStates.size))		
	}
	
	/**
	 * Compute and output the run's measures.
	 */
	def writeMeasures(measures: Measures) {
		
		val results = measures.compute;
		
		if (props.getProperty("environment").equals("local")) {
		
			println("***************************** RESULTS ******************************")
			println(s"Available processors: ${Runtime.getRuntime.availableProcessors}")
			println(s"Parallelism: ${parallelism}") 
			println(s"Run length, sec: ${runLenSecs}") 
			results.foreach { case (op, size, locals, remotes) =>
			  	println (s"Operation [${op}] (${size} measures)")
		  		println(locals.toList)
		  		println(remotes.toList)
			}
		}
		else {
			???
		}
	}

  /**
   * Cancel run. If running on AWS, signal death.
   */
	def cancelRun {
		
		canceled = true;
		Thread.currentThread().interrupt
		log.error("************************* RUN CANCELED *************************")
		
		if (props.getProperty("environment").equals("local")) { }
		else {
	  		SQS.enqueue(AWS_SQS_BENCHMARK_URL, "CANCEL")
		}
  	}
	
}