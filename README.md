Type safe Java and Scala RAML client generator
==============================================

This is a Java and Scala client library generator based on RAML definition files. This version supports DSL code generation with automated JSON to case class transformations. Enable code generation with scraml in your project with the scraml-sbt-plugin. Have a look at the scraml-test-scala project for an example of how to do it (look at plugin.sbt and build.sbt). The Java version is currently under development and a maven plugin will soon be available. 

The code contains the following modules:

   * *scraml-parser*: This is a simple Scala wrapper around the java-raml-generator. The resulting Raml model is expressed in Scala case classes without 'null' references.
   * *scraml-jsonschema-parser*: This module parses json-schema files into a high-level lookup table to simplify the code generation step.  
   * *scraml-dsl-java*: This is the Java code that supports the generated DSL and will be included in the resulting project by the scraml-maven-plugin (under development).
   * *scraml-dsl-scala*: This is the Scala code that supports the generated DSL and will be included in the resulting project by the scraml-sbt-plugin.
   * *scraml-generator*: The generator will generate a Scala DSL from a given RAML specification. 

See the scraml SBT plugin project to enable scraml in your Scala project: https://github.com/atomicbits/scraml-sbt-plugin

The Java DSL generator is currently under development.    

More detailed documentation is coming soon... 

