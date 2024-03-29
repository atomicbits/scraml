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

package io.atomicbits.scraml.generator.license

import java.security.{ KeyFactory, PublicKey, Signature }
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Created by peter on 29/06/16.
  */
object LicenseVerifier {

  val charset     = "UTF-8"
  val datePattern = "yyyy-MM-dd"

  val algorithm = "RSA"
  // Encryption algorithm
  val signatureAlgorithm = "SHA1WithRSA"

  val freeLicenseStatementMandatoryPart =
    "use the free scraml license in this project without any intend to serve commercial purposes for ourselves or anyone else"
  val freeLicenseStatementOfIntend = s"<We/I>, <enter name here>, $freeLicenseStatementMandatoryPart."

  val publicKeyPEM =
    """
      |MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7AhsLTLtb8uzz8ammZd3PScF/nyfevcM
      |1tNSk8O9gQbwPszMrOAR9cxODm4owkhg4GU2ADlOSqGvTZJxKHqoUWkp5DazN3D/tex+88I58v/9
      |8M61TJRGKrBV0q+TCudLswcDnLkw0WQpJUE4RgYKJoi4j1K9+R6L1c5/B6cdgaQrIurt52A9Jffj
      |aJ4+tlSyNsSmR3w320+KYj5uG+NKSZcghIwzbIYSZkSS+hKfQIEPbAUY0hFWIL0PuEuEuRXxnJXQ
      |6+tuQ04hfkJn9NVBFZPOIY+nw7nmzZnstbJsnCPW47Ple2Fyv6VV/jZlRyuE3P2VAn5p+8KPc2uH
      |5SnYWQIDAQAB
    """.stripMargin.trim

  @volatile
  private var validatedLicenses: Map[String, Option[LicenseData]] = Map.empty

  def validateLicense(licenseKey: String): Option[LicenseData] = {
    val cleanKey = cleanCommercialLicenseKey(licenseKey)
    // Make sure decodeAndRegister is only executed if the key is not yet contained in validatedLicenses!
    // Do not simply the expression below with validatedLicenses.getOrElse(licenseKey, decodeAndRegister(licenseKey)) !
    validatedLicenses.get(cleanKey) match {
      case Some(optionalLicenseData) => optionalLicenseData.flatMap(checkExpiryDate)
      case None                      => decodeVerifyAndRegister(cleanKey)
    }
  }

  private def decodeVerifyAndRegister(licenseKey: String): Option[LicenseData] = {
    val decoded = decodeLicense(licenseKey).flatMap(checkExpiryDate)
    addValidatedLicense(licenseKey, decoded)
    decoded
  }

  private def decodeLicense(licenseKey: String): Option[LicenseData] =
    decodeFreeLicense(licenseKey).orElse(decodeCommercialLicense(licenseKey))

  private def decodeFreeLicense(licenseKey: String): Option[LicenseData] = {

    val cleanKey = cleanFreeLicenseKey(licenseKey)

    if (cleanKey.toLowerCase.contains(freeLicenseStatementMandatoryPart.toLowerCase)) {

      val userName =
        getFreeLicenseKeyUser(cleanKey)
          .getOrElse(
            sys.error(
              s"Please state the free license intend as:\n$freeLicenseStatementOfIntend\n and fill in your name between the commas."
            )
          )

      val licenseData =
        LicenseData(
          customerId   = "-1",
          licenseId    = "-1",
          licenseType  = "Free",
          owner        = userName,
          period       = -1,
          purchaseDate = LocalDate.now()
        )

      println(s"Free Scraml license used by ${licenseData.owner}.")

      Some(licenseData)
    } else {
      None
    }
  }

  private def decodeCommercialLicense(licenseKey: String): Option[LicenseData] = {
    val encodedKey = licenseKey.trim
    val signedKeyBytes =
      Try(Base64.getDecoder().decode(encodedKey)).getOrElse(sys.error(s"Cannot verify license key with bad key format."))
    val signedKey = new String(signedKeyBytes, charset)
    val (unsignedKey, signature) = signedKey.split('!').toList match {
      case uKey :: sig :: _ => (uKey, sig)
      case _                => sys.error(s"Cannot verify key with bad key format: $signedKey")
    }
    if (verifySignature(unsignedKey, signature)) {
      unsignedKey.split(';').toList match {
        case List(licenseType, customerId, licenseId, owner, purchaseDateString, periodString) =>
          val licenseData =
            LicenseData(
              customerId  = customerId,
              licenseId   = licenseId,
              owner       = owner,
              licenseType = licenseType,
              period      = Try(periodString.toLong).getOrElse(0),
              purchaseDate =
                Try(LocalDate.parse(purchaseDateString, DateTimeFormatter.ofPattern(datePattern))).getOrElse(LocalDate.ofYearDay(1950, 1))
            )
          println(s"Scraml license $licenseId is licensed to $owner.")
          Some(licenseData)
        case x =>
          println(s"Invalid license key. Falling back to default AGPL license.")
          None
      }
    } else {
      println(s"Invalid license key. Falling back to default AGPL license.\n$licenseKey")
      None
    }
  }

  private def checkExpiryDate(licenseKey: LicenseData): Option[LicenseData] = {
    val today       = LocalDate.now()
    val expiryDate  = licenseKey.purchaseDate.plusDays(licenseKey.period)
    val warningDate = licenseKey.purchaseDate.plusDays(licenseKey.period - 31)

    if (licenseKey.period < 0) {
      // Key with unlimited period.
      Some(licenseKey)
    } else if (today.isAfter(expiryDate)) {
      println(
        s"""
           |
           |Warning: your scraml license key expired on ${expiryDate.format(DateTimeFormatter.ISO_DATE)}.
           |
           |We will now fall back to the default AGPL license!
           |
           |""".stripMargin
      )
      None
    } else if (today.isAfter(warningDate)) {
      println(
        s"""
           |
           |Warning: your scraml license key expires on ${expiryDate.format(DateTimeFormatter.ISO_DATE)}.
           |
           |""".stripMargin
      )
      Some(licenseKey)
    } else {
      Some(licenseKey)
    }
  }

  private def verifySignature(text: String, signature: String): Boolean = {
    val textBytes: Array[Byte] = text.getBytes("UTF8")
    val signer: Signature      = Signature.getInstance(signatureAlgorithm)
    signer.initVerify(publicKey)
    signer.update(textBytes)
    Try(signer.verify(Base64.getDecoder.decode(signature))).recover {
      case NonFatal(exc) => false
    }.get
  }

  lazy val publicKey: PublicKey = {
    val decoded: Array[Byte]     = Base64.getDecoder.decode(publicKeyPEM)
    val spec: X509EncodedKeySpec = new X509EncodedKeySpec(decoded)
    val kf: KeyFactory           = KeyFactory.getInstance(algorithm)
    kf.generatePublic(spec)
  }

  private def addValidatedLicense(licenseKey: String, licenseData: Option[LicenseData]): Unit = {
    LicenseVerifier.synchronized {
      validatedLicenses += licenseKey -> licenseData
    }
  }

  private def cleanCommercialLicenseKey(licenseKey: String): String = {
    licenseKey.split('\n').map(line => line.trim).mkString("\n")
  }

  /**
    * replace all (successive) whitespace with a single space character
    *
    * see: https://stackoverflow.com/questions/5455794/removing-whitespace-from-strings-in-java
    *
    */
  private def cleanFreeLicenseKey(licenseKey: String): String = licenseKey.replaceAll("\\s+", " ")

  private def getFreeLicenseKeyUser(licenseKey: String): Option[String] = {
    licenseKey.split(',').toList match {
      case l1 :: name :: ln => Some(name.trim)
      case _                => None
    }
  }

}
