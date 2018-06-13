/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{
  CanonicalName,
  EnumType,
  GenericReferrable,
  NonPrimitiveType,
  ObjectType,
  Property,
  TypeReference,
  UnionType,
  TypeParameter => CanonicalTypeParameter
}

/**
  * Created by peter on 14/01/17.
  *
  * Transforms a map containing canonical RAML or json-schema types to a sequence of source definitions. The number of
  * source definitions isn't always equal to the number of canonical types, because (multiple) inheritance may produce additional
  * source definitions.
  */
object CanonicalToSourceDefinitionGenerator {

  def transferObjectsToClassDefinitions(generationAggr: GenerationAggr): GenerationAggr = {

    // ToDo: see if the following assertion is true for all languages, for now we put this logic in the sourcecode generators.
    // We assume here that all our target languages support inheritance, but no multiple inheritance and that
    // all languages have a way to express interface definitions in order to 'simulate' the effect, to some extend,
    // of multiple inheritance.

    // Conclusion so far:
    // * SourceDefinitions are generated independent of the target language
    // * interface definitions are not generated now, its the target language's responsibility to generate them when necessary while
    //   generating the source codes (adding those interface definitions to the GenerationAggr for later source code generation).

    def objectTypeToTransferObjectClassDefinition(genAggr: GenerationAggr, objectType: ObjectType): GenerationAggr = {

      def propertyToField(propertyDef: (String, Property[_ <: GenericReferrable])): Field = {
        val (name, property) = propertyDef

        val fieldClassPointer =
          property.ttype match {
            case CanonicalTypeParameter(tParamName) => TypeParameter(tParamName)
            case typeReference: TypeReference       => Platform.typeReferenceToClassPointer(typeReference)
          }

        Field(
          fieldName    = name,
          classPointer = fieldClassPointer,
          required     = property.required
        )
      }

      val canonicalName = objectType.canonicalName

      val toClassReference =
        ClassReference(
          name           = canonicalName.name,
          packageParts   = canonicalName.packagePath,
          typeParameters = objectType.typeParameters.map(tp => TypeParameter(tp.name))
        )

      val transferObjectClassDefinition =
        TransferObjectClassDefinition(
          reference              = toClassReference,
          fields                 = objectType.properties.map(propertyToField).toList,
          parents                = objectType.parents.flatMap(Platform.typeReferenceToClassReference),
          typeDiscriminator      = objectType.typeDiscriminator,
          typeDiscriminatorValue = objectType.typeDiscriminatorValue
        )

      genAggr
        .addToDefinition(canonicalName, transferObjectClassDefinition)
        .addSourceDefinition(transferObjectClassDefinition)
    }

    def enumTypeToEnumDefinition(genAggr: GenerationAggr, enumType: EnumType): GenerationAggr = {

      val enumClassReference =
        ClassReference(
          name         = enumType.canonicalName.name,
          packageParts = enumType.canonicalName.packagePath
        )

      val enumDefinition =
        EnumDefinition(
          reference = enumClassReference,
          values    = enumType.choices
        )

      genAggr.addSourceDefinition(enumDefinition)
    }

    def unionTypeToUnionClassDefinition(genAggr: GenerationAggr, unionType: UnionType): GenerationAggr = {

      val unionClassReference =
        ClassReference(
          name         = unionType.canonicalName.name,
          packageParts = unionType.canonicalName.packagePath
        )

      val unionClassDefinition =
        UnionClassDefinition(
          reference = unionClassReference,
          union     = unionType.types.map(Platform.typeReferenceToClassPointer)
        )

      genAggr.addSourceDefinition(unionClassDefinition)
    }

    generationAggr.canonicalToMap.values.foldLeft(generationAggr) { (genAggr, nonPrimitiveType) =>
      nonPrimitiveType match {
        case objectType: ObjectType => objectTypeToTransferObjectClassDefinition(genAggr, objectType)
        case enumType: EnumType     => enumTypeToEnumDefinition(genAggr, enumType)
        case unionType: UnionType   => unionTypeToUnionClassDefinition(genAggr, unionType)
        case unexpected             => sys.error(s"Unexpected type seen during TO definition generation: $unexpected")
      }
    }
  }

}
