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
package de.bsvrz.dua.dalve.analyse;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVSendeAnmeldungsVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell.BerechnungsVorschrift;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Modul in dem die Analyse der einzelnen Messquerschnitte (auch virtuell) angeschoben 
 * wird. Für jeden betrachteten MQ und VMQ wird ein Objekt angelegt, dass auf die Daten
 * der assoziierten Objekte (Fahrstreifen oder MQs) lauscht und ggf. über diese Klasse
 * ein Analysedatum publiziert
 *  
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class MqAnalyseModul {
		
	/**
	 * Datenbeschreibung zum Publizieren von MQ-Analyse-Daten.
	 */
	protected DataDescription pubBeschreibung = null;

	/**
	 * Datensender.
	 */
	private DAVSendeAnmeldungsVerwaltung sender = null;
	
	/**
	 * Datenverteiler-Verbindung.
	 */
	private ClientDavInterface dav = null;
	
	private static final Debug _debug = Debug.getLogger();
	
	
	/**
	 * Initialisiert dieses Modul.<br>
	 * <b>Achtung:</b> Es wird hier davon ausgegangen, dass die statische
	 * Klasse <code>DuaVerkehrsNetz</code> bereits initialisiert wurde
	 * 
	 * @param verwaltung eine Verbindung zum Verwaltungsmodul
	 * @throws DUAInitialisierungsException wenn die Initialisierung wenigstens 
	 * eines Messquerschnittes fehlschlägt
	 */
	public final void initialisiere(final IVerwaltung verwaltung)
			throws DUAInitialisierungsException {
		this.dav = verwaltung.getVerbindung();

		Collection<SystemObject> messQuerschnitteGesamt = new HashSet<SystemObject>();
		Collection<SystemObject> messQuerschnitte = new HashSet<SystemObject>();
		Collection<SystemObject> messQuerschnitteVirtuellVLage = new HashSet<SystemObject>();
		Collection<SystemObject> messQuerschnitteVirtuellStandard = new HashSet<SystemObject>();

		/**
		 * Ermittle alle Messquerschnitte, die in dieser SWE betrachtet werden sollen
		 */
		for (MessQuerschnitt mq : MessQuerschnitt.getInstanzen()) {
			messQuerschnitte.addAll(DUAUtensilien.getBasisInstanzen(mq
					.getSystemObject(), verwaltung.getVerbindung(), verwaltung
					.getKonfigurationsBereiche()));
		}

		/**
		 * Ermittle alle virtuellen Messquerschnitte, die in dieser SWE betrachtet werden sollen
		 */
		for (MessQuerschnittVirtuell mqv : MessQuerschnittVirtuell
				.getInstanzen()) {
			if (mqv.getBerechnungsVorschrift() == BerechnungsVorschrift.AUF_BASIS_VON_ATG_MQ_VIRTUELL_STANDARD) {
				messQuerschnitteVirtuellStandard.addAll(DUAUtensilien
						.getBasisInstanzen(mqv.getSystemObject(), verwaltung
								.getVerbindung(), verwaltung
								.getKonfigurationsBereiche()));
			} else if (mqv.getBerechnungsVorschrift() == BerechnungsVorschrift.AUF_BASIS_VON_ATG_MQ_VIRTUELL_V_LAGE) {
				messQuerschnitteVirtuellVLage.addAll(DUAUtensilien
						.getBasisInstanzen(mqv.getSystemObject(), verwaltung
								.getVerbindung(), verwaltung
								.getKonfigurationsBereiche()));
			} else {
				_debug.warning("Virtueller Messquerschnitt " + mqv + " wird ignoriert, da keine Berechnungsvorschrift ermittelbar ist.");
			}
		}
		messQuerschnitteGesamt.addAll(messQuerschnitte);
		messQuerschnitteGesamt.addAll(messQuerschnitteVirtuellVLage);
		messQuerschnitteGesamt.addAll(messQuerschnitteVirtuellStandard);

		String configLog = "Betrachtete Messquerschnitte:"; //$NON-NLS-1$
		for (SystemObject mq : messQuerschnitteGesamt) {
			configLog += "\n" + mq; //$NON-NLS-1$
		}
		Debug.getLogger().config(configLog + "\n---"); //$NON-NLS-1$

		/**
		 * Publikationsbeschreibung für Analysewerte von allgemeinen MQs
		 */
		this.sender = new DAVSendeAnmeldungsVerwaltung(verwaltung
				.getVerbindung(), SenderRole.source());
		pubBeschreibung = new DataDescription(verwaltung.getVerbindung()
				.getDataModel()
				.getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ), verwaltung
				.getVerbindung().getDataModel().getAspect(
						DUAKonstanten.ASP_ANALYSE));

		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for (SystemObject mq : messQuerschnitteGesamt) {
			try {
				anmeldungen.add(new DAVObjektAnmeldung(mq, pubBeschreibung));
			} catch (IllegalArgumentException e) {
				throw new DUAInitialisierungsException("",
						e);
			}
		}
		this.sender.modifiziereObjektAnmeldung(anmeldungen);

		/**
		 * Initialisiere jetzt alle Messquerschnitte
		 */
		for (SystemObject mq : messQuerschnitte) {
			if (new DaAnalyseMessQuerschnitt().initialisiere(this, mq) == null) {
				try {
					anmeldungen.remove(new DAVObjektAnmeldung(mq,
							pubBeschreibung));
				} catch (IllegalArgumentException e) {
					throw new DUAInitialisierungsException(
							"", e);
				}
			}
		}

		/**
		 * Initialisiere jetzt alle virtuellen Messquerschnitte (nach der alten
		 * Methode)
		 */
		for (SystemObject mqv : messQuerschnitteVirtuellStandard) {
			if (new DaAnalyseMessQuerschnittVirtuellStandard().initialisiere(
					this, mqv) == null) {
				try {
					anmeldungen.remove(new DAVObjektAnmeldung(mqv,
							pubBeschreibung));
				} catch (IllegalArgumentException e) {
					throw new DUAInitialisierungsException(
							"", e);
				}
			}
		}

		/**
		 * Initialisiere jetzt alle virtuellen Messquerschnitte (nach der alten Methode)
		 */
		for (SystemObject mqv : messQuerschnitteVirtuellVLage) {
			if (new DaAnalyseMessQuerschnittVirtuellVLage().initialisiere(
					this, mqv) == null) {
				try {
					anmeldungen.remove(new DAVObjektAnmeldung(mqv,
							pubBeschreibung));
				} catch (IllegalArgumentException e) {
					throw new DUAInitialisierungsException(
							"", e);
				}
			}
		}

		this.sender.modifiziereObjektAnmeldung(anmeldungen);
	}

	/**
	 * Sendet ein Analysedatum an den Datenverteiler.
	 * 
	 * @param resultat ein Analysedatum 
	 */
	public final void sendeDaten(final ResultData resultat) {
		this.sender.sende(resultat);
	}

	/**
	 * Erfragt die Verbindung zum Datenverteiler.
	 * 
	 * @return die Verbindung zum Datenverteiler
	 */
	public final ClientDavInterface getDav() {
		return this.dav;
	}

}
