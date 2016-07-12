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
package de.bsvrz.dua.dalve;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.InvalidArgumentException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittAllgemein;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Stellt die virtuelle Erfassungsintervalldauer <code>T</code> eines MQ auf
 * Basis seiner Fahrstreifen zur Verfuegung.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public final class ErfassungsIntervallDauerMQ implements ClientReceiverInterface {

	/**
	 * Platzhalter fuer noch nicht ermittelbare Erfassungsintervalldauer.
	 */
	public static final long NOCH_NICHT_ERMITTELBAR = -1;
	
	/**
	 * Platzhalter fuer nicht einheitliche Erfassungsintervalldauern.
	 */
	public static final long NICHT_EINHEITLICH = -2;

	/**
	 * Alle statischen Instanzen dieser Klasse.
	 */
	private static final Map<SystemObject, ErfassungsIntervallDauerMQ> instanzen = new IdentityHashMap<>();

	/**
	 * Das jeweils letzte Datum pro Fahrstreifen.
	 */
	private Map<SystemObject, ResultData> letztesDatumProFahrstreifen = null;

	/**
	 * Aktuelle virtuelle Erfassungsintervalldauer <code>T</code> dieses MQ.
	 */
	private long erfassungsIntervallDauer = NOCH_NICHT_ERMITTELBAR;

	/**
	 * Standardkonstruktor.
	 * 
	 * @param dav
	 *            Verbindung zum Datenverteiler.
	 * @param mq
	 *            der Messquerschnitt.
	 * @throws InvalidArgumentException
	 *             wenn die Konfiguration des MQ nicht ausgelesen werden konnte.
	 */
	private ErfassungsIntervallDauerMQ(ClientDavInterface dav, SystemObject mq)
			throws InvalidArgumentException {
		MessQuerschnittAllgemein mqa = MessQuerschnittAllgemein.getInstanz(mq);

		if (mqa == null) {
			throw new InvalidArgumentException(
					"Die Konfigurationdaten des allgemeinen MQ " + mq
							+ " konnten nicht ausgelesen werden.");
		} else {
			DataDescription dd = new DataDescription(dav.getDataModel()
					.getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS), dav
					.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE));

			if (mqa.getFahrStreifen() == null
					|| mqa.getFahrStreifen().isEmpty()) {
				throw new InvalidArgumentException("Der MQ " + mq
						+ " besitzt keine Fahrstreifen");
			} else {
				this.letztesDatumProFahrstreifen = new HashMap<SystemObject, ResultData>();
				List<SystemObject> fsListe = new ArrayList<SystemObject>();

				for (FahrStreifen fs : mqa.getFahrStreifen()) {
					fsListe.add(fs.getSystemObject());
					this.letztesDatumProFahrstreifen.put(fs.getSystemObject(),
							null);
				}

				dav.subscribeReceiver(this, fsListe
						.toArray(new SystemObject[0]), dd, ReceiveOptions
						.normal(), ReceiverRole.receiver());
			}
		}
	}

	/**
	 * Erfragt eine statische Instanz dieser Klasse.
	 * 
	 * @param dav
	 *            Verbindung zum Datenverteiler.
	 * @param mq
	 *            der Messquerschnitt.
	 * @return eine statische Instanz dieser Klasse, oder <code>null</code>,
	 *         wenn diese nicht ermittelt werden konnte
	 */
	public static ErfassungsIntervallDauerMQ getInstanz(ClientDavInterface dav,
			SystemObject mq) {
		ErfassungsIntervallDauerMQ instanz = null;

		synchronized (instanzen) {
			instanz = instanzen.get(mq);
			if (instanz == null) {
				try {
					instanz = new ErfassungsIntervallDauerMQ(dav, mq);
					instanzen.put(mq, instanz);
				} catch (InvalidArgumentException e) {
					Debug.getLogger().warning(
							"Erfassungsintervalldauer von " + mq
									+ " kann nicht ueberwacht werden.\nGrund: "	+ e.getMessage());
				}
			}
		}

		return instanz;
	}

	/**
	 * Erfragt die aktuelle virtuelle Erfassungsintervalldauer <code>T</code>
	 * dieses MQ.
	 * 
	 * @return die aktuelle virtuelle Erfassungsintervalldauer <code>T</code>
	 *         dieses MQ.
	 */
	public long getT() {
		return this.erfassungsIntervallDauer;
	}

	/**
	 * Bestimmt die virtuelle Erfassungsintervalldauer dieses MQ auf Basis der
	 * letzen eingetroffenen Fahrstreifendaten.
	 */
	private void versucheNeuBerechnung() {
		SortedSet<Long> intervallDauern = new TreeSet<Long>();

		for (SystemObject mq : this.letztesDatumProFahrstreifen.keySet()) {
			ResultData letztesDatum = this.letztesDatumProFahrstreifen.get(mq);
			if (letztesDatum != null && letztesDatum.getData() != null) {
				intervallDauern.add(letztesDatum.getData().getTimeValue("T")
						.getMillis());
			}
		}

		if (intervallDauern.size() == 0) {
			this.erfassungsIntervallDauer = NOCH_NICHT_ERMITTELBAR;
		} else if (intervallDauern.size() == 1) {
			this.erfassungsIntervallDauer = intervallDauern.first();
		} else {
			this.erfassungsIntervallDauer = NICHT_EINHEITLICH;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] results) {
		if (results != null) {
			for (ResultData result : results) {
				if (result != null) {
					this.letztesDatumProFahrstreifen.put(result.getObject(),
							result);
					this.versucheNeuBerechnung();
				}
			}
		}
	}

}
