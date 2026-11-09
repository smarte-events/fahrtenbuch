package at.zweibit.fahrtenbuch.ui.planer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.zweibit.fahrtenbuch.FahrtenbuchApp
import at.zweibit.fahrtenbuch.planer.Planer
import at.zweibit.fahrtenbuch.sync.Kartendienst
import at.zweibit.fahrtenbuch.tracking.Adressen
import at.zweibit.fahrtenbuch.ui.AdressFeld
import at.zweibit.fahrtenbuch.ui.HauptViewModel
import at.zweibit.fahrtenbuch.ui.rememberStandortEinschalten
import at.zweibit.fahrtenbuch.util.Format
import kotlinx.coroutines.launch

private fun erlaubt(context: Context, recht: String) =
    ContextCompat.checkSelfPermission(context, recht) == PackageManager.PERMISSION_GRANTED

private fun mapsOeffnen(context: Context, link: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Google Maps konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show()
    }
}

/**
 * Route planen: Strecke und Fahrzeit schätzen, in Google Maps navigieren und optional gleich aufzeichnen.
 * Die normale Aufzeichnung („Fahrt starten“) braucht keine Planung.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanenScreen(vm: HauptViewModel, zurueck: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FahrtenbuchApp
    val orte by vm.orte.collectAsStateWithLifecycle()
    val laufend by vm.laufend.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var von by remember { mutableStateOf("") }
    var nach by remember { mutableStateOf("") }
    val ueber = remember { mutableStateListOf<String>() }
    var schaetzung by remember { mutableStateOf<String?>(null) }
    var rechnet by remember { mutableStateOf(false) }
    var hinweis by remember { mutableStateOf<String?>(null) }
    val standortEinschalten = rememberStandortEinschalten()

    val link = Planer.mapsLink(von, ueber.toList(), nach)

    val rechte = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    fun aufzeichnenUndMaps() {
        hinweis = null
        vm.starten(von.trim().ifBlank { null })
        standortEinschalten()
        link?.let { mapsOeffnen(context, it) }
    }

    val anfrage = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (erlaubt(context, Manifest.permission.ACCESS_FINE_LOCATION)) aufzeichnenUndMaps()
        else hinweis = "Ohne genaue Standortfreigabe kann keine Fahrt aufgezeichnet werden."
    }

    fun berechnen() {
        rechnet = true
        schaetzung = null
        scope.launch {
            try {
                var meter = 0.0
                var sekunden = 0L
                val abschnitte = Planer.abschnitte(von, ueber.toList(), nach)
                if (abschnitte.any { it.first == null }) {
                    schaetzung = "Für die Schätzung bitte einen Start eingeben."
                    return@launch
                }
                fun koordinaten(text: String) =
                    orte.firstOrNull { it.hatKoordinaten && it.alsAdresse.equals(text, ignoreCase = true) }
                        ?.let { it.lat!! to it.lon!! }
                for ((a, b) in abschnitte) {
                    val p = koordinaten(a!!) ?: Adressen.koordinaten(app, a)
                    val q = koordinaten(b) ?: Adressen.koordinaten(app, b)
                    if (p == null || q == null) {
                        schaetzung = "Adresse nicht gefunden: ${if (p == null) a else b}. Bitte mit Postleitzahl und Ort eingeben."
                        return@launch
                    }
                    val r = Kartendienst.route(app, p.first, p.second, q.first, q.second)
                    val route = r.getOrElse {
                        schaetzung = it.message ?: "Keine Route verfügbar."
                        return@launch
                    }
                    meter += route.meter
                    sekunden += route.sekunden
                }
                val min = (sekunden + 30) / 60
                val dauer = if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
                schaetzung = "ca. ${Format.km(meter)} · ca. $dauer (ohne Verkehr)"
            } finally {
                rechnet = false
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Route planen") },
                navigationIcon = {
                    IconButton(onClick = zurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
            )
        },
    ) { innen ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innen)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdressFeld(von, { von = it; schaetzung = null }, "Von (leer = aktueller Standort)", orte)
            ueber.forEachIndexed { i, z ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdressFeld(z, { ueber[i] = it; schaetzung = null }, "Über (Zwischenziel ${i + 1})", orte, Modifier.weight(1f))
                    IconButton(onClick = { ueber.removeAt(i); schaetzung = null }) { Icon(Icons.Filled.Close, "Zwischenziel entfernen") }
                }
            }
            TextButton(onClick = { ueber.add("") }) {
                Icon(Icons.Filled.Add, null)
                Text("Zwischenziel hinzufügen")
            }
            AdressFeld(nach, { nach = it; schaetzung = null }, "Nach", orte)

            OutlinedButton(onClick = ::berechnen, enabled = nach.isNotBlank() && !rechnet, modifier = Modifier.fillMaxWidth()) {
                Text(if (rechnet) "Wird berechnet …" else "Strecke und Fahrzeit schätzen")
            }
            schaetzung?.let { Text(it, fontWeight = FontWeight.Medium) }

            Button(
                onClick = { link?.let { mapsOeffnen(context, it) } },
                enabled = link != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("In Google Maps öffnen") }

            if (laufend == null) {
                OutlinedButton(
                    onClick = { if (rechte.any { !erlaubt(context, it) }) anfrage.launch(rechte) else aufzeichnenUndMaps() },
                    enabled = link != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Fahrt aufzeichnen und Google Maps öffnen") }
            } else {
                Text("Eine Fahrt wird gerade aufgezeichnet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            hinweis?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Die Navigation übernimmt Google Maps. Das Fahrtenbuch zeichnet die tatsächlich gefahrene Strecke per GPS " +
                    "auf – auch ohne Planung über „Fahrt starten“.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
