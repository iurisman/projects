package com.variant.proj.bench

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

/**
 * Accumulate measurement results in memory.
 * Results are accumulated in a concurrent list of 2-element int arrays,  keyed by the op name in a concurrent map.
 * The first elem contains local elapsed and the second elem contains remote elapsed. 
 * Op queues do not have to be pre-allocated: first insert into an operation creates an empty queue.
 * Clearly, this is a race condition, which we choose to tolerate as it's not likely to have a great
 * influence on the averages.
 */
class Measures() {
	
	val map = new TrieMap[String, ConcurrentLinkedQueue[Array[Int]]]()
	
	/**
	 * Add a result of a given operation
	 */
	def add(op: String, localElapsed: Int, remoteElapsed: Int) {
		map.get(op) match {
			case Some(list) => 
				list.add(Array(localElapsed, remoteElapsed))
			case None =>
				val list = new ConcurrentLinkedQueue[Array[Int]]()
				map.put(op, list)
				list.add(Array(localElapsed, remoteElapsed))
		}
	}
	
	/**
	 * Compute the results.
	 * Each element in the returned sequence is a tuple:
	 * • operation
	 * • number of executions.
	 * • 5-element local quartiles list
	 * • 5-element remote quartiles list
	 */
	def compute: Seq[(String, Int, Array[Int], Array[Int])] = {
			val result = new ArrayBuffer[(String, Int, Array[Int], Array[Int])]()
			
			for ( (op, list) <- map) {
				val localMeasures = list.asScala.map(_(0)).toArray
				val remoteMeasures = list.asScala.map(_(1)).toArray
				Sorting.quickSort(localMeasures)
				Sorting.quickSort(remoteMeasures)
				result.append(
						(
							op,
							localMeasures.size,
							Array(
								localMeasures(0), 
								localMeasures(localMeasures.length / 4), 
								localMeasures(localMeasures.length / 2), 
								localMeasures(localMeasures.length * 3 / 4),
								localMeasures(localMeasures.length - 1)
							),
							Array(
								remoteMeasures(0), 
								remoteMeasures(remoteMeasures.length / 4), 
								remoteMeasures(remoteMeasures.length / 2), 
								remoteMeasures(remoteMeasures.length * 3 / 4),
								remoteMeasures(remoteMeasures.length - 1)
							)
						)
				)		

			}
			
			result
	}
	
}