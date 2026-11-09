// Routenplanung: Die Navigation übernimmt Google Maps, das Fahrtenbuch plant und zeichnet auf.
// Gleiche Logik wie Planer.kt in der Android-App (ohne Browser-Abhängigkeiten, daher testbar).

const MAPS = "https://www.google.com/maps/dir/";

const kodieren = (s) => encodeURIComponent(s.trim()).replace(/%20/g, "+");

/**
 * Link auf die Wegbeschreibung in Google Maps (offizielle Maps-URL, kein API-Schlüssel nötig).
 * Ohne `von` startet Maps am aktuellen Standort. Leere Zwischenziele werden ausgelassen.
 * @returns {string|null} null, wenn kein Ziel angegeben ist
 */
export function mapsLink(von, ueber, nach) {
  if (!nach || !nach.trim()) return null;
  const teile = ["api=1", "travelmode=driving", `destination=${kodieren(nach)}`];
  if (von && von.trim()) teile.push(`origin=${kodieren(von)}`);
  const halte = (ueber ?? []).filter((z) => z && z.trim());
  if (halte.length) teile.push("waypoints=" + halte.map(kodieren).join("%7C"));
  return `${MAPS}?${teile.join("&")}`;
}

/** Streckenabschnitte der Planung als [von, nach]; `von` leer = aktueller Standort (null). */
export function abschnitte(von, ueber, nach) {
  const punkte = [von && von.trim() ? von.trim() : null, ...(ueber ?? []).map((z) => z.trim()).filter(Boolean), nach.trim()];
  return punkte.slice(1).map((b, i) => [punkte[i], b]);
}
