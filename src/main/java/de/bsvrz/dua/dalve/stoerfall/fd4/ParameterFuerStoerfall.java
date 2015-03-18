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
package de.bsvrz.dua.dalve.stoerfall.fd4;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;

// TODO: Auto-generated Javadoc
/**
 * Hier sind alle Vergleichswerte (mit Hysterese) gespeichert, die zur Ermittlung eines bestimmten
 * Stoerfalls (<code>Stau</code>, <code>freier Verkehr</code>, ...) ueber die lokale
 * Stoerfallerkennung benoetigt werden
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class ParameterFuerStoerfall extends AllgemeinerDatenContainer {

	/** die Stoerfallsituation, deren Vergleichswerte hier gespeichert werden (sollen). */
	private StoerfallSituation situation = null;

	/** Absoluter Wert für den Vergleich mit VKfzStörfall. */
	private double Vgrenz = -4;

	/** Hysteresewert für Vgrenz (VGrenz +/- VgrenzHysterese). */
	private double VgrenzHysterese = -4;

	/** Faktor für den Vergleich von KKfzStörfallG mid K0. */
	private double fk = -1;

	/** Hysteresewert für fk(fk +/- fkHysterese). */
	private double fkHysterese = -1;

	/** Faktor für den Vergleich von VKfzStörfallG mid V0. */
	private double fv = -1;

	/** Hysteresewert für fv(fv +/- fkHysterese). */
	private double fvHysterese = -1;

	/**
	 * der Faktor <code>fp</code>.
	 */
	private double fp = -1;

	/**
	 * der Faktor <code>fa</code>.
	 */
	private double fa = -1;

	/**
	 * Standardkonstruktor.
	 *
	 * @param situation
	 *            die Stoerfallsituation, deren Vergleichswerte hier gespeichert werden (sollen)
	 */
	protected ParameterFuerStoerfall(final StoerfallSituation situation) {
		this.situation = situation;
	}

	/**
	 * Speisst dieses Objekt.
	 *
	 * @param datum
	 *            ein DAV-Datum der Attributgruppe
	 *            <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 */
	protected final void importiere(final Data datum) {
		if (datum != null) {
			fa = datum.getScaledValue("fa").doubleValue();
			fp = datum.getScaledValue("fp").doubleValue();
			fk = datum.getScaledValue("fk").doubleValue();
			fkHysterese = datum.getScaledValue("fkHysterese").doubleValue();
			fv = datum.getScaledValue("fv").doubleValue();
			fvHysterese = datum.getScaledValue("fvHysterese").doubleValue();
			Vgrenz = datum.getUnscaledValue("Vgrenz").longValue();
			VgrenzHysterese = datum.getUnscaledValue("VgrenzHysterese").longValue();
		} else {
			Vgrenz = -4;
			VgrenzHysterese = -4;
			fk = -1;
			fkHysterese = -1;
			fv = -1;
			fvHysterese = -1;
			fa = -1;
			fp = -1;
		}
	}

	/**
	 * Erfragt, ob dieses Objekt bereits mit Parametern initialisiert wurde.
	 *
	 * @return ob dieses Objekt bereits mit Parametern initialisiert wurde
	 */
	protected final boolean isInitialisiert() {
		return (Vgrenz != -4) && (VgrenzHysterese != -4) && (fk != -1) && (fkHysterese != -1)
				&& (fv != -1) && (fvHysterese != -1) && (fa != -1) && (fp != -1);
	}

	/**
	 * Erfragt die Stoerfallsituation, deren Vergleichswerte hier gespeichert werden (sollen).
	 *
	 * @return die Stoerfallsituation, deren Vergleichswerte hier gespeichert werden (sollen)
	 */
	protected final StoerfallSituation getSituation() {
		return situation;
	}

	/**
	 * Erfragt absoluter Wert für den Vergleich mit VKfzStörfall.
	 *
	 * @return absoluter Wert für den Vergleich mit VKfzStörfall
	 */
	protected final double getVgrenz() {
		return Vgrenz;
	}

	/**
	 * Erfragt Hysteresewert für Vgrenz (VGrenz +/- VgrenzHysterese).
	 *
	 * @return Hysteresewert für Vgrenz (VGrenz +/- VgrenzHysterese)
	 */
	protected final double getVgrenzHysterese() {
		return VgrenzHysterese;
	}

	/**
	 * Erfragt den Faktor <code>fa</code>.
	 *
	 * @return der Faktor <code>fa</code>
	 **/
	protected final double getFa() {
		return fa;
	}

	/**
	 * Erfragt den Faktor <code>fp</code>.
	 *
	 * @return der Faktor <code>fp</code>
	 **/
	protected final double getFp() {
		return fp;
	}

	/**
	 * Erfragt Faktor für den Vergleich von KKfzStörfallG mid K0.
	 *
	 * @return Faktor für den Vergleich von KKfzStörfallG mid K0
	 */
	protected final double getFk() {
		return fk;
	}

	/**
	 * Erfragt Hysteresewert für fk(fk +/- fkHysterese).
	 *
	 * @return Hysteresewert für fk(fk +/- fkHysterese)
	 */
	protected final double getFkHysterese() {
		return fkHysterese;
	}

	/**
	 * Erfragt Faktor für den Vergleich von VKfzStörfallG mid V0.
	 *
	 * @return Faktor für den Vergleich von VKfzStörfallG mid V0
	 */
	protected final double getFv() {
		return fv;
	}

	/**
	 * Erfragt Hysteresewert für fv(fv +/- fkHysterese).
	 *
	 * @return Hysteresewert für fv(fv +/- fkHysterese)
	 */
	protected final double getFvHysterese() {
		return fvHysterese;
	}

}
