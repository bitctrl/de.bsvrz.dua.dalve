[![Build Status](https://travis-ci.org/bitctrl/de.bsvrz.dua.dalve.svg?branch=develop)](https://travis-ci.org/bitctrl/de.bsvrz.dua.dalve)
[![Build Status](https://api.bintray.com/packages/bitctrl/maven/de.bsvrz.dua.dalve/images/download.svg)](https://bintray.com/bitctrl/maven/de.bsvrz.dua.dalve)

# Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE

Version: ${version}

## Übersicht

Die SWE Datenaufbereitung LVE dient der Aufbereitung von messwertersetzten Kurzzeitdaten.

Sie ermittelt folgende Kenngrößen:

  - Analysewerte (je Fahrstreifen und je Messquerschnitt), siehe [AFo] S. 115f.
  - Prognosewerte (je Fahrstreifen und je Messquerschnitt), siehe [AFo] S. 134f.

Weiterhin wird hier die lokale Störfallerkennung bzw. die Ermittlung der Verkehrslagen wie folgt
durchgeführt:

  - Ermittlung spurbezogener Verkehrslagen (Störfallzustände), siehe [Afo] S. 151f.
  - Nach Verfahren I (MARZ)
  - Nach Verfahren II (NRW Verkehrslage)
  - Ermittlung querschnittsorientierter Verkehrslagen (Störfallzustände), siehe [Afo] S. 154f.
  - Nach Verfahren I (MARZ)
  - Nach Verfahren II (NRW Verkehrslage)
  - Nach Verfahren III (NRW Verkehrslage für RDS-Meldungen)
  - Nach Verfahren IV (Fundamentaldiagramm)

Sämtliche hier produzierten Daten werden unter den vorgesehenen Standardaspekten publiziert.

## Versionsgeschichte

### Version 2.0.3

- Applikationsname für MessageSender entsprechend NERZ-Vorgabe gesetzt

### Version 2.0.2

Release-Datum: 28.07.2016

de.bsvrz.dua.dalve.analyse.AtgVerkehrsDatenKurzZeitAnalyseFs	
de.bsvrz.dua.dalve.analyse.AtgVerkehrsDatenKurzZeitAnalyseMq

- die Klassen erweitern nicht mehr de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer
- equals und hashCode können nicht adäquat überschrieben werden, weil die Klasse änderbare Daten 
  repräsentiert und gleichzeitig als ClientDavReceiver in einer Map als Key eingesetzt wird

de.bsvrz.dua.dalve.stoerfall.fd4.ParameterFuerStoerfall
- die Klasse erweitern nicht mehr de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer
- equals und hashCode können nicht adäquat überschrieben werden, weil die Klasse änderbare Daten 
  repräsentiert. Die einzige von der ursprünglichen Basisklasse bereitgestellte Funktion "equals"
  wird entsprechend Code-Analyse nicht genutzt

de.bsvrz.dua.dalve.analyse.DaAnalyseMessQuerschnitt#berechneVDifferenz(Data)
- null-Prüfung zu spät potentieller NPE

de.bsvrz.dua.dalve.prognose.PrognoseAttributParameter
- equals und hashCode werden nicht überschrieben

de.bsvrz.dua.dalve.tests.DaLveTestBase
- der Member "_datenaufbereitungLVE" sollte nicht statisch sein, der er bei jedem Test neu initialisiert wird

- Javadoc für Java8-Kompatibilität korrigiert
- Obsolete SVN-Tags aus Kommentaren entfernt
- Obsolete inheritDoc-Kommentare entfernt

### Version 2.0.1

Release-Datum: 22.07.2016

- Umpacketierung gemäß NERZ-Konvention

### Version 2.0.0

Release-Datum: 31.05.2016

#### Neue Abhängigkeiten

Die SWE benötigt nun das Distributionspaket de.bsvrz.sys.funclib.bitctrl.dua in
Mindestversion 1.5.0 und de.bsvrz.sys.funclib.bitctrl in Mindestversion 1.4.0.

#### Änderungen

Folgende Änderungen gegenüber vorhergehenden Versionen wurden durchgeführt:

- Die SWE setzt keine Betriebsmeldungen mehr ab.
- Die Güte-Berechnungen wurden überarbeitet.
- Die Berechnung von virtuellen Messquerschnitten nach Verfahren Verkehrslage
  wurde komplett überarbeitet. Ist kein Geschwindigkeitsfahrstreifen angegeben,
  wird nun das Verfahren Allgemein angewendet, statt den erstbesten Fahrstreifen
  als Geschwindigkeitsfahrstreifen zu verwenden.
- Die Berechnung von virtuellen Messquerschnitten nach Verfahren Allgemein wurde
  ergänzt.
- Die Berechnung von virtuellen Messquerschnitten nach Verfahren Standard wurde
  überarbeitet und entspricht jetzt den Anwenderforderungen.
- Die Berechnungen der geglätteten Werte und der Prognosewerte wurde überarbeitet.
  Die SWE verwendet jetzt bei der internen Zwischenspeicherung immer gerundete
  Werte (statt Fließkommawerte).
- Überarbeitung der Berechnung der Störfallindikatoren:
   – Die Dichte wird in den Verfahren MARZ (I), NRW (II), RDS (III) jetzt
     entsprechend AFo berechnet.

#### Fehlerkorrekturen

Folgende Fehler gegenüber vorhergehenden Versionen wurden korrigiert:

- Der Versand von Störfall- und Prognosedaten verwendet jetzt keine Sendesteuerung
  mehr, da mit der alten Vorgehensweise potentielle Datenabnehmer unter bestimmten
  Umständen veraltete Daten übermittelt bekamen.
- Die SWE reagiert jetzt toleranter auf verschiedene Konfigurationsprobleme und
  gibt in dem Fall eine Warnung aus, statt abzustürzen (z.B. wenn bei einem virtuellen
  Messquerschnitt die Attributgruppe zur Berechnung der Werte fehlt).
- Die Erfassungsintervalle der Fahrstreifen eines Messquerschnitts wurden nicht immer
  korrekt zugeordnet bzw. verglichen.

### Version 1.7.0

- Umstellung auf Java 8 und UTF-8

### Version 1.6.1

- Kompatibilität zu DuA-2.0 hergestellt

### Version 1.6.0

- Umstellung auf Funclib-BitCtrl-Dua

### Version 1.5.0

- Umstellung auf Maven-Build

### Version 1.4.0

  - Für die Berechnung von VKfz am Messquerschnitt wird wenn VKFz eines Fahrstreifens nicht
    ermittelbar ist, jetzt die Durchschnittsgeschwindigkeit aller anderen Fahrstreifen zur
    Berechnung herangezogen. Bisher wurde wenn VKfz eines Fahrstreifen nicht ermittelbar war,    die
    QKfz dieses Fahrstreifen mit in die Berechnung einbezogen, so das für den Mittelwert der
    Durchschnittsgeschwindigkeit mehr Fahrzeuge bekannt waren, als deren Geschwindigkeit. Das hat
    die Durchschnittsgeschwindigkeit unverhältnismäßig nach unten gedrückt.

### Version 1.3.1

  - mit dem Kommandozeilenparameter "-ignoreDichteMax=true" kann die Beschränkung der Dichtewerte
    auf den parametrierten Maximalwert abgeschaltet werden

### Version 1.3.0

  - FIX: Beim Einsatz der DatenaufbereitungLVE ist uns aufgefallen, dass wenn virtuelle
    Messquerschnitte auf Basis der atg.messQuerschnittVirtuellStandard berechnet werden sollen,
    teilweise falsch gerechnet wird.

### Version 1.2.2

  - FIX: Beim Einsatz der DatenaufbereitungLVE ist uns aufgefallen, dass wenn virtuelle
    Messquerschnitte auf Basis der atg.messQuerschnittVirtuellStandard berechnet werden sollen, die
    Applikation sich mit einem Fehler beendet.

### Version 1.2.1

  - FIX: Sämtliche Konstruktoren DataDescription(atg, asp, sim) ersetzt durch
     DataDescription(atg, asp)

### Version 1.2.0

  - Berechnung von Stoerfallzustand nach Verfahren MARZ korrigiert (Verwendung von kKfzG statt kBG)

### Version 1.1.0

  - Berechnungsverfahren zur Berechnung der virtuellen MQs ergaenzt (jetzt zusaetzlich auf Basis von
    atg.messQuerschnittVirtuellVLage)
  - Stoerfallindikator VKDiffKfz ergaenzt
  - Uebernahme des Fundamentaldiagramms bei entsprechendem Stoerfallindikator auf
    Strassenteilsegment geaendert

### Version 1.0.0

  - Erste vollständige Auslieferung

### Version 1.0.0b

  - Erste Auslieferung (beta, nur teilweise nach Prüfspezifikation getestet)


## Disclaimer

Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
Copyright (C) 2007 BitCtrl Systems GmbH

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 51
Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.


## Kontakt

BitCtrl Systems GmbH
Weißenfelser Straße 67
04229 Leipzig
Phone: +49 341-490670
mailto: info@bitctrl.de
