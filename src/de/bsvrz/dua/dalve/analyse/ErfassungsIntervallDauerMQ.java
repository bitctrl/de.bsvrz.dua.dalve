/**
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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
package de.bsvrz.dua.dalve.analyse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.InvalidArgumentException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittAllgemein;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Stellt die virtuelle Erfassungsintervalldauer <code>T</code> eines MQ auf
 * Basis seiner Fahrstreifen zur Verfuegung.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
final class ErfassungsIntervallDauerMQ implements ClientReceiverInterface {

	/**
	 * Platzhalter fuer noch nicht ermittelbare Erfassungsintervalldauer.
	 */
	static final long NOCH_NICHT_ERMITTELBAR = -1;
	
	/**
	 * Platzhalter fuer nicht einheitliche Erfassungsintervalldauern.
	 */
	static final long NICHT_EINHEITLICH = -2;

	/**
	 * Alle statischen Instanzen dieser Klasse.
	 */
	private static Map<SystemObject, ErfassungsIntervallDauerMQ> instanzen = new HashMap<SystemObject, ErfassungsIntervallDauerMQ>();

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
	static ErfassungsIntervallDauerMQ getInstanz(ClientDavInterface dav,
			SystemObject mq) {
		ErfassungsIntervallDauerMQ instanz = null;

		synchronized (instanzen) {
			instanz = instanzen.get(mq);
			if (instanz == null) {
				try {
					instanz = new ErfassungsIntervallDauerMQ(dav, mq);
					instanzen.put(mq, instanz);
				} catch (InvalidArgumentException e) {
					e.printStackTrace();
					Debug.getLogger().error(
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
	long getT() {
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