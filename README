Scala RAML client generator
===========================

This is Spike code to start build a Scala client library generator based on RAML definition files. This project
needs at lease Scala 2.11.

The code contains the following modules:

   * scraml-parser: This is a simple Scala wrapper around the java-raml-generator. The resulting Raml model is expressed in Scala case classes without 'null' references.
   * scraml-generator: The generator will generate a Scala DSL from a given RAML specification. It uses the paradise compiler plugin to enable macro annotations.
   * scraml-testdef: The test definition project defines a macro annotation that expands into a DSL at compile time.
   * scraml-test: This test project tests the result of the code that is generated in the testdef project. It only
   has a binary dependency on scaml-testdef. The reason why scraml-testdef and scraml-test are defined in separate
   modules is that if you use the DSL in the same module that defines the macro, you will have no highlighting
   support on the DSL in your IDE (be it Intellij or Eclipse) because it doesn't see the code that hasn't been
   generated yet.

