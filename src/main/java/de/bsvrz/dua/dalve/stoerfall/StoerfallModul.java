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
package de.bsvrz.dua.dalve.stoerfall;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.stoerfall.fd4.FdStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.marz1.MarzStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.nrw2.NrwStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.rds3.RdsStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.vkdiffkfz.VKDiffKfzStoerfallIndikator;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;

import java.util.Collection;

/**
 * Von diesem Objekt aus wird die Berechnung der einzelnen Stoerfallindikatoren
 * gestartet.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class StoerfallModul {

	/**
	 * Initialisiert alle Stoerfallindikatoren
	 * 
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param objekte
	 *            Menge der Fahrstreifen und Messquerschnitte, die betrachtet
	 *            werden sollen
	 * @throws DUAInitialisierungsException
	 *             wird weitergereicht
	 */
	public final void initialisiere(final ClientDavInterface dav,
			final Collection<SystemObject> objekte)
			throws DUAInitialisierungsException {

		/**
		 * (I, II) SFI nach Verfahren MARZ, NRW
		 */
		for (SystemObject obj : objekte) {
			if (obj.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)
					|| obj.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
				new MarzStoerfallIndikator().initialisiere(dav, obj);
				new NrwStoerfallIndikator().initialisiere(dav, obj);
			}
		}


		/**
		 * (III) SFI nach Verfahren RDS
		 */
		for (SystemObject obj : objekte) {
			if (obj.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
				new RdsStoerfallIndikator().initialisiere(dav, obj);
			}
		}

		/**
		 * (IV) SFI nach Verfahren Fundamentaldiagramm
		 */
		for (SystemObject obj : objekte) {
			if (obj.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
				new FdStoerfallIndikator().initialisiere(dav, obj);
			}
		}

		/**
		 * (V) SFI nach Verfahren VKDiffKfz
		 */
		for (SystemObject obj : objekte) {
			if (obj.isOfType(DUAKonstanten.TYP_STRASSEN_ABSCHNITT)) {
				new VKDiffKfzStoerfallIndikator().initialisiere(dav, obj);
			}
		}
	}

}
