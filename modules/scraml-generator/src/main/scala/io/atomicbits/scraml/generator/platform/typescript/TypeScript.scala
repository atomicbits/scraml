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

package io.atomicbits.scraml.generator.platform.typescript

import java.nio.file.{ Path, Paths }

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._
import Platform._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 15/12/17.
  */
case class TypeScript() extends Platform with CleanNameTools {

  val apiBasePackageParts: List[String] = List.empty

  val name: String = "TypeScript"

  implicit val platform: TypeScript = this

  override def dslBasePackageParts = List.empty

  override def rewrittenDslBasePackage = List.empty

  override def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference = {
    classPointer match {
      case classReference: ClassReference => classReference
      case ArrayClassPointer(arrayType) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = List(arrayType)
        ClassReference(name = "Array", typeParameters = List(typeParameter), typeParamValues = typeParamValues, predef = true)
      case StringClassPointer =>
        ClassReference(name = "string", packageParts = List(), predef = true)
      case ByteClassPointer =>
        ClassReference(name = "Byte", packageParts = List(), predef = true)
        ??? // not implemented
      case BinaryDataClassPointer =>
        ClassReference(name = "BinaryData", packageParts = List(), library = true) // Uint8Array ?
        ??? // not implemented
      case FileClassPointer =>
        ClassReference(name = "File", packageParts = List(), library = true)
        ??? // not implemented
      case InputStreamClassPointer =>
        ClassReference(name = "InputStream", packageParts = List(), library = true)
        ??? // not implemented
      case JsObjectClassPointer =>
        ClassReference(name = "any", packageParts = List(), library = true)
      case JsValueClassPointer =>
        ClassReference(name = "JsValue", packageParts = List(), library = true)
        ??? // not implemented
      case BodyPartClassPointer =>
        ClassReference(name = "BodyPart", packageParts = List(), library = true)
        ??? // not implemented
      case LongClassPointer(primitive) =>
        ClassReference(name = "number", packageParts = List(), predef = true)
      case DoubleClassPointer(primitive) =>
        ClassReference(name = "number", packageParts = List(), predef = true)
      case BooleanClassPointer(primitive) =>
        ClassReference(name = "boolean", packageParts = List(), predef = true)
      case DateTimeRFC3339ClassPointer => // See: http://blog.stevenlevithan.com/archives/date-time-format
        // ClassReference(name = "DateTimeRFC3339", packageParts = List(), library = true)
        ClassReference(name = "string", packageParts = List(), predef = true)
      case DateTimeRFC2616ClassPointer =>
        // ClassReference(name = "DateTimeRFC2616", packageParts = List(), library = true)
        ClassReference(name = "string", packageParts = List(), predef = true)
      case DateTimeOnlyClassPointer =>
        // ClassReference(name = "DateTimeOnly", packageParts = List(), library = true)
        ClassReference(name = "string", packageParts = List(), predef = true)
      case TimeOnlyClassPointer =>
        // ClassReference(name = "TimeOnly", packageParts = List(), library = true)
        ClassReference(name = "string", packageParts = List(), predef = true)
      case DateOnlyClassPointer =>
        // ClassReference(name = "DateOnly", packageParts = List(), library = true)
        ClassReference(name = "string", packageParts = List(), predef = true)
      case ListClassPointer(typeParamValue) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = List(typeParamValue)
        ClassReference(name = "Array", typeParameters = List(typeParameter), typeParamValues = typeParamValues, predef = true)
      case typeParameter: TypeParameter =>
        ClassReference(name = typeParameter.name, predef = true, isTypeParameter = true)
      case _: io.atomicbits.scraml.generator.typemodel.PrimitiveClassPointer => ???
    }
  }

  /**
    * The implementing interface reference is the reference to the class (transfer object class) that implements the
    * interface that replaces it in a multiple inheritance relation (and in Scala also in a regular inheritance relation).
    * E.g. AnimalImpl implements Animal --> Here, 'Animal' is the interface where resources and cross referencing inside TO's point to.
    */
  override def implementingInterfaceReference(classReference: ClassReference) = ??? // not implemented

  /**
    * The definition of a class.
    * E.g. List[String] or List<String> or Element or PagedList<T> or PagedList[Element]
    * or their fully qualified verions
    * E.g. List[String] or java.util.List<String> or io.atomicbits.Element or io.atomicbits.PagedList<T> or io.atomicbits.PagedList[Element]
    */
  override def classDefinition(classPointer: ClassPointer, fullyQualified: Boolean) = {
    val classReference = classPointer.native

    val typedClassDefinition =
      (classReference.typeParameters, classReference.typeParamValues) match {
        case (Nil, _) => classReference.name
        case (tps, Nil) =>
          val typeParameterNames = tps.map(_.name)
          s"${classReference.name}<${typeParameterNames.mkString(",")}>"
        case (tps, tpvs) if tps.size == tpvs.size =>
          val typeParameterValueClassDefinitions =
            tpvs.map { classPointer =>
              if (fullyQualified) classPointer.native.fullyQualifiedClassDefinition
              else classPointer.native.classDefinition
            }
          s"${classReference.name}<${typeParameterValueClassDefinitions.mkString(",")}>"
        case (tps, tpvs) =>
          val message =
            s"""
               |The following class definition has a different number of type parameter
               |values than there are type parameters:
               |$classPointer
             """.stripMargin
          sys.error(message)
      }

    val arrayedClassDefinition =
      if (classReference.isArray) s"$typedClassDefinition[]"
      else typedClassDefinition

    if (fullyQualified) {
      val parts = safePackageParts(classPointer) :+ arrayedClassDefinition
      parts.mkString(".")
    } else {
      arrayedClassDefinition
    }
  }

  override def className(classPointer: ClassPointer): String = classPointer.native.name

  override def packageName(classPointer: ClassPointer): String = ""

  override def fullyQualifiedName(classPointer: ClassPointer) = className(classPointer)

  override def safePackageParts(classPointer: ClassPointer): List[String] = List()

  override def safeFieldName(fieldName: String): String = {
    // In typescript, all field names are allowed, even reserved words.
    // If the field name contains special characters such as spaces, '-', '~', ..., then we need to quote it.
    // Just to be sure to handle all special characters, we quote all fields.
    quoteString(fieldName)
  }

  override def fieldDeclarationWithDefaultValue(field: Field) = fieldDeclaration(field)

  override def fieldDeclaration(field: Field) =
    if (field.required) s"${safeFieldName(field)}: ${classDefinition(field.classPointer)}"
    else s"${safeFieldName(field)}?: ${classDefinition(field.classPointer)}"

  override def importStatements(targetClassReference: ClassPointer, dependencies: Set[ClassPointer]): Set[String] = Set()

  override def toSourceFile(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition) =
    ToClassGenerator(this).generate(generationAggr, toClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition) =
    InterfaceGenerator(this).generate(generationAggr, toInterfaceDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, enumDefinition: EnumDefinition) =
    EnumGenerator(this).generate(generationAggr, enumDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, unionClassDefinition: UnionClassDefinition) =
    UnionClassGenerator(this).generate(generationAggr, unionClassDefinition)

  override def classFileExtension = "d.ts"

  /**
    * Transforms a given class reference to a file path. The given class reference already has clean package and class names.
    *
    * @param classPointer The class reference for which a file path is generated.
    * @return The relative file name for the given class.
    */
  override def toFilePath(classPointer: ClassPointer): Path = {
    classPointer match {
      case classReference: ClassReference =>
        Paths.get("", s"${classReference.name}.$classFileExtension") // This results in a relative path both on Windows as on Linux/Mac
      case _ => sys.error(s"Cannot create a file path from a class pointer that is not a class reference!")
    }
  }

  override def mapSourceFiles(sources: Set[SourceFile], combinedSourcesFileName: Option[String] = None): Set[SourceFile] = {
    combinedSourcesFileName.map { combinedName =>
      val allContent = sources.toList.sortBy(_.filePath).map(_.content)
      Set(
        SourceFile(
          filePath = Paths.get(combinedName), // We will fill in the actual filePath later.
          content  = allContent.mkString("\n")
        )
      )
    } getOrElse (sources)
  }

  val reservedKeywords =
    Set(
      "break",
      "case",
      "catch",
      "class",
      "const",
      "continue",
      "debugger",
      "default",
      "delete",
      "do",
      "else",
      "enum",
      "export",
      "extends",
      "false",
      "finally",
      "for",
      "function",
      "if",
      "import",
      "in",
      "instanceof",
      "new",
      "null",
      "return",
      "super",
      "switch",
      "this",
      "throw",
      "true",
      "try",
      "typeof",
      "var",
      "void",
      "while",
      "with",
      "as",
      "implements",
      "interface",
      "let",
      "package",
      "private",
      "protected",
      "public",
      "static",
      "yield",
      "any",
      "boolean",
      "constructor",
      "declare",
      "get",
      "module",
      "require",
      "number",
      "set",
      "string",
      "symbol",
      "type",
      "from",
      "of"
    ) ++
      Set(
        "otherFields"
      ) // These are the fields we have reserved by scraml.

  def escapeTypeScriptKeyword(someName: String, escape: String = "$"): String =
    reservedKeywords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"$name$escape"
      else name
    }

}
