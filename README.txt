************************************************************************************
*  Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE  *
************************************************************************************

Version: ${version}

Übersicht
=========

Die SWE Datenaufbereitung LVE dient der Aufbereitung von messwertersetzten Kurzzeitdaten.

Sie ermittelt folgende Kenngrößen:
•
  - Analysewerte (je Fahrstreifen und je Messquerschnitt), siehe [AFo] S. 115f.
•  - Prognosewerte (je Fahrstreifen und je Messquerschnitt), siehe [AFo] S. 134f.

Weiterhin wird hier die lokale Störfallerkennung bzw. die Ermittlung der Verkehrslagen wie folgt
durchgeführt:
•
  - Ermittlung spurbezogener Verkehrslagen (Störfallzustände), siehe [Afo] S. 151f.
  - Nach Verfahren I (MARZ)
  - Nach Verfahren II (NRW Verkehrslage)
  - Ermittlung querschnittsorientierter Verkehrslagen (Störfallzustände), siehe [Afo] S. 154f.
  - Nach Verfahren I (MARZ)
  - Nach Verfahren II (NRW Verkehrslage)
  - Nach Verfahren III (NRW Verkehrslage für RDS-Meldungen)
  - Nach Verfahren IV (Fundamentaldiagramm)

Sämtliche hier produzierten Daten werden unter den vorgesehenen Standardaspekten publiziert.


Versionsgeschichte
==================

1.5.0

- Umstellung auf Maven-Build

1.4.0

  - Für die Berechnung von VKfz am Messquerschnitt wird wenn VKFz eines Fahrstreifens nicht
    ermittelbar ist, jetzt die Durchschnittsgeschwindigkeit aller anderen Fahrstreifen zur
    Berechnung herangezogen. Bisher wurde wenn VKfz eines Fahrstreifen nicht ermittelbar war, die
    QKfz dieses Fahrstreifen mit in die Berechnung einbezogen, so das für den Mittelwert der
    Durchschnittsgeschwindigkeit mehr Fahrzeuge bekannt waren, als deren Geschwindigkeit. Das hat
    die Durchschnittsgeschwindigkeit unverhältnismäßig nach unten gedrückt.

1.3.1

  - mit dem Kommandozeilenparameter "-ignoreDichteMax=true" kann die Beschränkung der Dichtewerte
    auf den parametrierten Maximalwert abgeschaltet werden

1.3.0

  - FIX: Beim Einsatz der DatenaufbereitungLVE ist uns aufgefallen, dass wenn virtuelle
    Messquerschnitte auf Basis der atg.messQuerschnittVirtuellStandard berechnet werden sollen,
    teilweise falsch gerechnet wird.

1.2.2

  - FIX: Beim Einsatz der DatenaufbereitungLVE ist uns aufgefallen, dass wenn virtuelle
    Messquerschnitte auf Basis der atg.messQuerschnittVirtuellStandard berechnet werden sollen, die
    Applikation sich mit einem Fehler beendet.

1.2.1

  - FIX: Sämtliche Konstruktoren DataDescription(atg, asp, sim) ersetzt durch
     DataDescription(atg, asp)

1.2.0

  - Berechnung von Stoerfallzustand nach Verfahren MARZ korrigiert (Verwendung von kKfzG statt kBG)

1.1.0

  - Berechnungsverfahren zur Berechnung der virtuellen MQs ergaenzt (jetzt zusaetzlich auf Basis von
    atg.messQuerschnittVirtuellVLage)
  - Stoerfallindikator VKDiffKfz ergaenzt
  - Uebernahme des Fundamentaldiagramms bei entsprechendem Stoerfallindikator auf
    Strassenteilsegment geaendert

1.0.0

  - Erste vollständige Auslieferung

1.0.0b

  - Erste Auslieferung (beta, nur teilweise nach Prüfspezifikation getestet)


Bemerkungen
===========

Diese SWE ist eine eigenständige Datenverteiler-Applikation, welche über die Klasse
de.bsvrz.dua.dalve.DatenaufbereitungLVE mit folgenden Parametern gestartet werden kann
(zusaetzlich zu den normalen Parametern jeder Datenverteiler-Applikation):
	-KonfigurationsBereichsPid=pid(,pid)


- Tests:

	Die automatischen Tests, die in Zusammenhang mit der Prüfspezifikation durchgeführt
	werden, sind noch nicht endgültig implementiert.
	Für die Tests wird eine Verbindung zum Datenverteiler mit einer Konfiguration mit dem
	Testkonfigurationsbereich "kb.duaTestObjekteSWE4.5" benötigt.
	Die Verbindung wird über die statische Variable CON_DATA der Klasse
	de.bsvrz.dua.dalve.DatenaufbereitungLVETest hergestellt.
	Die Testdaten befinden sich im Verzeichnis extra.

	/**
	 * Verbindungsdaten
	 */
	public static final String[] CON_DATA = new String[] {
			"-datenverteiler=localhost:8083", //$NON-NLS-1$
			"-benutzer=Tester", //$NON-NLS-1$
			"-authentifizierung=c:\\passwd"}; //$NON-NLS-1$

	Das Wurzelverzeichnis mit den Testdaten (csv-Dateien) muss ebenfalls innerhalb dieser Datei verlinkt sein:

	/**
	 * Wurzelverzeichnis der Testdaten
	 */
	public static final String TEST_DATEN_VERZ = "...extra\\"; //$NON-NLS-1$


- Logging-Hierarchie (Wann wird welche Art von Logging-Meldung produziert?):

	ERROR:
	- DUAInitialisierungsException --> Beendigung der Applikation
	- Fehler beim An- oder Abmelden von Daten beim Datenverteiler
	- Interne unerwartete Fehler

	WARNING:
	- Fehler, die die Funktionalität grundsätzlich nicht
	  beeinträchtigen, aber zum Datenverlust führen können
	- Nicht identifizierbare Konfigurationsbereiche
	- Probleme beim Explorieren von Attributpfaden
	  (von Plausibilisierungsbeschreibungen)
	- Wenn mehrere Objekte eines Typs vorliegen, von dem
	  nur eine Instanz erwartet wird
	- Wenn Parameter nicht korrekt ausgelesen werden konnten
	  bzw. nicht interpretierbar sind
	- Wenn inkompatible Parameter übergeben wurden
	- Wenn Parameter unvollständig sind
	- Wenn ein Wert bzw. Status nicht gesetzt werden konnte

	INFO:
	- Wenn neue Parameter empfangen wurden

	CONFIG:
	- Allgemeine Ausgaben, welche die Konfiguration betreffen
	- Benutzte Konfigurationsbereiche der Applikation bzw.
	  einzelner Funktionen innerhalb der Applikation
	- Benutzte Objekte für Parametersteuerung von Applikationen
	  (z.B. die Instanz der Datenflusssteuerung, die verwendet wird)
	- An- und Abmeldungen von Daten beim Datenverteiler

	FINE:
	- Wenn Daten empfangen wurden, die nicht weiterverarbeitet
	  (plausibilisiert) werden können (weil keine Parameter vorliegen)
	- Informationen, die nur zum Debugging interessant sind


Disclaimer
==========

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


Kontakt
=======

BitCtrl Systems GmbH
Weißenfelser Straße 67
04229 Leipzig
Phone: +49 341-490670
mailto: info@bitctrl.de
