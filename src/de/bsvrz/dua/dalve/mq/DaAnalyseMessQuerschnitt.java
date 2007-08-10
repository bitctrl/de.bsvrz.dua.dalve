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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.ConfigurationObject;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.dua.dalve.DaMesswertUnskaliert;
import de.bsvrz.dua.dalve.mq.atg.AtgVerkehrsDatenKurzZeitAnalyseMq;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DaAnalyseMessQuerschnitt {
	
	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
		
	/**
	 * Verbindung zum Datenverteiler
	 */
	private static ClientDavInterface DAV = null;
	
	/**
	 * Mapt alle hier betrachteten Fahrstreifen auf das letzte
	 * von ihnen empfangene Analysedatum
	 */
	private Map<SystemObject, ResultData> aktuelleFSAnalysen = new HashMap<SystemObject, ResultData>();
	
	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt
	 */
	private SystemObject messQuerschnitt = null;
	
	/**
	 * letztes für diesen Messquerschnitt errechnetes Ergebnis
	 */
	private ResultData letztesErgebnis = null;
	
	/**
	 * Aktuelle Analyseparameter dieses MQs
	 */
	private AtgVerkehrsDatenKurzZeitAnalyseMq parameter = null;
	
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param mq der Messquerschnitt
	 * @param fahrStreifenMenge eine Menge von Fahrstreifen, die mit diesem
	 * Messquerschnitt assoziiert ist
	 */
	public DaAnalyseMessQuerschnitt(ClientDavInterface dav, 
									SystemObject mq,
									Collection<SystemObject> fahrStreifenMenge){
		if(DAV == null){
			DAV = dav;
		}
		this.messQuerschnitt = mq;
		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(dav, mq);
		for(SystemObject fs:fahrStreifenMenge){
			this.aktuelleFSAnalysen.put(fs, null);
		}
	}
	
	
	/**
	 * Erfragt die Menge der an diesem Messquerschnitt ausgewerteten Fahrstreifen
	 * 
	 * @return die Menge der an diesem Messquerschnitt ausgewerteten Fahrstreifen
	 */
	public Collection<SystemObject> getFahrstreifen(){
		return this.aktuelleFSAnalysen.keySet();
	}
	
	
	/**
	 * Erfragt den Messquerschnitt
	 * 
	 * @return der Messquerschnitt
	 */
	public SystemObject getObjekt(){
		return messQuerschnitt;
	}
	
	
	/**
	 * Wenn ist ein Datum berechenbar?
	 * 1. Wenn für jeden assoziierten Fahrstreifen ein Datum mit Nutzdaten
	 *    und gleichem Zeitstempel da ist
	 * 2. Wenn für jeden 
	 * 
	 * Wenn das erste Datum mit keine Daten empfangen wird, kann hier ein
	 * Datum keine Daten veröffentlicht werden
	 */
	/**
	 * Trigger
	 * 
	 * TODO: NoData . Datensatz
	 * 
	 * @param triggerDatum
	 * @return
	 */
	public final ResultData trigger(ResultData triggerDatum){
		ResultData ergebnis = null;
		this.aktuelleFSAnalysen.put(triggerDatum.getObject(), triggerDatum);
		
		/**
		 * Zeigt an, ob ein Datum berechnet wurde. Sonst wurden die Daten nur begutachtet
		 * und für <b>noch</b> nicht vollständig eingestuft 
		 */
		boolean datumBerechnet = false;
		
		/**
		 * Das folgende Flag zeigt an, ob dieser MQ zur Zeit auf
		 * "keine Daten" steht. Dies ist der Fall,<br>
		 * 1. wenn noch nie ein Datum für diesen MQ berechnet (versendet) wurde, oder<br>
		 * 2. wenn das letzte für diesen MQ berechnete (versendete) Datum keine Daten hatte.
		 */
		boolean aktuellKeineDaten = this.letztesErgebnis == null || this.letztesErgebnis.getData() == null; 
		
		/**
		 * Es wurde für einen Fahrstreifen dieses MQ ein Datum ohne Nutzdaten empfangen.
		 * D.h., setzte diesen MQ auf keine Daten, wenn er nicht schon in diesem Zustand ist
		 */
		if(triggerDatum.getData() == null && !aktuellKeineDaten){
			datumBerechnet = true;
			ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG,
							triggerDatum.getDataTime(), null);
		}
		
		/**
		 * Es wurde für einen Fahrstreifen dieses MQ ein Datum mit Nutzdaten empfangen.
		 * Überprüfe, ob für alle anderen Fahrstreifen auch bereits Daten mit diesem
		 * Zeitstempel da sind und berechne ggf. die Analysedaten
		 */
		if(triggerDatum.getData() != null){
			final long datenZeit = triggerDatum.getDataTime();
			boolean analysiere = true;
			for(SystemObject fsObj:this.aktuelleFSAnalysen.keySet()){
				ResultData fsKzd = this.aktuelleFSAnalysen.get(fsObj);
				if(fsKzd == null || fsKzd.getData() == null || fsKzd.getDataTime() != datenZeit){
					analysiere = false;
					break;
				}			
			}
			
			if(analysiere){
				datumBerechnet = true;
				Data analyseDatum = DAV.createData(MqAnalyseModul.PUB_BESCHREIBUNG.getAttributeGroup());
				
				/**
				 * Berechne Verkehrsstärken
				 */
				this.berechneVerkehrsStaerke(analyseDatum, "Kfz"); //$NON-NLS-1$
				this.berechneVerkehrsStaerke(analyseDatum, "Lkw"); //$NON-NLS-1$
				this.berechneVerkehrsStaerke(analyseDatum, "Pkw"); //$NON-NLS-1$

				/**
				 * Berechne mittlere Geschwindigkeiten
				 */
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "V", "v");  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Lkw", "V", "v");  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Pkw", "V", "v");  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "Vg", "vg");  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				
				ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG,
						datenZeit, analyseDatum);				
			}
		}

		if(datumBerechnet){
			this.letztesErgebnis = ergebnis;
		}
		
		return ergebnis;
	}
 
	
	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.118f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param attName der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	private final void berechneVerkehrsStaerke(Data analyseDatum, String attName){
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert("Q" + attName);		 //$NON-NLS-1$
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;
		
		long summe = 0;
		ArrayList<GWert> gueteWerte = new ArrayList<GWert>();
		for(ResultData fsDaten:this.aktuelleFSAnalysen.values()){
			MesswertUnskaliert fsWert = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$
			
			if(fsWert.getWertUnskaliert() >= 0){
				summe += fsWert.getWertUnskaliert();
				interpoliert |= fsWert.isInterpoliert();
				try {
					gueteWerte.add(new GWert(fsDaten.getData().getItem("q" + attName).getItem("Güte"))); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (GueteException e) {
					gueteBerechenbar = false;
					e.printStackTrace();
					LOGGER.error("Guete-Index fuer Q" + attName + //$NON-NLS-1$ 
							" nicht berechenbar", e); //$NON-NLS-1$
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
				break;
			}
		}
					
		if(nichtErmittelbarFehlerhaft){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			qAnalyse.setWertUnskaliert(summe);
			if(interpoliert){
				qAnalyse.setInterpoliert(true);
			}
			if(gueteBerechenbar){
				try {
					GWert gesamtGuete = GueteVerfahren.summe(gueteWerte.toArray(new GWert[0]));
					qAnalyse.setGueteIndex(gesamtGuete.getIndex());
					qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
				} catch (GueteException e) {
					e.printStackTrace();
					LOGGER.error("Guete-Index fuer Q" + attName + //$NON-NLS-1$ 
							" nicht berechenbar", e); //$NON-NLS-1$
				}
			}
		}
		
		qAnalyse.kopiereInhaltNach(analyseDatum);
	}
	
	
	
	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.118f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param attName der Attributname des Verkehrswertes, der berechnet werden soll
	 * @param praefixGross Präfix des Attributwertes groß
	 * @param praefixKlein Präfix des Attributwertes klein
	 */
	private final void berechneMittlereGeschwindigkeiten(Data analyseDatum,
														 String attName,
														 String praefixGross,
														 String praefixKlein){
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert(praefixGross + attName);
				
		MesswertUnskaliert Q = new MesswertUnskaliert("Q" + attName); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		long summe = 0;
		ArrayList<GWert> gueteProdukte = new ArrayList<GWert>();

		if(Q.getWertUnskaliert() > 0){
			
			for(ResultData fsDaten:this.aktuelleFSAnalysen.values()){
				MesswertUnskaliert q = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$
				MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName, fsDaten.getData());
				
				if(q.getWertUnskaliert() >= 0 && v.getWertUnskaliert() >= 0){
					interpoliert |= q.isInterpoliert() || v.isInterpoliert();
					summe += q.getWertUnskaliert() * v.getWertUnskaliert();					
				}else{
					nichtErmittelbarFehlerhaft = true;
					break;
				}
				
				try {
					gueteProdukte.add(
							GueteVerfahren.produkt(
									new GWert(fsDaten.getData().getItem("q" + attName).getItem("Güte")), //$NON-NLS-1$ //$NON-NLS-2$
									new GWert(fsDaten.getData().getItem(praefixKlein + attName).getItem("Güte")) //$NON-NLS-1$
							)
					);
				} catch (GueteException e) {
					gueteBerechenbar = false;
					e.printStackTrace();
					LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
							" nicht berechenbar", e); //$NON-NLS-1$
				}				
			}
						
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
								
		if(nichtErmittelbarFehlerhaft){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			long ergebnis = Math.round((double)summe / (double)Q.getWertUnskaliert());
			
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(praefixGross + attName), ergebnis)){
				qAnalyse.setWertUnskaliert(ergebnis);
				if(interpoliert){
					qAnalyse.setInterpoliert(true);
				}
				if(gueteBerechenbar){
					try {
						GWert gesamtGuete = GueteVerfahren.quotient(
								GueteVerfahren.summe(gueteProdukte.toArray(new GWert[0])),
								new GWert(analyseDatum.getItem("Q" + attName).getItem("Güte")) //$NON-NLS-1$ //$NON-NLS-2$
							);
						
						qAnalyse.setGueteIndex(gesamtGuete.getIndex());
						qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
					} catch (GueteException e) {
						e.printStackTrace();
						LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
								" nicht berechenbar", e); //$NON-NLS-1$
					}
				}				
			}else{
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}
		
		qAnalyse.kopiereInhaltNach(analyseDatum);
	}

}
