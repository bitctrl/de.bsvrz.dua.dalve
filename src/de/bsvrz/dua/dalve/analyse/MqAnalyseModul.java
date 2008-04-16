/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.analyse;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import com.bitctrl.Constants;

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
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Modul in dem die Analyse der einzelnen Messquerschnitte (auch virtuell) angeschoben 
 * wird. Für jeden betrachteten MQ und VMQ wird ein Objekt angelegt, dass auf die Daten
 * der assoziierten Objekte (Fahrstreifen oder MQs) lauscht und ggf. über diese Klasse
 * ein Analysedatum publiziert
 *  
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class MqAnalyseModul{
		
	/**
	 * Datenbeschreibung zum Publizieren von MQ-Analyse-Daten
	 */
	protected static DataDescription PUB_BESCHREIBUNG = null;

	/**
	 * Datensender
	 */
	private DAVSendeAnmeldungsVerwaltung sender = null;
	
	/**
	 * Datenverteiler-Verbindung
	 */
	private ClientDavInterface dav = null;
	
	
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
	throws DUAInitialisierungsException{
		this.dav = verwaltung.getVerbindung();
		
		Collection<SystemObject> messQuerschnitteGesamt = new HashSet<SystemObject>();
		Collection<SystemObject> messQuerschnitte = new HashSet<SystemObject>();
		Collection<SystemObject> messQuerschnitteVirtuell = new HashSet<SystemObject>();
		
		/**
		 * Ermittle alle Messquerschnitte, die in dieser SWE betrachtet werden sollen
		 */
		for(MessQuerschnitt mq:MessQuerschnitt.getInstanzen()){
			messQuerschnitte.addAll(
					DUAUtensilien.getBasisInstanzen(mq.getSystemObject(),
													verwaltung.getVerbindung(),
													verwaltung.getKonfigurationsBereiche()));
		}
		
		/**
		 * Ermittle alle virtuellen Messquerschnitte, die in dieser SWE betrachtet werden sollen
		 */
		for(MessQuerschnittVirtuell mqv:MessQuerschnittVirtuell.getInstanzen()){
			messQuerschnitteVirtuell.addAll(
					DUAUtensilien.getBasisInstanzen(mqv.getSystemObject(),
													verwaltung.getVerbindung(),
													verwaltung.getKonfigurationsBereiche()));
		}
		messQuerschnitteGesamt.addAll(messQuerschnitte);
		messQuerschnitteGesamt.addAll(messQuerschnitteVirtuell);
		
		String configLog = "Betrachtete Messquerschnitte:"; //$NON-NLS-1$
		for(SystemObject mq:messQuerschnitteGesamt){
			configLog += "\n" + mq; //$NON-NLS-1$
		}
		Debug.getLogger().config(configLog + "\n---"); //$NON-NLS-1$
		
		/**
		 * Publikationsbeschreibung für Analysewerte von allgemeinen MQs
		 */
		this.sender = new DAVSendeAnmeldungsVerwaltung(
									verwaltung.getVerbindung(),
									SenderRole.source());	
		PUB_BESCHREIBUNG = new DataDescription(
				verwaltung.getVerbindung().getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ),
				verwaltung.getVerbindung().getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
				(short)0);
		
		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for(SystemObject mq:messQuerschnitteGesamt){
			try {
				anmeldungen.add(new DAVObjektAnmeldung(mq, PUB_BESCHREIBUNG));
			} catch (IllegalArgumentException e) {
				throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
			}
		}
		this.sender.modifiziereObjektAnmeldung(anmeldungen);
		
		/**
		 * Initialisiere jetzt alle Messquerschnitte
		 */
		for(SystemObject mq:messQuerschnitte){
			if(new DaAnalyseMessQuerschnitt().initialisiere(this, mq) == null){
				try {
					anmeldungen.remove(new DAVObjektAnmeldung(mq, PUB_BESCHREIBUNG));
				} catch (IllegalArgumentException e) {
					throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
				}
			}
		}
		
		/**
		 * Initialisiere jetzt alle Messquerschnitte
		 */
		for(SystemObject mqv:messQuerschnitteVirtuell){
			if(new DaAnalyseMessQuerschnittVirtuell().initialisiere(this, mqv) == null){
				try {
					anmeldungen.remove(new DAVObjektAnmeldung(mqv, PUB_BESCHREIBUNG));
				} catch (IllegalArgumentException e) {
					throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
				}				
			}
		}
		
		this.sender.modifiziereObjektAnmeldung(anmeldungen);
	}
	
	
	/**
	 * Sendet ein Analysedatum an den Datenverteiler
	 * 
	 * @param resultat ein Analysedatum 
	 */
	public final void sendeDaten(final ResultData resultat){
		this.sender.sende(resultat);
	}

	
	/**
	 * Erfragt die Verbindung zum Datenverteiler
	 * 
	 * @return die Verbindung zum Datenverteiler
	 */
	public final ClientDavInterface getDav(){
		return this.dav;
	}
	
}
