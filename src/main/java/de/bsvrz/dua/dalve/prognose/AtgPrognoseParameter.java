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
package de.bsvrz.dua.dalve.prognose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;

// TODO: Auto-generated Javadoc
/**
 * Haelt f�r ein bestimmtes Objekt (Fahrstreifen oder Messquerschnitt) alle Parameter bereit die
 * sich auf die Messwertprognose beziehen. Dabei kann zwischen den Parametertypen <code>Flink</code>
 * , <code>Normal</code> und <code>Tr�ge</code> unterschieden werden
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgPrognoseParameter implements ClientReceiverInterface {

	/** der Prognosetyp. */
	private PrognoseTyp typ = null;

	/** das Objekt, auf dessen Prognose-Parameter sich angemeldet werden soll. */
	private SystemObject objekt = null;

	/** Menge aktueller Werte der Attributparameter. */
	private final Map<PrognoseAttribut, PrognoseAttributParameter> einzelWerte = new HashMap<PrognoseAttribut, PrognoseAttributParameter>();

	/** Menge von Beobachtern einzelner Attributparameter. */
	private final Map<PrognoseAttribut, Set<IAtgPrognoseParameterListener>> attributListener = new HashMap<PrognoseAttribut, Set<IAtgPrognoseParameterListener>>();

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param objekt
	 *            das Objekt, auf dessen Prognose-Parameter sich angemeldet werden soll
	 * @param typ
	 *            der Typ der Parameter auf die sich angemeldet werden soll (Flink, Normal, Tr�ge)
	 */
	public AtgPrognoseParameter(final ClientDavInterface dav, final SystemObject objekt,
			final PrognoseTyp typ) {
		this.objekt = objekt;
		this.typ = typ;
		initialisiere();
		dav.subscribeReceiver(
				this,
				objekt,
				new DataDescription(typ.getParameterAtg(objekt
						.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), dav.getDataModel().getAspect(
						DaVKonstanten.ASP_PARAMETER_SOLL)), ReceiveOptions.normal(), ReceiverRole
						.receiver());
	}

	/**
	 * F�gt der Menge aller Listener auf einen bestimmten Attributparameter einen neuen Listener
	 * hinzu (und informiert diesen initial).
	 *
	 * @param listener
	 *            der neue Listener
	 * @param attribut
	 *            das Attribut, auf dessen Parameter geh�rt werden soll
	 */
	public final void addListener(final IAtgPrognoseParameterListener listener,
			final PrognoseAttribut attribut) {
		synchronized (this) {
			Set<IAtgPrognoseParameterListener> beobachterMenge = attributListener.get(attribut);
			if (beobachterMenge == null) {
				beobachterMenge = new HashSet<IAtgPrognoseParameterListener>();
				attributListener.put(attribut, beobachterMenge);
			}

			beobachterMenge.add(listener);
			listener.aktualisiereParameter(einzelWerte.get(attribut));
		}
	}

	/**
	 * Informiert alle Beobachter �ber Veraenderungen.
	 */
	private final void informiereAlleBeobachter() {
		synchronized (this) {
			// System.out.println();
			for (final PrognoseAttribut attribut : attributListener.keySet()) {
				final PrognoseAttributParameter einzelWert = einzelWerte.get(attribut);
				for (final IAtgPrognoseParameterListener listener : attributListener.get(attribut)) {
					// System.out.println("Informiere: " + listener.toString() +
					// ", ueber: " + einzelWert) ;
					listener.aktualisiereParameter(einzelWert);
				}
			}
			// System.out.println();
		}
	}

	/**
	 * Initialisiert die Daten dieses Objekts auf den Zustand von <code>keine Daten</code>.
	 */
	private final void initialisiere() {
		synchronized (this) {
			for (final PrognoseAttribut attribut : PrognoseAttribut.getInstanzen()) {
				/**
				 * fuellt jeden Attributwert mit nicht initialisierten Werten
				 */
				einzelWerte.put(attribut, new PrognoseAttributParameter(attribut));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					synchronized (this) {
						if (resultat.getData() != null) {
							for (final PrognoseAttribut attribut : PrognoseAttribut.getInstanzen()) {
								einzelWerte.get(attribut).setDaten(resultat.getData(),
										objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN));
							}
						} else {
							initialisiere();
						}
						informiereAlleBeobachter();
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return objekt + ", " + typ;
	}

}
