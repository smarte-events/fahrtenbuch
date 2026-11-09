// Planer der Web-App: Maps-Link und Streckenabschnitte – gleiche Erwartungen wie PlanerTest.kt in der Android-App.
import assert from "node:assert/strict";
import { test } from "node:test";
import { abschnitte, mapsLink } from "../public/app/planer.js";

test("Link mit Start, Zwischenziel und Ziel", () => {
  assert.equal(
    mapsLink("Hauptplatz 1, 8720 Knittelfeld", ["Bahnhofstraße 2, Judenburg", "  "], "Wien Stephansplatz"),
    "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=Wien+Stephansplatz" +
      "&origin=Hauptplatz+1%2C+8720+Knittelfeld&waypoints=Bahnhofstra%C3%9Fe+2%2C+Judenburg",
  );
});

test("mehrere Zwischenziele mit senkrechtem Strich", () => {
  assert.equal(mapsLink("", ["A", "B"], "Z"), "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=Z&waypoints=A%7CB");
});

test("ohne Start beginnt Maps am Standort, ohne Ziel gibt es keinen Link", () => {
  assert.ok(!mapsLink("", [], "Graz").includes("origin="));
  assert.equal(mapsLink("Graz", [], "  "), null);
});

test("Abschnitte", () => {
  assert.deepEqual(abschnitte("", ["A", ""], "Z"), [[null, "A"], ["A", "Z"]]);
  assert.deepEqual(abschnitte("S", [], "Z"), [["S", "Z"]]);
});
