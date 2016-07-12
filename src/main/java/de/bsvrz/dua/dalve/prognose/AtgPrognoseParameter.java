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
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Haelt für ein bestimmtes Objekt (Fahrstreifen oder Messquerschnitt) alle
 * Parameter bereit die sich auf die Messwertprognose beziehen. Dabei kann
 * zwischen den Parametertypen <code>Flink</code>, <code>Normal</code> und
 * <code>Träge</code> unterschieden werden
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 */
public class AtgPrognoseParameter implements ClientReceiverInterface {

	/**
	 * der Prognosetyp
	 */
	private PrognoseTyp typ = null;

	/**
	 * das Objekt, auf dessen Prognose-Parameter sich angemeldet werden soll
	 */
	private SystemObject objekt = null;

	/**
	 * Menge aktueller Werte der Attributparameter
	 */
	private Map<PrognoseAttribut, PrognoseAttributParameter> einzelWerte = new HashMap<PrognoseAttribut, PrognoseAttributParameter>();

	/**
	 * Menge von Beobachtern einzelner Attributparameter
	 */
	private Map<PrognoseAttribut, Set<IAtgPrognoseParameterListener>> attributListener = new HashMap<PrognoseAttribut, Set<IAtgPrognoseParameterListener>>();

	/**
	 * Standardkonstruktor
	 * 
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param objekt
	 *            das Objekt, auf dessen Prognose-Parameter sich angemeldet
	 *            werden soll
	 * @param typ
	 *            der Typ der Parameter auf die sich angemeldet werden soll
	 *            (Flink, Normal, Träge)
	 */
	public AtgPrognoseParameter(final ClientDavInterface dav,
			final SystemObject objekt, final PrognoseTyp typ) {
		this.objekt = objekt;
		this.typ = typ;
		this.initialisiere();
		dav.subscribeReceiver(this, objekt, new DataDescription(typ
				.getParameterAtg(objekt
						.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), dav
				.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_SOLL)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	/**
	 * Fügt der Menge aller Listener auf einen bestimmten Attributparameter
	 * einen neuen Listener hinzu (und informiert diesen initial)
	 * 
	 * @param listener
	 *            der neue Listener
	 * @param attribut
	 *            das Attribut, auf dessen Parameter gehört werden soll
	 */
	public final void addListener(final IAtgPrognoseParameterListener listener,
			final PrognoseAttribut attribut) {
		synchronized (this) {
			Set<IAtgPrognoseParameterListener> beobachterMenge = this.attributListener
					.get(attribut);
			if (beobachterMenge == null) {
				beobachterMenge = new HashSet<IAtgPrognoseParameterListener>();
				this.attributListener.put(attribut, beobachterMenge);
			}

			beobachterMenge.add(listener);
			listener.aktualisiereParameter(this.einzelWerte.get(attribut));
		}
	}

	/**
	 * Informiert alle Beobachter über Veraenderungen
	 */
	private final void informiereAlleBeobachter() {
		synchronized (this) {
			// System.out.println();
			for (PrognoseAttribut attribut : this.attributListener.keySet()) {
				PrognoseAttributParameter einzelWert = this.einzelWerte
						.get(attribut);
				for (IAtgPrognoseParameterListener listener : this.attributListener
						.get(attribut)) {
					// System.out.println("Informiere: " + listener.toString() +
					// ", ueber: " + einzelWert) ;
					listener.aktualisiereParameter(einzelWert);
				}
			}
			// System.out.println();
		}
	}

	/**
	 * Initialisiert die Daten dieses Objekts auf den Zustand von
	 * <code>keine Daten</code>
	 */
	private final void initialisiere() {
		synchronized (this) {
			for (PrognoseAttribut attribut : PrognoseAttribut.getInstanzen()) {
				/**
				 * fuellt jeden Attributwert mit nicht initialisierten Werten
				 */
				this.einzelWerte.put(attribut, new PrognoseAttributParameter(
						attribut));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					synchronized (this) {
						if (resultat.getData() != null) {
							for (PrognoseAttribut attribut : PrognoseAttribut
									.getInstanzen()) {
								this.einzelWerte
										.get(attribut)
										.setDaten(
												resultat.getData(),
												this.objekt
														.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN));
							}
						} else {
							this.initialisiere();
						}
						this.informiereAlleBeobachter();
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
		return this.objekt + ", " + this.typ;
	}

}
