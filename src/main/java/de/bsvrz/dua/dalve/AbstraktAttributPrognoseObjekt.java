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
package de.bsvrz.dua.dalve;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;

// TODO: Auto-generated Javadoc
/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer ein Messwertattribut
 * durch (analog SE-02.00.00.00.00-AFo-4.0, S. 135).<br>
 * <b>Achtung:</b> Diese Implementierung geht davon aus, dass nur Attribute verarbeitet werden, die
 * die Zustaende <code>fehlerhaft</code>, <code>nicht ermittelbar</code> und
 * <code>fehlerhaft/nicht ermittelbar</code> in den Werten -1, -2 und -3 besitzen. Werte mit einem
 * dieser Zustände werden igniriert.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AbstraktAttributPrognoseObjekt {

	/** DAV-Parameter <code>alpha</code> des letzten Glaettungsintervalls mit Trend deltaZNeu = 0. */
	private double alphaAltBeiDeltaZNeuGleich0 = Double.NaN;

	/** DAV-Parameter <code>beta</code> des letzten Glaettungsintervalls mit Trend deltaZNeu = 0. */
	private double betaAltBeiDeltaZNeuGleich0 = Double.NaN;

	/** DAV-Parameter <code>alpha1</code> dieses Attributs. */
	protected double alpha1 = Double.NaN;

	/** DAV-Parameter <code>alpha2</code> dieses Attributs. */
	protected double alpha2 = Double.NaN;

	/** DAV-Parameter <code>beta1</code> dieses Attributs. */
	protected double beta1 = Double.NaN;

	/** DAV-Parameter <code>beta2</code> dieses Attributs. */
	protected double beta2 = Double.NaN;

	/** initialialer Wert fuer ZAlt. */
	protected double ZAltInit = Double.NaN;

	/** Alter Wert fuer deltaZ. */
	protected double deltaZAlt = 0.0;

	/** Alter Prognosewert. */
	protected long ZPAlt = -4;

	/** Prognosewert. */
	private long ZP = DUAKonstanten.NICHT_ERMITTELBAR;

	/** Prognosewert (ungerundet). */
	private double ZPd = DUAKonstanten.NICHT_ERMITTELBAR;

	/** geglaetteter Wert ohne Prognoseanteil. */
	private long ZG = DUAKonstanten.NICHT_ERMITTELBAR;

	/** geglaetteter Wert ohne Prognoseanteil (ungerundet). */
	private double ZGd = DUAKonstanten.NICHT_ERMITTELBAR;

	/** Alter Wert fuer Z. */
	private double ZAlt = Double.NaN;

	/** Wurde das Programm gerade gestartet?. */
	private boolean start = true;

	/**
	 * Berechnet die Glaettungsparameter alpha und beta und startet die Berechnung der
	 * Prognosewerte.
	 *
	 * @param ZAktuell
	 *            aktueller Wert fuer den der geglaettete und der Prognoseteil berechnet werden soll
	 * @param implausibel
	 *            zeigt an, ob das Attribut als implausibel markiert ist
	 * @param istVAttributUndKeineVerkehrsStaerke
	 *            indiziert, ob es sich hier um ein Geschwindigkeitsattribut handelt <b>und</b> dies
	 *            ein Messintervall ohne Fahrzeugdetektion ist
	 * @param davDatum
	 *            das DAV-Datum, aus dem der Z-Wert entnommen wurde bzw. <code>null</code>, wenn
	 *            nicht auf Wertebereiche geachtet werden soll
	 * @throws PrognoseParameterException
	 *             wenn die Parameter noch nicht gesetzt wurden
	 */
	protected final void berechneGlaettungsParameterUndStart(final long ZAktuell,
			final boolean implausibel, final boolean istVAttributUndKeineVerkehrsStaerke,
			final Data davDatum) throws PrognoseParameterException {
		ueberpruefeParameter();

		if (Double.isNaN(ZAlt)) {
			ZAlt = ZAltInit;
		}

		/**
		 * Fehlerhafte Werte werden vom Verfahren ignoriert
		 */
		if ((ZAktuell >= 0) && !implausibel) {
			double alpha = alphaAltBeiDeltaZNeuGleich0;
			double beta = betaAltBeiDeltaZNeuGleich0;

			if (start || (deltaZAlt != 0)) {
				start = false;
				/**
				 * 5. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135 Ist der Trend DZNeu = 0, dann
				 * gelten die Glättungsfaktoren des letzten Glättungsintervalls mit Trend DZNeu = 0.
				 * Email H.C.Kniss (11.09.07): War der Trend im letzten Intervall 0, so werden die
				 * Glättungsparamter für den aktuellen Zyklus nicht geändert (man tut so, als ob
				 * sich der Trend der davor liegenden Zyklen fortsetzt).
				 */
				if (ZAktuell > ZAlt) {
					alpha = alpha2;
					beta = beta2;
				} else {
					alpha = alpha1;
					beta = beta1;
				}
			}

			final double ZNeu = (alpha * ZAktuell) + ((1.0 - alpha) * ZAlt);
			double deltaZNeu;
			if (ZPAlt == 0) {
				/**
				 * 2. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135 Tritt bei der Kurzzeitprognose
				 * ein Wert ZP = 0 auf, so muss der nächste Messwert direkt als Ergebnis der
				 * Prognoserechnung übernommen und der alte Trend auf 0 gesetzt werden Email
				 * H.C.Kniss (11.09.07): War ZP = 0 so ist im aktuellen Zyklus der Messwert als
				 * Prognosewert zu verwenden. Der Trend wird dabei zu 0 gesetzt, weil in diesem Fall
				 * kein Trend ermittelbar ist.
				 */
				deltaZNeu = beta * (ZAktuell - ZAlt);
				// deltaZNeu = 0;
				ZP = ZAktuell;
				ZPd = ZAktuell;
			} else {
				deltaZNeu = (beta * (ZAktuell - ZAlt)) + ((1 - beta) * deltaZAlt);
				ZP = Math.round(ZNeu + deltaZNeu);
				ZPd = ZNeu + deltaZNeu;
			}

			if (ZP < 0) {
				/**
				 * 1. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135 Tritt bei der Kurzzeitprognose
				 * ein Wert ZP < 0 auf, so ist ZP = 0 zu setzen
				 */
				ZP = 0;
				ZPd = 0.0;

				deltaZNeu = 0;
			}

			if (!(istVAttributUndKeineVerkehrsStaerke)) {
				/**
				 * 4. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135 Nach Messintervallen ohne
				 * Fahrzeugdetektion müssen alle geglätteten Geschwindigkeitswerte vom
				 * Vorgängerintervall übernommen werden.<br>
				 * D.h. hier, dass ZG nur dann neu bestimmt wird, wenn dies kein
				 * Geschwindigkeitsattribut ist oder wenn es eines ist und Fahrzeuge detektiert
				 * wurden
				 */
				ZG = Math.round(ZNeu);
				ZGd = ZNeu;
			}

			alphaAltBeiDeltaZNeuGleich0 = alpha;
			betaAltBeiDeltaZNeuGleich0 = beta;

			ZAlt = ZNeu;
			deltaZAlt = deltaZNeu;
			ZPAlt = ZP;
		} else {
			/**
			 * Ausgangzustand wieder herstellen
			 */
			deltaZAlt = 0.0;
			ZPAlt = -4;
			ZAlt = Double.NaN;
			ZP = DUAKonstanten.NICHT_ERMITTELBAR;
			ZPd = DUAKonstanten.NICHT_ERMITTELBAR;
			ZG = DUAKonstanten.NICHT_ERMITTELBAR;
			ZGd = DUAKonstanten.NICHT_ERMITTELBAR;

			start = true;
			// this.alphaAltBeiDeltaZNeuGleich0 = Double.NaN;
			// this.betaAltBeiDeltaZNeuGleich0 = Double.NaN;
		}

		if ((davDatum != null) && !DUAUtensilien.isWertInWerteBereich(davDatum, ZP)) {
			/**
			 * Ausgangzustand wieder herstellen
			 */
			ZPAlt = -4;
			ZP = DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT;
			ZPd = DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT;

			start = true;
			// this.alphaAltBeiDeltaZNeuGleich0 = Double.NaN;
			// this.betaAltBeiDeltaZNeuGleich0 = Double.NaN;
		}
	}

	/**
	 * Erfragt den aktuellen Prognosewert <code>ZP</code> (ungerundet).
	 *
	 * @return der aktuelle Prognosewert <code>ZP</code>
	 */
	public final double getZPOriginal() {
		return ZPd;
	}

	/**
	 * Erfragt den aktuellen Prognosewert <code>ZP</code>.
	 *
	 * @return der aktuelle Prognosewert <code>ZP</code>
	 */
	public final long getZP() {
		return ZP;
	}

	/**
	 * Erfragt den aktuellen geglaetteten Wert <code>ZG</code>.
	 *
	 * @return der aktuelle geglaettete Wert <code>ZG</code>
	 */
	public final long getZG() {
		return ZG;
	}

	/**
	 * Erfragt den aktuellen geglaetteten Wert <code>ZG</code> (ungerundet).
	 *
	 * @return der aktuelle geglaettete Wert <code>ZG</code>
	 */
	public final double getZGOriginal() {
		return ZGd;
	}

	/**
	 * Ueberprueft, ob die Parameter schon gesetzt wurden.
	 *
	 * @throws PrognoseParameterException
	 *             wenn die Parameter noch nicht gesetzt wurden
	 */
	private final void ueberpruefeParameter() throws PrognoseParameterException {
		String subjekt = null;
		if (Double.isNaN(ZAltInit)) {
			subjekt = "ZAlt"; //$NON-NLS-1$
		} else if (Double.isNaN(alpha1)) {
				subjekt = "alpha1"; //$NON-NLS-1$
			} else if (Double.isNaN(alpha2)) {
					subjekt = "alpha2"; //$NON-NLS-1$
				} else if (Double.isNaN(beta1)) {
						subjekt = "beta1"; //$NON-NLS-1$
					} else if (Double.isNaN(beta2)) {
							subjekt = "beta2"; //$NON-NLS-1$
						}

		if (subjekt != null) {
			throw new PrognoseParameterException("Der Parameter " + //$NON-NLS-1$
					subjekt + " wurde noch nicht initialisiert"); //$NON-NLS-1$
		}
	}
}
