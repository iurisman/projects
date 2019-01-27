package com.variant.proj.bench

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

/**
 * Accumulate measurement results in memory.
 * Results are accumulated in a concurrent list of int results,  keyed by the op name in a concurrent map. 
 * Op queues do not have to be pre-allocated: first insert into an operation creates an empty queue.
 * Clearly, this is a race condition, which we choose to tolerate as it's not likely to have a great
 * influence on the averages.
 */
class Results() {
	
	val map = new TrieMap[String, ConcurrentLinkedQueue[Int]]()
	
	/**
	 * Add a result of a given operation
	 */
	def add(op: String, result: Int) {
		
		map.get(op) match {
			case Some(queue) => 
				queue.add(result)
			case None =>
				val queue = new ConcurrentLinkedQueue[Int]()
				map.put(op,queue)
				queue.add(result)
		}
	}
	
	/**
	 * Compute the results.
	 * Each element in the returned sequence is a double, whose first element
	 * is the name of the method and the second is a 5-element Int array with the interquartile measurements. Note that we ignore the largest
	 * number as it tends to be way off.
	 */
	def compute: Seq[(String, Array[Int])] = {
			val result = new ArrayBuffer[(String, Array[Int])]()
			
			for ( (op, queue) <- map) {
				val measures = queue.asScala.toArray
				Sorting.quickSort(measures)
				result.append(
						(op, 
						Array(
								measures(0), 
								measures(measures.length / 4), 
								measures(measures.length / 2), 
								measures(measures.length * 3 / 4),
								// Ignore the largest number, as it tends to be misleadingly large.
								measures(measures.length - 2))))
			}
			
			result
	}
	
}