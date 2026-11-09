package at.zweibit.fahrtenbuch.planer

import java.net.URLEncoder

/**
 * Routenplanung: Die Route selbst berechnet Google Maps (Navigation), das Fahrtenbuch plant und zeichnet auf.
 * Hier liegt nur die Logik ohne Android-Abhängigkeiten.
 */
object Planer {
    private const val MAPS = "https://www.google.com/maps/dir/"

    private fun kodieren(s: String) = URLEncoder.encode(s.trim(), "UTF-8")

    /**
     * Link auf die Wegbeschreibung in Google Maps (offizielle Maps-URL, kein API-Schlüssel nötig).
     * Ohne [von] startet Maps am aktuellen Standort. Leere Zwischenziele werden ausgelassen.
     * @return null, wenn kein Ziel angegeben ist
     */
    fun mapsLink(von: String, ueber: List<String>, nach: String): String? {
        if (nach.isBlank()) return null
        val teile = mutableListOf("api=1", "travelmode=driving", "destination=${kodieren(nach)}")
        if (von.isNotBlank()) teile += "origin=${kodieren(von)}"
        val halte = ueber.filter { it.isNotBlank() }
        // Maps trennt Zwischenziele mit | (kodiert %7C)
        if (halte.isNotEmpty()) teile += "waypoints=" + halte.joinToString("%7C") { kodieren(it) }
        return "$MAPS?" + teile.joinToString("&")
    }

    /** Streckenabschnitte der Planung als Paare (von, nach); [von] leer = aktueller Standort (null). */
    fun abschnitte(von: String, ueber: List<String>, nach: String): List<Pair<String?, String>> {
        val punkte = listOf(von.trim().ifBlank { null }) + ueber.map { it.trim() }.filter { it.isNotEmpty() } + nach.trim()
        return punkte.zipWithNext().map { (a, b) -> a to b!! }
    }
}
