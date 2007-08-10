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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVSendeAnmeldungsVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Modul in dem die Analyse der einzelnen Messquerschnitte stattfindet. Dieses
 * Modul ist auf alle Analysewerte der in den übergebenen Konfigurationsbereichen
 * definierten Fahrstreifen angemeldet und berechnet daraus die Analysedaten der
 * Messquerschnitte.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class MqAnalyseModul
implements ClientReceiverInterface{
	
	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	/**
	 * Datenbeschreibung zum Publizieren
	 */
	protected static DataDescription PUB_BESCHREIBUNG = null;

	/**
	 * Verbindung zum Verwaltungsmodul
	 */
	private IVerwaltung verwaltung = null;
	
	/**
	 * Mapt alle innerhalb dieser SWE betrachteten Fahrstreifen auf die
	 * Messquerschnitte, zu denen sie gehören
	 */
	private Map<SystemObject, Collection<DaAnalyseMessQuerschnitt>> fsAufMqMap = new
										HashMap<SystemObject, Collection<DaAnalyseMessQuerschnitt>>();
	
	/**
	 * Datensender
	 */
	private DAVSendeAnmeldungsVerwaltung sender = null;

	
	
	/**
	 * Standardkonstruktor<br>
	 * <b>Achtung:</b> Es wird hier davon ausgegangen, dass die statische
	 * Klasse <code>DuaVerkehrsNetz</code> bereits initialisiert wurde
	 * 
	 * @param verwaltung eine Verbindung zum Verwaltungsmodul
	 * @throws DUAInitialisierungsException wenn die Initialisierung wenigstens 
	 * eines Messquerschnittes fehlschlägt
	 */
	public MqAnalyseModul(final IVerwaltung verwaltung)
	throws DUAInitialisierungsException{
		this.verwaltung = verwaltung;

		String configLog = "Messquerschnitte:"; //$NON-NLS-1$
		Collection<DaAnalyseMessQuerschnitt> messQuerschnitte = new HashSet<DaAnalyseMessQuerschnitt>();
		Collection<SystemObject> messQuerschnittObjekte = new HashSet<SystemObject>();
		
		/**
		 * Ermittle alle Messquerschnitte, die in dieser SWE betrachtet werden
		 */
		for(MessQuerschnitt mq:MessQuerschnitt.getInstanzen()){
			Collection<SystemObject> fsObjekteImMq = new HashSet<SystemObject>();
			for(SystemObject fsObj:verwaltung.getSystemObjekte()){
				FahrStreifen fs = FahrStreifen.getInstanz(fsObj);
				if(fs != null && mq.getFahrStreifen().contains(fs)){
					fsObjekteImMq.add(fsObj);
				}
			}
			
			if( !fsObjekteImMq.isEmpty() ){
				configLog += "\n" + mq; //$NON-NLS-1$				
				messQuerschnitte.add(new DaAnalyseMessQuerschnitt(verwaltung.getVerbindung(), 
																  mq.getSystemObject(), 
																  fsObjekteImMq));
				messQuerschnittObjekte.add(mq.getSystemObject());
			}
		}
		LOGGER.config(configLog + "\n---"); //$NON-NLS-1$
		
		/**
		 * Ermittle die Messquerschnitte, deren Berechnung beim Emfang eines Datums für
		 * einen bestimmten Fahrstreifen getriggert werden soll
		 */
		for(DaAnalyseMessQuerschnitt analyseMQ:messQuerschnitte){
			for(SystemObject fsObj:analyseMQ.getFahrstreifen()){
				Collection<DaAnalyseMessQuerschnitt> messQuerschnitteZumFahrStreifen = this.fsAufMqMap.get(fsObj);
				if(messQuerschnitteZumFahrStreifen == null){
					messQuerschnitteZumFahrStreifen = new HashSet<DaAnalyseMessQuerschnitt>();
					this.fsAufMqMap.put(fsObj, messQuerschnitteZumFahrStreifen);
				}
				messQuerschnitteZumFahrStreifen.add(analyseMQ);
			}
		}
		
		this.sender = new DAVSendeAnmeldungsVerwaltung(
				this.verwaltung.getVerbindung(),
				SenderRole.source());	
		
		PUB_BESCHREIBUNG = new DataDescription(
				verwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
				verwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
				(short)0);
		
		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for(SystemObject mq:messQuerschnittObjekte){
			try {
				anmeldungen.add(new DAVObjektAnmeldung(mq, PUB_BESCHREIBUNG));
			} catch (Exception e) {
				e.printStackTrace();
				throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
			}
		}
		this.sender.modifiziereObjektAnmeldung(anmeldungen);

		verwaltung.getVerbindung().subscribeReceiver(this, verwaltung.getSystemObjekte(), 
				new DataDescription(
						verwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitFs"), //$NON-NLS-1$
						verwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
						(short)0), ReceiveOptions.normal(), ReceiverRole.receiver());		
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					Collection<DaAnalyseMessQuerschnitt> triggerMQs = this.fsAufMqMap.get(resultat.getObject());
					if(triggerMQs != null){
						for(DaAnalyseMessQuerschnitt triggerMQ:triggerMQs){
							ResultData ergebnis = triggerMQ.trigger(resultat);
							if(ergebnis != null){
								this.sender.sende(ergebnis);
							}
						}
					}
				}
			}
		}
	}

}
