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
package de.bsvrz.dua.dalve.mq;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVSendeAnmeldungsVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

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
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
			
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
		
		String configLog = "Messquerschnitte:"; //$NON-NLS-1$
		for(SystemObject mq:messQuerschnitteGesamt){
			configLog += "\n" + mq; //$NON-NLS-1$
		}
		LOGGER.config(configLog + "\n---"); //$NON-NLS-1$
		

		this.sender = new DAVSendeAnmeldungsVerwaltung(
				verwaltung.getVerbindung(),
				SenderRole.source());	
		
		PUB_BESCHREIBUNG = new DataDescription(
				verwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
				verwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
				(short)0);
		
		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for(SystemObject mq:messQuerschnitteGesamt){
			try {
				anmeldungen.add(new DAVObjektAnmeldung(mq, PUB_BESCHREIBUNG));
			} catch (Exception e) {
				e.printStackTrace();
				throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
			}
		}
		this.sender.modifiziereObjektAnmeldung(anmeldungen);
		
		/**
		 * Initialisiere jetzt alle Messquerschnitte
		 */
		for(SystemObject mq:messQuerschnitte){
			new DaAnalyseMessQuerschnitt().initialisiere(this, mq);
		}
		
		/**
		 * Initialisiere jetzt alle Messquerschnitte
		 */
		for(SystemObject mqv:messQuerschnitteVirtuell){
			new DaAnalyseMessQuerschnittVirtuell().initialisiere(this, mqv);
		}		
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
	
//	/**
//	 * Debug-Logger
//	 */
//	private static final Debug LOGGER = Debug.getLogger();
//	
//	/**
//	 * Datenbeschreibung zum Publizieren
//	 */
//	protected static DataDescription PUB_BESCHREIBUNG = null;
//
//	/**
//	 * Verbindung zum Verwaltungsmodul
//	 */
//	private IVerwaltung verwaltung = null;
//	
//	/**
//	 * Mapt alle innerhalb dieser SWE betrachteten Fahrstreifen auf die
//	 * Messquerschnitte, zu denen sie gehören
//	 */
//	private Map<SystemObject, Collection<DaAnalyseMessQuerschnitt>> fsAufMqMap = new
//										HashMap<SystemObject, Collection<DaAnalyseMessQuerschnitt>>();
//	
//	/**
//	 * Datensender
//	 */
//	private DAVSendeAnmeldungsVerwaltung sender = null;
//
//	
//	
//	/**
//	 * Standardkonstruktor<br>
//	 * <b>Achtung:</b> Es wird hier davon ausgegangen, dass die statische
//	 * Klasse <code>DuaVerkehrsNetz</code> bereits initialisiert wurde
//	 * 
//	 * @param verwaltung eine Verbindung zum Verwaltungsmodul
//	 * @throws DUAInitialisierungsException wenn die Initialisierung wenigstens 
//	 * eines Messquerschnittes fehlschlägt
//	 */
//	public MqAnalyseModul(final IVerwaltung verwaltung)
//	throws DUAInitialisierungsException{
//		this.verwaltung = verwaltung;
//
//		String configLog = "Messquerschnitte:"; //$NON-NLS-1$
//		Collection<DaAnalyseMessQuerschnitt> messQuerschnitte = new HashSet<DaAnalyseMessQuerschnitt>();
//		Collection<SystemObject> messQuerschnittObjekte = new HashSet<SystemObject>();
//		
//		/**
//		 * Ermittle alle Messquerschnitte, die in dieser SWE betrachtet werden
//		 */
//		for(MessQuerschnitt mq:MessQuerschnitt.getInstanzen()){
//			Collection<SystemObject> fsObjekteImMq = new HashSet<SystemObject>();
//			for(SystemObject fsObj:verwaltung.getSystemObjekte()){
//				FahrStreifen fs = FahrStreifen.getInstanz(fsObj);
//				if(fs != null && mq.getFahrStreifen().contains(fs)){
//					fsObjekteImMq.add(fsObj);
//				}
//			}
//			
//			if( !fsObjekteImMq.isEmpty() ){
//				configLog += "\n" + mq; //$NON-NLS-1$
//				messQuerschnitte.add(new DaAnalyseMessQuerschnitt().
//											initialisiere(verwaltung.getVerbindung(), 
//											mq.getSystemObject(), 
//										    fsObjekteImMq));
//				messQuerschnittObjekte.add(mq.getSystemObject());
//			}
//		}
//		LOGGER.config(configLog + "\n---"); //$NON-NLS-1$
//		
//		/**
//		 * Ermittle die Messquerschnitte, deren Berechnung beim Emfang eines Datums für
//		 * einen bestimmten Fahrstreifen getriggert werden soll
//		 */
//		for(DaAnalyseMessQuerschnitt analyseMQ:messQuerschnitte){
//			for(SystemObject fsObj:analyseMQ.getFahrstreifen()){
//				Collection<DaAnalyseMessQuerschnitt> messQuerschnitteZumFahrStreifen = this.fsAufMqMap.get(fsObj);
//				if(messQuerschnitteZumFahrStreifen == null){
//					messQuerschnitteZumFahrStreifen = new HashSet<DaAnalyseMessQuerschnitt>();
//					this.fsAufMqMap.put(fsObj, messQuerschnitteZumFahrStreifen);
//				}
//				messQuerschnitteZumFahrStreifen.add(analyseMQ);
//			}
//		}
//		
//		this.sender = new DAVSendeAnmeldungsVerwaltung(
//				this.verwaltung.getVerbindung(),
//				SenderRole.source());	
//		
//		PUB_BESCHREIBUNG = new DataDescription(
//				verwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
//				verwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
//				(short)0);
//		
//		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
//		for(SystemObject mq:messQuerschnittObjekte){
//			try {
//				anmeldungen.add(new DAVObjektAnmeldung(mq, PUB_BESCHREIBUNG));
//			} catch (Exception e) {
//				e.printStackTrace();
//				throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
//			}
//		}
//		this.sender.modifiziereObjektAnmeldung(anmeldungen);
//
//		verwaltung.getVerbindung().subscribeReceiver(this, verwaltung.getSystemObjekte(), 
//				new DataDescription(
//						verwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitFs"), //$NON-NLS-1$
//						verwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
//						(short)0), ReceiveOptions.normal(), ReceiverRole.receiver());		
//	}
//	
//
//	/**
//	 * {@inheritDoc}
//	 */
//	public void update(ResultData[] resultate) {
//		if(resultate != null){
//			for(ResultData resultat:resultate){
//				if(resultat != null){
//					Collection<DaAnalyseMessQuerschnitt> triggerMQs = this.fsAufMqMap.get(resultat.getObject());
//					if(triggerMQs != null){
//						for(DaAnalyseMessQuerschnitt triggerMQ:triggerMQs){
//							ResultData ergebnis = triggerMQ.trigger(resultat);
//							if(ergebnis != null){
//								this.sender.sende(ergebnis);
//							}
//						}
//					}
//				}
//			}
//		}
//	}
}
