package com.organisator.print3d.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Recopie les photos choisies dans le stockage privé de l'application.
 *
 * Le sélecteur système ne donne qu'une autorisation de lecture temporaire sur
 * l'URI d'origine : la conserver telle quelle rendrait la photo illisible après
 * un redémarrage. L'image est donc redimensionnée puis réécrite en JPEG dans
 * `files/project_photos`, où elle survit aux mises à jour et suit la sauvegarde
 * de l'application.
 */
class PhotoStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir: File
        get() = File(appContext.filesDir, DIRECTORY).apply { if (!exists()) mkdirs() }

    /** Retourne le chemin du fichier créé, ou null si l'image n'a pas pu être lue. */
    fun import(uri: Uri): String? = runCatching {
        val bitmap = decodeScaled(uri) ?: return null
        val oriented = applyExifRotation(uri, bitmap)
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }
        if (oriented !== bitmap) bitmap.recycle()
        oriented.recycle()
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        val file = path?.let { File(it) } ?: return
        if (file.parentFile?.name == DIRECTORY) file.delete()
    }

    /**
     * Supprime les photos qu'aucun projet ne référence : un import abandonné
     * avant l'enregistrement laisserait sinon un fichier derrière lui.
     */
    fun removeOrphans(referenced: Collection<String>) {
        val keep = referenced.toHashSet()
        dir.listFiles()?.forEach { file ->
            if (file.absolutePath !in keep) file.delete()
        }
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        // Première passe : on ne lit que les dimensions. decodeStream rend toujours
        // null dans ce mode, c'est donc l'ouverture du flux, et elle seule, qui dit
        // si l'image est lisible.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = appContext.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val stream = appContext.contentResolver.openInputStream(uri) ?: return null
        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Puissance de deux la plus grande qui garde l'image au-dessus de [MAX_SIDE]. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= MAX_SIDE) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private companion object {
        const val DIRECTORY = "project_photos"
        const val MAX_SIDE = 1600
        const val QUALITY = 85
    }
}
