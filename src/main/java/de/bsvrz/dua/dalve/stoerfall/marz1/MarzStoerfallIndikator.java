/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * Copyright 2015 by Kappich Systemberatung Aachen
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.
 * 
 * de.bsvrz.dua.dalve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */
package de.bsvrz.dua.dalve.stoerfall.marz1;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.ErfassungsIntervallDauerMQ;
import de.bsvrz.dua.dalve.prognose.PrognoseTyp;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Repräsentiert einen Stoerfallindikator nach MARZ
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 */
public class MarzStoerfallIndikator extends AbstraktStoerfallIndikator {

	/**
	 * MARZ-Situation <code>freier Verkehr</code>
	 */
	private static final StoerfallSituation Z1 = StoerfallSituation.FREIER_VERKEHR;

	/**
	 * MARZ-Situation <code>dichter Verkehr</code>
	 */
	private static final StoerfallSituation Z2 = StoerfallSituation.DICHTER_VERKEHR;

	/**
	 * MARZ-Situation <code>zähfließender Verkehr</code>
	 */
	private static final StoerfallSituation Z3 = StoerfallSituation.ZAEHER_VERKEHR;

	/**
	 * MARZ-Situation <code>Stau</code>
	 */
	private static final StoerfallSituation Z4 = StoerfallSituation.STAU;

	/**
	 * Grenzgeschwindigkeit 1 (0 &lt; v1 &lt; v2)
	 */
	private long v1 = -4;

	/**
	 * Grenzgeschwindigkeit 2 (0 &lt; v1 &lt; v2)
	 */
	private long v2 = -4;

	/**
	 * Grenzfahrzeugdichte 2 (0 &lt; k1 &lt; k2)
	 */
	private long k1 = -4;

	/**
	 * Grenzfahrzeugdichte 2 (0 &lt; k1 &lt; k2)
	 */
	private long k2 = -4;

	/**
	 * letzter errechneter Störfallzustand
	 */
	private StoerfallSituation letzterStoerfallZustand = StoerfallSituation.KEINE_AUSSAGE;

	/**
	 * Erfassungsintervalldauer.
	 */
	private ErfassungsIntervallDauerMQ erf = null;
	
	@Override
	public void initialisiere(ClientDavInterface dav, SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);
		
		if(objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			this.erf = ErfassungsIntervallDauerMQ.getInstanz(dav, objekt); 
		}
			
		/**
		 * Anmeldung auf Daten
		 */
		dav.subscribeReceiver(this, objekt, new DataDescription(
				DatenaufbereitungLVE.getPubAtgGlatt(this.objekt),
				PrognoseTyp.NORMAL.getAspekt()), ReceiveOptions
				.normal(), ReceiverRole.receiver());
	}

	@Override
	protected String getParameterAtgPid() {
		return "atg.verkehrsLageVerfahren1"; //$NON-NLS-1$
	}

	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenMARZ"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * analog MARZ 2004 (siehe 2.3.2.1.4 Verkehrssituationsuebersicht)
	 * 
	 * @param resultat
	 *            ein empfangenes geglaettes Datum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat) {
		Data data = null;

		if (resultat.getData() != null) {
			String attrV = this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "vKfzG" : "VKfzG"; //$NON-NLS-1$ //$NON-NLS-2$
			String attrK = this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "kKfzG" : "KKfzG"; //$NON-NLS-1$ //$NON-NLS-2$
			//String attrK = this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "kBG" : "KBG"; //$NON-NLS-1$ //$NON-NLS-2$

			long v = resultat.getData().getItem(attrV)
					.getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			long k = resultat.getData().getItem(attrK)
					.getUnscaledValue("Wert").longValue(); //$NON-NLS-1$

			StoerfallSituation situation = StoerfallSituation.KEINE_AUSSAGE;

			if (v >= 0 && k >= 0 && v1 >= 0 && v2 >= 0 && k1 >= 0 && k2 >= 0) {

				if (v >= v2 && k >= 0 && k <= k1) {
					situation = Z1;
				}
				if (v >= v2 && k > k1 && k <= k2) {
					situation = Z2;
				}
				if (v >= v1 && v < v2 && k <= k2) {
					situation = Z3;
				}
				if (v < v1 && k > k2) {
					situation = Z4;
				} else if (v < v1 || k > k2) {
					if (letzterStoerfallZustand == Z3
							|| letzterStoerfallZustand == Z4) {
						situation = Z4;
					} else {
						if (v >= v2 && k > k2) {
							situation = Z2;
						}
						if ((v < v2 && k > k2) || (v < v1 && k <= k2)) {
							situation = Z3;
						}
					}
				}
			}

			StoerfallZustand zustand = new StoerfallZustand(DAV);
			if (this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
				zustand.setT(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
			} else {
				if(this.erf != null && this.erf.getT() > 0) {
					zustand.setT(this.erf.getT()); //$NON-NLS-1$
				}
			}
			zustand.setSituation(situation);
			data = zustand.getData();
			letzterStoerfallZustand = situation;
		}

		ResultData ergebnis = new ResultData(this.objekt, this.pubBeschreibung,
				resultat.getDataTime(), data);
		this.sendeErgebnis(ergebnis);
	}

	@Override
	protected void readParameter(ResultData parameter) {
		if (parameter.getData() != null) {
			this.v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			this.v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			this.k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			this.k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$

			/**
			 * Konsitenz-Check
			 */
			if (!(v1 > 0 && v1 < v2)) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < v1 < v2) empfangen fuer " + //$NON-NLS-1$
								this.objekt + ": v1 = " + v1 + ", v2 = " + v2); //$NON-NLS-1$//$NON-NLS-2$
			}
			if (!(k1 > 0 && k1 < k2)) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < k1 < k2) empfangen fuer " + //$NON-NLS-1$
								this.objekt + ": k1 = " + k1 + ", k2 = " + k2); //$NON-NLS-1$//$NON-NLS-2$
			}
		} else {
			this.v1 = -4;
			this.v2 = -4;
			this.k1 = -4;
			this.k2 = -4;
		}
	}

}
