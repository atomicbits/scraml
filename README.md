Scala RAML client generator
===========================

This is a Scala client library generator based on RAML definition files. This early version already supports 
 DSL code generation with automated JSON to case class transformations. This project needs at least Scala 2.11.

The code contains the following modules:

   * *scraml-parser*: This is a simple Scala wrapper around the java-raml-generator. The resulting Raml model is expressed in Scala case classes without 'null' references.
   * *scraml-generator*: The generator will generate a Scala DSL from a given RAML specification. It uses the paradise compiler plugin to enable macro annotations.
   * *scraml-jsonschema-parser*: This module parses json-schema files into a high-level lookup table to simplify the code generation step.  
   
If you want to see this Scraml generation in action, checkout the template 
project: https://github.com/atomicbits/scraml-template 
