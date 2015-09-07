Scala RAML client generator
===========================

This is a Scala client library generator based on RAML definition files. This version supports DSL code generation with automated JSON to case class transformations. Enable code generation with scraml in your project with the scraml-sbt-plugin. Have a look at the scraml-test-scala project for an example of how to do it (look at plugin.sbt and build.sbt). 

The code contains the following modules:

   * *scraml-parser*: This is a simple Scala wrapper around the java-raml-generator. The resulting Raml model is expressed in Scala case classes without 'null' references.
   * *scraml-jsonschema-parser*: This module parses json-schema files into a high-level lookup table to simplify the code generation step.  
   * *scraml-dsl*: This is the code that supports the generated DSL and will be included in the resulting project by the scraml-sbt-plugin.
   * *scraml-generator*: The generator will generate a Scala DSL from a given RAML specification. 

See the scraml SBT plugin project to enable scraml in your Scala project: https://github.com/atomicbits/scraml-sbt-plugin

A Java DSL generator is on our backlog.  

More detailed documentation is coming soon... 

