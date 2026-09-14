package com.organisator.print3d.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icônes dessinées à la main plutôt que tirées de `material-icons-extended` :
 * cette bibliothèque pèse plusieurs dizaines de mégaoctets pour quatre glyphes.
 */
object AppIcons {

    /** Pile de couches : le plateau d'impression. */
    val Plateau: ImageVector by lazy {
        icon("Plateau") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.5f); lineTo(21.5f, 7.5f); lineTo(12f, 12.5f); lineTo(2.5f, 7.5f); close()
            }
            path(fill = SolidColor(Color.Black), fillAlpha = 0.45f) {
                moveTo(4.6f, 11.2f); lineTo(12f, 15.1f); lineTo(19.4f, 11.2f)
                lineTo(21.5f, 12.3f); lineTo(12f, 17.3f); lineTo(2.5f, 12.3f); close()
            }
            path(fill = SolidColor(Color.Black), fillAlpha = 0.45f) {
                moveTo(4.6f, 16f); lineTo(12f, 19.9f); lineTo(19.4f, 16f)
                lineTo(21.5f, 17.1f); lineTo(12f, 22.1f); lineTo(2.5f, 17.1f); close()
            }
        }
    }

    /** Dossier : un projet. */
    val Projet: ImageVector by lazy {
        icon("Projet") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 5.5f)
                curveTo(3f, 4.7f, 3.7f, 4f, 4.5f, 4f)
                lineTo(9.3f, 4f)
                lineTo(11.3f, 6.2f)
                lineTo(19.5f, 6.2f)
                curveTo(20.3f, 6.2f, 21f, 6.9f, 21f, 7.7f)
                lineTo(21f, 18.5f)
                curveTo(21f, 19.3f, 20.3f, 20f, 19.5f, 20f)
                lineTo(4.5f, 20f)
                curveTo(3.7f, 20f, 3f, 19.3f, 3f, 18.5f)
                close()
            }
        }
    }

    /** Histogramme : les statistiques. */
    val Stats: ImageVector by lazy {
        icon("Stats") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.2f, 13f); lineTo(6.6f, 13f); lineTo(6.6f, 20.5f); lineTo(3.2f, 20.5f); close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.3f, 8f); lineTo(13.7f, 8f); lineTo(13.7f, 20.5f); lineTo(10.3f, 20.5f); close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.4f, 3.5f); lineTo(20.8f, 3.5f); lineTo(20.8f, 20.5f); lineTo(17.4f, 20.5f); close()
            }
        }
    }

    /** Horloge : une impression planifiée. */
    val Horloge: ImageVector by lazy {
        icon("Horloge") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.5f)
                curveTo(6.8f, 2.5f, 2.5f, 6.8f, 2.5f, 12f)
                curveTo(2.5f, 17.2f, 6.8f, 21.5f, 12f, 21.5f)
                curveTo(17.2f, 21.5f, 21.5f, 17.2f, 21.5f, 12f)
                curveTo(21.5f, 6.8f, 17.2f, 2.5f, 12f, 2.5f)
                close()
                moveTo(12f, 19.5f)
                curveTo(7.9f, 19.5f, 4.5f, 16.1f, 4.5f, 12f)
                curveTo(4.5f, 7.9f, 7.9f, 4.5f, 12f, 4.5f)
                curveTo(16.1f, 4.5f, 19.5f, 7.9f, 19.5f, 12f)
                curveTo(19.5f, 16.1f, 16.1f, 19.5f, 12f, 19.5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.1f, 6.8f); lineTo(12.9f, 6.8f); lineTo(12.9f, 12.4f)
                lineTo(16.8f, 14.7f); lineTo(15.9f, 16.2f); lineTo(11.1f, 13.4f); close()
            }
        }
    }

    private fun icon(
        name: String,
        content: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(content).build()
}
