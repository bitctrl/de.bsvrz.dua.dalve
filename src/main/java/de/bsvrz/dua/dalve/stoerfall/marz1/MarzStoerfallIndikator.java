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
package de.bsvrz.dua.dalve.stoerfall.marz1;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
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
 * Repräsentiert einen Stoerfallindikator nach MARZ.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class MarzStoerfallIndikator extends AbstraktStoerfallIndikator {

	private static final Debug LOGGER = Debug.getLogger();

	/** MARZ-Situation <code>freier Verkehr</code>. */
	private static final StoerfallSituation Z1 = StoerfallSituation.FREIER_VERKEHR;

	/** MARZ-Situation <code>dichter Verkehr</code>. */
	private static final StoerfallSituation Z2 = StoerfallSituation.DICHTER_VERKEHR;

	/** MARZ-Situation <code>zähfließender Verkehr</code>. */
	private static final StoerfallSituation Z3 = StoerfallSituation.ZAEHER_VERKEHR;

	/** MARZ-Situation <code>Stau</code>. */
	private static final StoerfallSituation Z4 = StoerfallSituation.STAU;

	/** Grenzgeschwindigkeit 1 (0 &lt; v1 &lt; v2). */
	private long v1 = -4;

	/** Grenzgeschwindigkeit 2 (0 &lt; v1 &lt; v2). */
	private long v2 = -4;

	/** Grenzfahrzeugdichte 2 (0 &lt; k1 &lt; k2). */
	private long k1 = -4;

	/** Grenzfahrzeugdichte 2 (0 &lt; k1 &lt; k2). */
	private long k2 = -4;

	/** letzter errechneter Störfallzustand. */
	private StoerfallSituation letzterStoerfallZustand = StoerfallSituation.KEINE_AUSSAGE;

	/**
	 * Erfassungsintervalldauer.
	 */
	private ErfassungsIntervallDauerMQ erf = null;

	@Override
	public void initialisiere(final ClientDavInterface dav, final SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		if (objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			erf = ErfassungsIntervallDauerMQ.getInstanz(dav, objekt);
		}

		/**
		 * Anmeldung auf Daten
		 */
		dav.subscribeReceiver(this, objekt,
				new DataDescription(DatenaufbereitungLVE.getPubAtgGlatt(this.objekt),
						PrognoseTyp.normal.getAspekt()),
						ReceiveOptions.normal(), ReceiverRole.receiver());
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
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten analog MARZ 2004
	 * (siehe 2.3.2.1.4 Verkehrssituationsuebersicht).
	 *
	 * @param resultat
	 *            ein empfangenes geglaettes Datum mit Nutzdaten
	 */
	@Override
	protected void berechneStoerfallIndikator(final ResultData resultat) {
		Data data = null;

		if (resultat.getData() != null) {
			final String attrV = objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "vKfzG" //$NON-NLS-1$
					: "VKfzG"; //$NON-NLS-1$
			final String attrK = objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "kKfzG" //$NON-NLS-1$
					: "KKfzG"; //$NON-NLS-1$
			// String attrK = this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "kBG" : "KBG";
			// //$NON-NLS-1$ //$NON-NLS-2$

			final long v = resultat.getData().getItem(attrV).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			final long k = resultat.getData().getItem(attrK).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$

			StoerfallSituation situation = StoerfallSituation.KEINE_AUSSAGE;

			if ((v >= 0) && (k >= 0) && (v1 >= 0) && (v2 >= 0) && (k1 >= 0) && (k2 >= 0)) {
				data = DAV.createData(pubBeschreibung.getAttributeGroup());

				if ((v >= v2) && (k >= 0) && (k <= k1)) {
					situation = Z1;
				}
				if ((v >= v2) && (k > k1) && (k <= k2)) {
					situation = Z2;
				}
				if ((v >= v1) && (v < v2) && (k <= k2)) {
					situation = Z3;
				}
				if ((v < v1) && (k > k2)) {
					situation = Z4;
				} else if ((v < v1) || (k > k2)) {
					if (letzterStoerfallZustand.equals(Z3) || letzterStoerfallZustand.equals(Z4)) {
						situation = Z4;
					} else {
						if ((v >= v2) && (k > k2)) {
							situation = Z2;
						}
						if (((v < v2) && (k > k2)) || ((v < v1) && (k <= k2))) {
							situation = Z3;
						}
					}
				}
			}

			final StoerfallZustand zustand = new StoerfallZustand(DAV);
			if (objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
				zustand.setT(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
			} else {
				if ((erf != null) && (erf.getT() > 0)) {
					zustand.setT(erf.getT());
				}
			}
			zustand.setSituation(situation);
			data = zustand.getData();
			letzterStoerfallZustand = situation;
		}

		final ResultData ergebnis = new ResultData(objekt, pubBeschreibung, resultat.getDataTime(),
				data);
		sendeErgebnis(ergebnis);
	}

	@Override
	protected void readParameter(final ResultData parameter) {
		if (parameter.getData() != null) {
			v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$

			/**
			 * Konsitenz-Check
			 */
			if (!((v1 > 0) && (v1 < v2))) {
				LOGGER.warning("Fehlerhafte Parameter (0 < v1 < v2) empfangen fuer " + //$NON-NLS-1$
						objekt + ": v1 = " + v1 + ", v2 = " + v2); //$NON-NLS-1$//$NON-NLS-2$
			}
			if (!((k1 > 0) && (k1 < k2))) {
				LOGGER.warning("Fehlerhafte Parameter (0 < k1 < k2) empfangen fuer " + //$NON-NLS-1$
						objekt + ": k1 = " + k1 + ", k2 = " + k2); //$NON-NLS-1$//$NON-NLS-2$
			}
		} else {
			v1 = -4;
			v2 = -4;
			k1 = -4;
			k2 = -4;
		}
	}

}
