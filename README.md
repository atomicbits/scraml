Type safe Java and Scala RAML client generator
==============================================

[![Build Status](https://travis-ci.org/atomicbits/scraml.svg?branch=develop)](https://travis-ci.org/atomicbits/scraml)


Scraml generates a typesafe Java or Scala client library based on a [RAML](http://raml.org) specification. It transforms JSON schema into fully typed Java POJOs or Scala case classes and a REST resources DSL that enforces your RAML specification

Enable code generation with scraml in your project with the [scraml-maven-plugin](https://github.com/atomicbits/scraml-maven-plugin) (Java) or the [scraml-sbt-plugin](https://github.com/atomicbits/scraml-sbt-plugin) (Scala). Have a look at the [scraml-test-java](https://github.com/atomicbits/scraml-test-java) and [scraml-test-scala](https://github.com/atomicbits/scraml-test-scala) projects respectively for an example of how to empower scraml in you projects. 

The scraml code contains the following modules:

   * *scraml-parser*: This is a simple Scala wrapper around the java-raml-generator. The resulting Raml model is expressed in Scala case classes without 'null' references.
   * *scraml-jsonschema-parser*: This module parses json-schema files into a high-level lookup table to simplify the code generation step.  
   * *scraml-dsl-java*: This is the Java code that supports the generated DSL and will be included in the resulting project by the scraml-maven-plugin.
   * *scraml-dsl-scala*: This is the Scala code that supports the generated DSL and will be included in the resulting project by the scraml-sbt-plugin.
   * *scraml-generator*: The generator will generate the actual DSL code from a given RAML specification. 



