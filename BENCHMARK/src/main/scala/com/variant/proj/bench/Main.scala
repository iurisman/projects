package com.variant.proj.bench

import java.util.concurrent.Executors

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
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
import com.variant.proj.aws.AWS._
import com.variant.proj.aws.Dynamo
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import java.util.Calendar

object Main extends App {
	
	val OP_CREATE_SESSION = "Create Session"
	val OP_TARGET_SESSION = "Target Session"
	val OP_COMMIT_REQUEST = "Commit State Request"
	val OP_FAIL_REQUEST   = "Fail State Request"
	val OP_READ_ATTR      = "Read Session Attribute"
	val OP_WRITE_ATTR     = "Write Session Attribute"
	
	// SQS Client
	val sqs = SQS("https://sqs.us-east-2.amazonaws.com/071311804336/benchmark")

	// DynamoDB client (dunno why awsCreds isn't getting passed implicitly)
	val dynamo = new Dynamo(implicitly)

	val rand = new java.util.Random(System.currentTimeMillis)

	val props = new java.util.Properties()
	props.load(getClass.getResourceAsStream("/benchmark.props"))
				
	// Pure local time cannot exceed this
	val maxWaitMicros = props.getProperty("max.local.millis").toInt * 1000
	// How long to run?
	val runLenSecs = props.getProperty("run.duration.secs").toInt 
	// How many parallel threads to fire?
	val parallelism = props.getProperty("parallelism.factor").toInt * Runtime.getRuntime.availableProcessors

	val log = LoggerFactory.getLogger(getClass)
   	
	// All defaults are suitable for local execution. 
	// In AWS mode, override with -D properties on command line.
	val clientId = Option(System.getProperty("client.id")).getOrElse("igors mac");
	val runId = Option(System.getProperty("run.id")).getOrElse(s"$parallelism threads");
	val serverUrl = Option(System.getProperty("server.url")).getOrElse("variant://localhost:5377/benchmark");

	// Block until we get green flag from SQS
	val startupTimeoutSecs = props.getProperty("startup.timeout.secs").toInt
	if (sqs.dequeue(startupTimeoutSecs seconds).size == 0)
		cancelRun(s"Timed out after ${startupTimeoutSecs} seconds waiting for green flag") 
	
	val startTime = Calendar.getInstance
	
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
				// Write attribute
				val ssn = ref.asInstanceOf[Session]
				ssn.getAttributes.put("foo", "bar");
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
	val conn = variant.connectTo(serverUrl)

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
							val elapsedRemote = Timers.remoteTimer.get.getAndClear
							val elapsedLocal = Timers.localTimer.get.getAndClear - elapsedRemote
		        
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
									// We may have been interrupted mid-stop by the pool shutdown.
									if (!Thread.currentThread.isInterrupted) {
										val msg = s"Run canceled at step [${stepIx}] due to exception in Runnable [${t.getMessage}]"
										log.error(msg, t)
										cancelRun(msg)
									}
						}
					}
	      	}
	      }
		)
    }
	
	// 'parallelism' number of threads are running.  Block this thread for runLenSec seconds.
	val now = System.currentTimeMillis
	while (!canceled && (System.currentTimeMillis() - now) <= runLenSecs * 1000)
		Thread.sleep(50)
	
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
			println(s"Run duration, sec: ${runLenSecs}") 
			results.foreach { case (op, size, locals, remotes) =>
			  	println (s"Operation [${op}] (${size} measures)")
		  		println("Locals: " + locals.toList)
		  		println("Remotes: " + remotes.toList)
			}
		}
		else {
			val summaryRecord = Map(
					"key" -> s"$runId $clientId summary",
					"run_id" -> runId,
					"client_id" -> clientId,
					"rec_type" -> "summary",
					"processors" -> Runtime.getRuntime.availableProcessors,
					"parallelism" -> parallelism,
					"run_duration_sec" -> runLenSecs,
					"run_start" -> startTime.toString())
			
			dynamo.writeItem("benchmark", summaryRecord)
			
			results.foreach { case (op, size, locals, remotes) =>
				val opRecord =
					Map(
						"key" -> s"$runId $clientId $op",
						"run_id" -> runId,
						"client_id" -> clientId,
						"rec_type" -> op,					
						"op" -> op,
						"size" -> size,
						"local_0" -> locals(0),
						"local_1" -> locals(1),
						"local_2" -> locals(2),
						"local_3" -> locals(3),
						"local_4" -> locals(4),
						"remote_0" -> remotes(0),
						"remote_1" -> remotes(1),
						"remote_2" -> remotes(2),
						"remote_3" -> remotes(3),
						"remote_4" -> remotes(4),
				)
				dynamo.writeItem("benchmark", opRecord)	
			}				
		}
	}

  /**
   * Cancel run. If running on AWS, signal death.
   */
	def cancelRun(msg: String) {
		
		canceled = true;
		log.error("************************* RUN CANCELED *************************")
				val opRecord =
					Map(
						"key" -> s"$runId $clientId cancel",
						"run_id" -> runId,
						"client_id" -> clientId,
						"rec_type" -> "cancel",					
						"op" -> s"OPERATION CANCELED [$msg]"
				)
				dynamo.writeItem("benchmark", opRecord)	
		
		Thread.currentThread().interrupt

  	}
	
}