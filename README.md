Type safe Java and Scala RAML client generator
==============================================

[![Build Status](https://travis-ci.org/atomicbits/scraml.svg?branch=develop)](https://travis-ci.org/atomicbits/scraml)


[Scraml](http://scraml.io) generates a typesafe Java or Scala client library based on a [RAML](http://raml.org) specification. It transforms 
JSON schema into fully typed Java POJOs or Scala case classes and a REST resources DSL that enforces your RAML specification. We also support
Typescript TO generation and HTML documentation generation. Plugins are available for maven, gradle and sbt.  

## Quickstart

Follow the steps in these quickstart guidelines: 

 1. [Java Quickstart](https://github.com/atomicbits/scraml/blob/develop/documentation/javadocumentation.adoc#quickstart-java)
 2. [Scala Quickstart](https://github.com/atomicbits/scraml/blob/develop/documentation/scaladocumentation.adoc#quickstart-scala)

## Release notes

[Read here about the most recent releases](https://github.com/atomicbits/scraml/blob/develop/documentation/release-notes.adoc) 


Enable code generation with scraml in your project with the [scraml-maven-plugin](https://github.com/atomicbits/scraml-maven-plugin), 
the [scraml-gradle-plugin](https://github.com/atomicbits/scraml-gradle-plugin) 
or the [scraml-sbt-plugin](https://github.com/atomicbits/scraml-sbt-plugin). Have a look at 
the [scraml-test-java](https://github.com/atomicbits/scraml-test-java), the [scraml-test-java-gradle](https://github.com/atomicbits/scraml-test-java-gradle) 
and the [scraml-test-scala](https://github.com/atomicbits/scraml-test-scala) projects respectively for an example of how to enable scraml in you projects. 


## Copyright and License
Copyright 2018 Atomic BITS bvba. Code released under the Apache 2.0 License. 
