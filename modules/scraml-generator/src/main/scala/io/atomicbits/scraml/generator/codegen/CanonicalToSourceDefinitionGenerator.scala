/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
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
  * source definitions isn't always equal to the number of canonical types, because multiple inheritance may produce additional
  * source definitions.
  */
object CanonicalToSourceDefinitionGenerator {

  def transferObjectsToClassDefinitions(generationAggr: GenerationAggr,
                                        canonicalToMap: Map[CanonicalName, NonPrimitiveType]): GenerationAggr = {

    // ToDo: see if the following assertion is true for all languages, for now we put this logic in the sourcecode generators.
    // We assume here that all our target languages support inheritance, but no multiple inheritance and that
    // all languages have a way to express interface definitions in order to 'simulate' the effect, to some extend,
    // of multiple inheritance.

    // Conclusion so far:
    // * SourceDefinitions are generated independent of the target language
    // * interface definitions are not generated now, its the target language's responsibility to generate them when necessary while
    //   generating the source codes (adding those interface definitions to the GenerationAggr for later source code generation).

    def objectTypeToTransferObjectClassDefinition(objectType: ObjectType): TransferObjectClassDefinition = {

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

      val toClassReference =
        ClassReference(
          name           = objectType.canonicalName.name,
          packageParts   = objectType.canonicalName.packagePath,
          typeParameters = objectType.typeParameters.map(tp => TypeParameter(tp.name))
        )

      val jsonTypeInfoOpt =
        for {
          typeDiscrimintator <- objectType.typeDiscriminator
          typeDiscrimintatorValue <- objectType.typeDiscriminatorValue
        } yield JsonTypeInfo(discriminator = typeDiscrimintator, discriminatorValue = typeDiscrimintatorValue)

      TransferObjectClassDefinition(
        reference    = toClassReference,
        fields       = objectType.properties.map(propertyToField).toList,
        parents      = objectType.parents.map(Platform.typeReferenceToClassPointer(_)),
        jsonTypeInfo = jsonTypeInfoOpt
      )
    }

    def enumTypeToEnumDefinition(enumType: EnumType): EnumDefinition = {

      val enumClassReference =
        ClassReference(
          name         = enumType.canonicalName.name,
          packageParts = enumType.canonicalName.packagePath
        )

      EnumDefinition(
        reference = enumClassReference,
        values    = enumType.choices
      )
    }

    def unionTypeToUnionClassDefinition(unionType: UnionType): UnionClassDefinition = {

      val unionClassReference =
        ClassReference(
          name         = unionType.canonicalName.name,
          packageParts = unionType.canonicalName.packagePath
        )

      UnionClassDefinition(
        reference = unionClassReference,
        union     = unionType.types.map(Platform.typeReferenceToClassPointer(_))
      )
    }

    canonicalToMap.values.foldLeft(generationAggr) { (genAggr, theType) =>
      val sourceDefinition =
        theType match {
          case objectType: ObjectType => objectTypeToTransferObjectClassDefinition(objectType)
          case enumType: EnumType     => enumTypeToEnumDefinition(enumType)
          case unionType: UnionType   => unionTypeToUnionClassDefinition(unionType)
          case unexpected             => sys.error(s"Unexpected type seen during TO definition generation: $unexpected")
        }
      genAggr.addSourceDefinition(sourceDefinition)
    }
  }

}
