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

import java.util.HashMap;
import java.util.Map;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;

/**
 * Stellt die aktuellen Informationen der Attributgruppe
 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code> fuer einen bestimmten Messquerschnitt
 * zur Verfuegung
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgLokaleStoerfallErkennungFundamentalDiagramm implements ClientReceiverInterface {

	/** Mappt eine Stoerfallsituation auf ihre Parameter. */
	private final Map<StoerfallSituation, ParameterFuerStoerfall> parameter = new HashMap<>();

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param objekt
	 *            Systemobjekt des betrachteten Messquerschnittes
	 */
	protected AtgLokaleStoerfallErkennungFundamentalDiagramm(final ClientDavInterface dav,
			final SystemObject objekt) {
		parameter.put(StoerfallSituation.FREIER_VERKEHR,
				new ParameterFuerStoerfall(StoerfallSituation.FREIER_VERKEHR));
		parameter.put(StoerfallSituation.STAU, new ParameterFuerStoerfall(StoerfallSituation.STAU));
		parameter.put(StoerfallSituation.ZAEHER_VERKEHR,
				new ParameterFuerStoerfall(StoerfallSituation.ZAEHER_VERKEHR));

		dav.subscribeReceiver(this, objekt,
				new DataDescription(
						dav.getDataModel().getAttributeGroup(
								"atg.lokaleStörfallErkennungFundamentalDiagramm"), //$NON-NLS-1$
				dav.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_SOLL)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	/**
	 * Erfragt die aktuellen Parameter der Attributgruppe
	 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code> fuer einen bestimmten Stoerfall
	 *
	 * @param situation
	 *            ein bestimmter Stoerfall
	 * @return die aktuellen Parameter der Attributgruppe
	 *         <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code> fuer den Stoerfall
	 */
	protected final ParameterFuerStoerfall getParameterFuerStoerfall(
			final StoerfallSituation situation) {
		return parameter.get(situation);
	}

	/**
	 * Erfragt, ob alle Parameter valide sind.
	 *
	 * @return ob alle Parameter valide sind
	 */
	protected boolean alleParameterInitialisiert() {
		for (final ParameterFuerStoerfall pss : parameter.values()) {
			if (!pss.isInitialisiert()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					if (resultat.getData() != null) {
						final ParameterFuerStoerfall frei = new ParameterFuerStoerfall(
								StoerfallSituation.FREIER_VERKEHR);
						final ParameterFuerStoerfall zaeh = new ParameterFuerStoerfall(
								StoerfallSituation.ZAEHER_VERKEHR);
						final ParameterFuerStoerfall stau = new ParameterFuerStoerfall(
								StoerfallSituation.STAU);

						frei.importiere(resultat.getData().getItem("FreierVerkehr"));
						zaeh.importiere(resultat.getData().getItem("ZaehFliessenderVerkehr"));
						stau.importiere(resultat.getData().getItem("Stau"));

						synchronized (parameter) {
							parameter.put(StoerfallSituation.FREIER_VERKEHR, frei);
							parameter.put(StoerfallSituation.STAU, stau);
							parameter.put(StoerfallSituation.ZAEHER_VERKEHR, zaeh);
						}
					}
				}
			}
		}
	}

}
