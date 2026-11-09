package at.zweibit.fahrtenbuch

import at.zweibit.fahrtenbuch.planer.Planer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanerTest {
    @Test
    fun linkMitStartZwischenzielUndZiel() {
        val l = Planer.mapsLink("Hauptplatz 1, 8720 Knittelfeld", listOf("Bahnhofstraße 2, Judenburg", "  "), "Wien Stephansplatz")!!
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=Wien+Stephansplatz" +
                "&origin=Hauptplatz+1%2C+8720+Knittelfeld&waypoints=Bahnhofstra%C3%9Fe+2%2C+Judenburg",
            l,
        )
    }

    @Test
    fun mehrereZwischenzieleMitSenkrechtemStrich() {
        val l = Planer.mapsLink("", listOf("A", "B"), "Z")!!
        assertEquals("https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=Z&waypoints=A%7CB", l)
    }

    @Test
    fun ohneStartBeginntMapsAmStandort() {
        assertEquals(false, Planer.mapsLink("", emptyList(), "Graz")!!.contains("origin="))
    }

    @Test
    fun ohneZielKeinLink() {
        assertNull(Planer.mapsLink("Graz", emptyList(), "  "))
    }

    @Test
    fun abschnitte() {
        assertEquals(
            listOf<Pair<String?, String>>(null to "A", "A" to "Z"),
            Planer.abschnitte("", listOf("A", ""), "Z"),
        )
        assertEquals(listOf<Pair<String?, String>>("S" to "Z"), Planer.abschnitte("S", emptyList(), "Z"))
    }
}
