package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object RefExtractor {

//  def unaply(): Option[] = ???


  def isFragment(ref: String): Boolean = ref.trim.startsWith("#")


}
