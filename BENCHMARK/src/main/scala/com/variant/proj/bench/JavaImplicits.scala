package com.variant.proj.bench

object JavaImplicits {
  
   implicit def оptional2Option[T](optional: java.util.Optional[T]): Option[T] = {
      Option(optional.orElse(null.asInstanceOf[T]))
   }
   
   implicit def оption2Optional[T](option: Option[T]): java.util.Optional[T] = {
      java.util.Optional.ofNullable(option.getOrElse(null.asInstanceOf[T]))
   }

}