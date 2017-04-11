/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
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

import sun.misc.BASE64Decoder

import scala.util.Try

/**
  * Created by peter on 29/06/16.
  */
object LicenseVerifier {

  val charset     = "UTF-8"
  val datePattern = "yyyy-MM-dd"

  val algorithm = "RSA"
  // Encryption algorithm
  val signatureAlgorithm = "SHA1WithRSA"

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
    val cleanKey = cleanLicenseKey(licenseKey)
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

  private def decodeLicense(licenseKey: String): Option[LicenseData] = {
    val encodedKey     = licenseKey.trim
    val signedKeyBytes = new BASE64Decoder().decodeBuffer(encodedKey)
    val signedKey      = new String(signedKeyBytes, charset)
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
      println(s"Invalid license key. Falling back to default AGPL license.")
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
    signer.verify(new BASE64Decoder().decodeBuffer(signature))
  }

  lazy val publicKey: PublicKey = {
    val b64: BASE64Decoder       = new BASE64Decoder
    val decoded: Array[Byte]     = b64.decodeBuffer(publicKeyPEM)
    val spec: X509EncodedKeySpec = new X509EncodedKeySpec(decoded)
    val kf: KeyFactory           = KeyFactory.getInstance(algorithm)
    kf.generatePublic(spec)
  }

  private def addValidatedLicense(licenseKey: String, licenseData: Option[LicenseData]): Unit = {
    LicenseVerifier.synchronized {
      validatedLicenses += licenseKey -> licenseData
    }
  }

  private def cleanLicenseKey(licenseKey: String): String = {
    licenseKey.split('\n').map(line => line.trim).mkString("\n")
  }

}
