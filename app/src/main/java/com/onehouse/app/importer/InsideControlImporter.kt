package com.onehouse.app.importer

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.io.IOException
import java.nio.charset.Charset
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object InsideControlImporter {
    private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val INSIDE_CONTROL_KEY = "mkt5iscdtadr2012"
    private val ZERO_IV = ByteArray(16)

    sealed interface Result {
        data class Success(val project: ImportedKnxProject) : Result
        data class Failure(val message: String, val cause: Throwable? = null) : Result
    }

    fun import(contentResolver: ContentResolver, uri: Uri): Result {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
                ?: return Result.Failure("No se pudo abrir el archivo seleccionado")

            import(bytes)
        } catch (error: SecurityException) {
            Result.Failure("OneHouse no tiene permiso para leer el archivo", error)
        } catch (error: IOException) {
            Result.Failure("No se pudo leer el archivo: ${error.message.orEmpty()}", error)
        } catch (error: Exception) {
            Result.Failure(error.message ?: "No se pudo abrir el proyecto", error)
        }
    }

    fun import(fileBytes: ByteArray): Result {
        return try {
            if (fileBytes.isEmpty()) {
                return Result.Failure("El archivo está vacío")
            }

            val encryptedText = fileBytes.toString(Charsets.UTF_8)
                .replace("\r", "")
                .replace("\n", "")
                .trim()

            if (encryptedText.isBlank()) {
                return Result.Failure("El archivo está vacío")
            }

            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            if (encryptedBytes.isEmpty()) {
                return Result.Failure("El archivo no contiene datos cifrados")
            }

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(INSIDE_CONTROL_KEY.toByteArray(Charsets.US_ASCII), "AES"),
                    IvParameterSpec(ZERO_IV)
                )
            }

            val plainBytes = cipher.doFinal(encryptedBytes)
            val decodedText = decodeInsideControlText(plainBytes)
            val project = InsideControlParser.parse(decodedText)
            Result.Success(project)
        } catch (error: IllegalArgumentException) {
            Result.Failure(
                error.message ?: "El archivo no tiene el formato de InsideControl Builder",
                error
            )
        } catch (error: BadPaddingException) {
            Result.Failure(
                "No se pudo descifrar el proyecto. Puede pertenecer a otra versión de InsideControl",
                error
            )
        } catch (error: IllegalBlockSizeException) {
            Result.Failure("El contenido cifrado está incompleto o dañado", error)
        } catch (error: Exception) {
            Result.Failure(error.message ?: "No se pudo importar el proyecto", error)
        }
    }

    private fun decodeInsideControlText(bytes: ByteArray): String {
        val candidates = listOf(
            Charsets.UTF_8,
            Charset.forName("windows-1252"),
            Charsets.ISO_8859_1
        )

        return candidates
            .asSequence()
            .map { charset -> bytes.toString(charset) }
            .firstOrNull { text ->
                text.contains("{BUILDER_VERSION=") && text.contains("{ROOMS=")
            }
            ?: bytes.toString(Charsets.UTF_8)
    }
}
