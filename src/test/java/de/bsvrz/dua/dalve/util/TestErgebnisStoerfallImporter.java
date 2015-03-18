/*
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

package de.bsvrz.dua.dalve.util;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;

// TODO: Auto-generated Javadoc
/**
 * Liest die Ausgangsdaten für die Prüfung der Datenaufbereitung LVE ein.
 *
 * @author BitCtrl Systems GmbH, Görlitz
 */
public class TestErgebnisStoerfallImporter extends CSVImporter {

	/** Verbindung zum Datenverteiler. */
	protected static ClientDavInterface DAV = null;

	/** Hält aktuelle Daten des FS 1-3. */
	protected String[] ZEILE;

	/** T. */
	protected static long INTERVALL = Constants.MILLIS_PER_MINUTE;

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Datenverteier-Verbindung
	 * @param csvQuelle
	 *            Quelle der Daten (CSV-Datei)
	 * @throws Exception
	 *             falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public TestErgebnisStoerfallImporter(final ClientDavInterface dav, final String csvQuelle)
			throws Exception {
		super(csvQuelle);
		if (DAV == null) {
			DAV = dav;
		}

		/**
		 * Tabellenkopf überspringen
		 */
		getNaechsteZeile();
	}

	/**
	 * Setzt Datenintervall.
	 *
	 * @param t
	 *            Datenintervall
	 */
	public static final void setT(final long t) {
		INTERVALL = t;
	}

	/**
	 * Importiert die nächste Zeile aus der CSV-Datei.
	 */
	public final void importNaechsteZeile() {
		ZEILE = getNaechsteZeile();
	}

	/**
	 * Bildet einen Ausgabe-Datensatz des Stoerfallindikator MARZ aus den Daten der aktuellen
	 * CSV-Zeile.
	 *
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile oder
	 *         <code>null</code>, wenn der Dateizeiger am Ende ist
	 */
	public final Data getSFvstMARZ() {
		return getSF(0);
	}

	/**
	 * Bildet einen Ausgabe-Datensatz des Stoerfallindikator NRW aus den Daten der aktuellen
	 * CSV-Zeile.
	 *
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile oder
	 *         <code>null</code>, wenn der Dateizeiger am Ende ist
	 */
	public final Data getSFvstNRW() {
		return getSF(1);
	}

	/**
	 * Bildet einen Ausgabe-Datensatz des Stoerfallindikator RDS aus den Daten der aktuellen
	 * CSV-Zeile.
	 *
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile oder
	 *         <code>null</code>, wenn der Dateizeiger am Ende ist
	 */
	public final Data getSFvstRDS() {
		return getSF(2);
	}

	/**
	 * Bildet einen Ausgabe-Datensatz der Stoerfallindikatoren aus den Daten der aktuellen
	 * CSV-Zeile.
	 *
	 * @param offset
	 *            Der Offset des zu nutzenden Stoerfallverfahrens
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile oder
	 *         <code>null</code>, wenn der Dateizeiger am Ende ist
	 */
	private final Data getSF(final int offset) {
		Data datensatz = DAV
				.createData(DAV.getDataModel().getAttributeGroup("atg.störfallZustand")); //$NON-NLS-1$

		if (datensatz != null) {

			if (ZEILE != null) {
				try {
					final int situation = Integer.parseInt(ZEILE[17 + offset]);

					DUAUtensilien
					.getAttributDatum("T", datensatz).asTimeValue().setMillis(INTERVALL); //$NON-NLS-1$
					DUAUtensilien
					.getAttributDatum("Situation", datensatz).asUnscaledValue().set(situation); //$NON-NLS-1$
					DUAUtensilien
					.getAttributDatum("Horizont", datensatz).asTimeValue().setMillis(0); //$NON-NLS-1$
					DUAUtensilien
					.getAttributDatum("Güte.Index", datensatz).asUnscaledValue().set(1); //$NON-NLS-1$
					DUAUtensilien
					.getAttributDatum("Güte.Verfahren", datensatz).asUnscaledValue().set(0); //$NON-NLS-1$

				} catch (final ArrayIndexOutOfBoundsException ex) {
					datensatz = null;
				}
			} else {
				datensatz = null;
			}
		}

		return datensatz;
	}

}
