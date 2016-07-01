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
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.AbstraktAttributPrognoseObjekt;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer ein Attribut eines
 * Fahrstreifens bzw. eines Messquerschnittes durch
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DavAttributPrognoseObjekt extends AbstraktAttributPrognoseObjekt
		implements IAtgPrognoseParameterListener {

	/** der Prognosetyp. */
	private PrognoseTyp typ = null;

	/** Das Objekt, dessen Attribut hier betrachtet wird. */
	private SystemObject prognoseObjekt = null;

	/** Das Attribut, das hier betrachtet wird. */
	private PrognoseAttribut attribut = null;

	/** Erfragt, ob es sich bei diesem Attribut um ein Geschwindigkeitsattribut handelt. */
	private boolean vAttribut = false;

	/** aktuelles Datum. */
	private DaMesswertUnskaliert aktuellesDatum = null;

	/** der Name des Attributs, das hier betrachtet wird (Daten-Quelle) Prognose. */
	private String attributNameP = null;

	/** der Name des Attributs, das hier betrachtet wird (Daten-Quelle) glatt. */
	private String attributNameG = null;

	/** der Name des Attributs, das hier betrachtet wird (Daten-Ziel). */
	private String attributNameQuelle = null;

	/**
	 * Standardkonstruktor.
	 *
	 * @param prognoseObjekt
	 *            das Objekt, das hier betrachtet wird
	 * @param attribut
	 *            das Attribut, das hier betrachtet wird
	 * @param typ
	 *            der Prognosetyp
	 */
	public DavAttributPrognoseObjekt(final SystemObject prognoseObjekt,
			final PrognoseAttribut attribut, final PrognoseTyp typ) {
		this.prognoseObjekt = prognoseObjekt;
		this.attribut = attribut;
		this.typ = typ;
		vAttribut = attribut.equals(PrognoseAttribut.V_KFZ)
				|| attribut.equals(PrognoseAttribut.V_LKW)
				|| attribut.equals(PrognoseAttribut.V_PKW);
		attributNameP = this.attribut.getAttributNamePrognose(
				this.prognoseObjekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN));
		attributNameG = this.attribut
				.getAttributNameGlatt(this.prognoseObjekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN));
		attributNameQuelle = this.attribut
				.getAttributName(prognoseObjekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN));
	}

	/**
	 * Aktualisiert die Daten dieses Prognoseobjektes mit empfangenen Daten.
	 *
	 * @param resultat
	 *            ein empfangenes Analysedatum
	 * @throws PrognoseParameterException
	 *             wenn die Parameter noch nicht gesetzte wurden
	 */
	public final void aktualisiere(final ResultData resultat) throws PrognoseParameterException {

		if (resultat.getData() != null) {
			aktuellesDatum = new DaMesswertUnskaliert(attributNameQuelle, resultat.getData());
			final long zAktuell = resultat.getData().getItem(attributNameQuelle)
					.getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			final Data davDatum = resultat.getData().getItem(attributNameQuelle).getItem("Wert");
			final boolean implausibel = resultat.getData().getItem(attributNameQuelle)
					.getItem("Status").getItem("MessWertErsetzung").getUnscaledValue("Implausibel") //$NON-NLS-2$ //$NON-NLS-3$
					.intValue() == DUAKonstanten.JA;

			/**
			 * Messintervallen ohne Fahrzeugdetektion?
			 */
			boolean keineVerkehrsStaerke = false;
			if (vAttribut) {

				/**
				 * Aenderung nach Email vom 14.4.2008:
				 *
				 * hier ist wirklich der Fall gemeint, dass im aktuellen Intervall kein Fahrzeug
				 * gefahren ist (qKfz = 0) dann sollen die geglätteten Geschwindigkeitswerte des
				 * Vorgängerintervalls übernommen werden.
				 */
				if (prognoseObjekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
					keineVerkehrsStaerke = resultat.getData().getItem("qKfz") //$NON-NLS-1$
							.getUnscaledValue("Wert").longValue() == 0;
				} else {
					keineVerkehrsStaerke = resultat.getData().getItem("QKfz") //$NON-NLS-1$
							.getUnscaledValue("Wert").longValue() == 0;
				}

				// keineVerkehrsStaerke =
				// resultat.getData().getItem(this.attribut.getQAttributAnalogon(
				// this.prognoseObjekt.isFahrStreifen())).getUnscaledValue("Wert").longValue()
				// == 0; //$NON-NLS-1$
			}

			berechneGlaettungsParameterUndStart(zAktuell, implausibel, keineVerkehrsStaerke,
					davDatum);
		} else {
			aktuellesDatum = null;
		}
	}

	/**
	 * Erfragt den Attributname.
	 *
	 * @return attribut den Attributname
	 */
	public PrognoseAttribut getAttribut() {
		return attribut;
	}

	/**
	 * Exportiert die letzten hier errechneten geglaetteten Werte in das uebergebene Zieldatum.
	 *
	 * @param zielDatum
	 *            ein veraenderbares Zieldatum der Attributgruppe
	 *            <code>atg.verkehrsDatenKurzZeitGeglättetFs</code>
	 */
	public final void exportiereDatenGlatt(final Data zielDatum) {
		final MesswertUnskaliert exportWert = new MesswertUnskaliert(attributNameG);

		exportWert.setWertUnskaliert(getZG());
		exportWert.setNichtErfasst(aktuellesDatum.isNichtErfasst());
		exportWert.setInterpoliert(aktuellesDatum.isPlausibilisiert());
		exportWert.getGueteIndex().setWert(aktuellesDatum.getGueteIndex().getWert());
		exportWert.setVerfahren(aktuellesDatum.getVerfahren());

		exportWert.kopiereInhaltNachModifiziereIndex(zielDatum);
	}

	/**
	 * Exportiert die letzten hier errechneten Prognosewerte in das uebergebene Zieldatum.
	 *
	 * @param zielDatum
	 *            ein veraenderbares Zieldatum der Attributgruppe
	 *            <code>atg.verkehrsDatenKurzZeitTrendExtraPolationFs</code>
	 */
	public final void exportiereDatenPrognose(final Data zielDatum) {
		final MesswertUnskaliert exportWert = new MesswertUnskaliert(attributNameP);

		exportWert.setWertUnskaliert(getZP());
		exportWert.setNichtErfasst(aktuellesDatum.isNichtErfasst());
		exportWert.setInterpoliert(aktuellesDatum.isPlausibilisiert());
		exportWert.getGueteIndex().setWert(aktuellesDatum.getGueteIndex().getWert());
		exportWert.setVerfahren(aktuellesDatum.getVerfahren());

		exportWert.kopiereInhaltNachModifiziereIndex(zielDatum);
	}

	@Override
	public void aktualisiereParameter(final PrognoseAttributParameter parameterSatzFuerAttribut) {
		ZAltInit = parameterSatzFuerAttribut.getStart();
		alpha1 = parameterSatzFuerAttribut.getAlpha1();
		alpha2 = parameterSatzFuerAttribut.getAlpha2();
		beta1 = parameterSatzFuerAttribut.getBeta1();
		beta2 = parameterSatzFuerAttribut.getBeta2();
	}

	@Override
	public String toString() {
		return prognoseObjekt.getPid() + ", " + attribut + ", " + typ;
	}

}
