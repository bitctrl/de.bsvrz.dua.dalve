/*
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Wei�enfelser Stra�e 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

package de.bsvrz.dua.dalve.util;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;

// TODO: Auto-generated Javadoc
/**
 * Liest die Ausgangsdaten f�r die Pr�fung der Datenaufbereitung LVE ein.
 *
 * @author BitCtrl Systems GmbH, G�rlitz
 */
public class TestAnalyseMessquerschnittImporter extends CSVImporter {

	/** Verbindung zum Datenverteiler. */
	protected static ClientDavInterface DAV = null;

	/** H�lt aktuelle Daten des MQ 1-3. */
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
	 *             falls dieses Objekt nicht vollst�ndig initialisiert werden konnte
	 */
	public TestAnalyseMessquerschnittImporter(final ClientDavInterface dav, final String csvQuelle)
			throws Exception {
		super(csvQuelle);
		if (DAV == null) {
			DAV = dav;
		}

		/**
		 * Tabellenkopf �berspringen
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
	 * Importiert die n�chste Zeile aus der CSV-Datei.
	 */
	public final void importNaechsteZeile() {
		ZEILE = getNaechsteZeile();
	}

	/**
	 * Bildet einen Eingabe-Datensatz aus den Daten der aktuellen CSV-Zeile.
	 *
	 * @return ein Datensatz der �bergebenen Attributgruppe mit den Daten der n�chsten Zeile oder
	 *         <code>null</code>, wenn der Dateizeiger am Ende ist
	 */
	public final Data getDatensatz() {
		Data datensatz = DAV.createData(DAV.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_MQ));

		if (datensatz != null) {

			if (ZEILE != null) {
				try {
					// int fsMulti = 0;
					// int QKfz = Integer.parseInt(ZEILE[0+(fsMulti*2)]);
					// String QKfzStatus = ZEILE[1+(fsMulti*2)];
					// int QPkw = Integer.parseInt(ZEILE[6+(fsMulti*2)]);
					// String QPkwStatus = ZEILE[7+(fsMulti*2)];
					// int QLkw = Integer.parseInt(ZEILE[12+(fsMulti*2)]);
					// String QLkwStatus = ZEILE[13+(fsMulti*2)];
					// int VKfz = Integer.parseInt(ZEILE[18+(fsMulti*2)]);
					// String VKfzStatus = ZEILE[19+(fsMulti*2)];
					// int VPkw = Integer.parseInt(ZEILE[24+(fsMulti*2)]);
					// String VPkwStatus = ZEILE[25+(fsMulti*2)];
					// int VLkw = Integer.parseInt(ZEILE[30+(fsMulti*2)]);
					// String VLkwStatus = ZEILE[31+(fsMulti*2)];
					// int VgKfz = Integer.parseInt(ZEILE[36+(fsMulti*2)]);
					// String VgKfzStatus = ZEILE[37+(fsMulti*2)];
					// int B = Integer.parseInt(ZEILE[42+(fsMulti*2)]);
					// String BStatus = ZEILE[43+(fsMulti*2)];
					// int ALkw = Integer.parseInt(ZEILE[48+(fsMulti*2)]);
					// String ALkwStatus = ZEILE[49+(fsMulti*2)];
					// int KKfz = Integer.parseInt(ZEILE[54+(fsMulti*2)]);
					// String KKfzStatus = ZEILE[55+(fsMulti*2)];
					// int KLkw = Integer.parseInt(ZEILE[60+(fsMulti*2)]);
					// String KLkwStatus = ZEILE[61+(fsMulti*2)];
					// int KPkw = Integer.parseInt(ZEILE[66+(fsMulti*2)]);
					// String KPkwStatus = ZEILE[67+(fsMulti*2)];
					// int QB = Integer.parseInt(ZEILE[72+(fsMulti*2)]);
					// String QBStatus = ZEILE[73+(fsMulti*2)];
					// int KB = Integer.parseInt(ZEILE[78+(fsMulti*2)]);
					// String KBStatus = ZEILE[79+(fsMulti*2)];
					// int SKfz = 1;
					// String SKfzStatus = null;
					// int BMax = 0;
					// String BMaxStatus = null;
					// int VDelta = 0;
					// String VDeltaStatus = null;

					final int QKfz = Integer.parseInt(ZEILE[84]);
					final String QKfzStatus = ZEILE[85];
					final int QLkw = Integer.parseInt(ZEILE[86]);
					final String QLkwStatus = ZEILE[87];
					final int QPkw = Integer.parseInt(ZEILE[88]);
					final String QPkwStatus = ZEILE[89];
					final int VKfz = Integer.parseInt(ZEILE[90]);
					final String VKfzStatus = ZEILE[91];
					final int VLkw = Integer.parseInt(ZEILE[92]);
					final String VLkwStatus = ZEILE[93];
					final int VPkw = Integer.parseInt(ZEILE[94]);
					final String VPkwStatus = ZEILE[95];
					final int VgKfz = Integer.parseInt(ZEILE[96]);
					final String VgKfzStatus = ZEILE[97];
					final int B = Integer.parseInt(ZEILE[98]);
					final String BStatus = ZEILE[99];
					final int BMax = Integer.parseInt(ZEILE[100]);
					final String BMaxStatus = ZEILE[101];
					final int SKfz = Integer.parseInt(ZEILE[102]);
					final String SKfzStatus = ZEILE[103];
					final int ALkw = Integer.parseInt(ZEILE[104]);
					final String ALkwStatus = ZEILE[105];
					final int KKfz = Integer.parseInt(ZEILE[106]);
					final String KKfzStatus = ZEILE[107];
					final int KLkw = Integer.parseInt(ZEILE[108]);
					final String KLkwStatus = ZEILE[109];
					final int KPkw = Integer.parseInt(ZEILE[110]);
					final String KPkwStatus = ZEILE[111];
					final int QB = Integer.parseInt(ZEILE[112]);
					final String QBStatus = ZEILE[113];
					final int KB = Integer.parseInt(ZEILE[114]);
					final String KBStatus = ZEILE[115];
					final int VDelta = Integer.parseInt(ZEILE[116]);
					final String VDeltaStatus = ZEILE[117];

					datensatz = setAttribut("QKfz", QKfz, QKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QPkw", QPkw, QPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QLkw", QLkw, QLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VKfz", VKfz, VKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VPkw", VPkw, VPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VLkw", VLkw, VLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VgKfz", VgKfz, VgKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("B", B, BStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("BMax", BMax, BMaxStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("ALkw", ALkw, ALkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KKfz", KKfz, KKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KLkw", KLkw, KLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KPkw", KPkw, KPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QB", QB, QBStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KB", KB, KBStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("SKfz", SKfz, SKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VDelta", VDelta, VDeltaStatus, datensatz); //$NON-NLS-1$
				} catch (final ArrayIndexOutOfBoundsException ex) {
					datensatz = null;
				}
			} else {
				datensatz = null;
			}
		}

		return datensatz;
	}

	/**
	 * Setzt Attribut in Datensatz.
	 *
	 * @param attributName
	 *            Name des Attributs
	 * @param wert
	 *            Wert des Attributs
	 * @param status
	 *            the status
	 * @param datensatz
	 *            der Datensatz
	 * @return der ver�nderte Datensatz
	 */
	private final Data setAttribut(final String attributName, long wert, final String status,
			final Data datensatz) {
		final Data data = datensatz;

		if (attributName.startsWith("V") && (wert >= 255)) { //$NON-NLS-1$
			wert = -1;
		}

		if (attributName.startsWith("K") && (wert > 10000)) { //$NON-NLS-1$
			wert = -1;
		}

		if (attributName.startsWith("B") && (wert > 100)) { //$NON-NLS-1$
			wert = -1;
		}

		int nErf = DUAKonstanten.NEIN;
		int wMax = DUAKonstanten.NEIN;
		int wMin = DUAKonstanten.NEIN;
		int wMaL = DUAKonstanten.NEIN;
		int wMiL = DUAKonstanten.NEIN;
		int impl = DUAKonstanten.NEIN;
		int intp = DUAKonstanten.NEIN;
		double guete = 1.0;

		int errCode = 0;

		if (status != null) {
			final String[] splitStatus = status.trim().split(" "); //$NON-NLS-1$

			for (final String splitStatu : splitStatus) {
				if (splitStatu.equalsIgnoreCase("Fehl")) {
					errCode = errCode - 2;
				}

				if (splitStatu.equalsIgnoreCase("nErm")) {
					errCode = errCode - 1;
				}

				if (splitStatu.equalsIgnoreCase("Impl")) {
					impl = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("Intp")) {
					intp = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("nErf")) {
					nErf = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("wMaL")) {
					wMaL = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("wMax")) {
					wMax = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("wMiL")) {
					wMiL = DUAKonstanten.JA;
				}

				if (splitStatu.equalsIgnoreCase("wMin")) {
					wMin = DUAKonstanten.JA;
				}

				try {
					// guete = Float.parseFloat(splitStatus[i].replace(",", "."))*10000;
					guete = Float.parseFloat(splitStatu.replace(",", ".")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (final Exception e) {
					// kein float Wert
				}
			}
		}

		if (errCode < 0) {
			wert = errCode;
		}

		DUAUtensilien.getAttributDatum(attributName + ".Wert", data).asUnscaledValue().set(wert); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.Erfassung.NichtErfasst", data).asUnscaledValue().set(nErf); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.PlFormal.WertMax", data).asUnscaledValue().set(wMax); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.PlFormal.WertMin", data).asUnscaledValue().set(wMin); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.PlLogisch.WertMaxLogisch", data).asUnscaledValue().set(wMaL); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.PlLogisch.WertMinLogisch", data).asUnscaledValue().set(wMiL); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.MessWertErsetzung.Implausibel", data).asUnscaledValue().set(impl); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".Status.MessWertErsetzung.Interpoliert", data).asUnscaledValue().set(intp); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".G�te.Index", data).asScaledValue().set(guete); //$NON-NLS-1$
		DUAUtensilien
		.getAttributDatum(attributName + ".G�te.Verfahren", data).asUnscaledValue().set(0); //$NON-NLS-1$

		return datensatz;
	}
}
