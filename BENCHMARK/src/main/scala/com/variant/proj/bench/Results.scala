package com.variant.proj.bench

import scala.collection.concurrent.TrieMap
import java.util.concurrent.ConcurrentLinkedQueue

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
	 * Print all the results.
	 */
	def print {
		for ( (op, queue) <- map) {
			println(s"Results for [${op}]")
			println("  " + queue)
		}
	}
}