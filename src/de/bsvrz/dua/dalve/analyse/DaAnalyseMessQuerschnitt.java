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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.dua.dalve.prognose.DaMesswertUnskaliert;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der Analysewerte
 * eines Messquerschnitts notwendig sind gespeichert. Jedes mit dem MQ assoziierte 
 * Fahrstreifendatum muss durch dieses Objekt (Methode <code>trigger(..)</code>) geleitet
 * werden um ggf. auch eine neue Berechnung von Analysewerten auszulösen.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DaAnalyseMessQuerschnitt
implements ClientReceiverInterface{
	
	/**
	 * Debug-Logger
	 */
	protected static final Debug LOGGER = Debug.getLogger();
		
	/**
	 * Verbindung zum Analysemodul
	 */
	protected static MqAnalyseModul MQ_ANALYSE = null;
	
	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt
	 */
	protected SystemObject messQuerschnitt = null;
	
	/**
	 * letztes für diesen Messquerschnitt errechnetes Ergebnis
	 */
	protected ResultData letztesErgebnis = null;
	
	/**
	 * Aktuelle Analyseparameter dieses MQs
	 */
	protected AtgVerkehrsDatenKurzZeitAnalyseMq parameter = null;
	
	/**
	 * Mapt alle hier betrachteten Fahrstreifen auf das letzte
	 * von ihnen empfangene Analysedatum
	 */
	private Map<SystemObject, ResultData> aktuelleFSAnalysen = new HashMap<SystemObject, ResultData>();
	
	
	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurück.
	 * Nach dieser Initialisierung ist das Objekt auf alle Daten (seiner assoziierten
	 * Fahrstreifen) angemeldet und analysiert ggf. Daten 
	 * 
	 * @param analyseModul Verbindung zum Analysemodul
	 * @param messQuerschnitt der Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException wenn die Konfigurationsdaten des MQs nicht vollständig
	 * ausgelesen werden konnte 
	 */
	public DaAnalyseMessQuerschnitt initialisiere(MqAnalyseModul analyseModul, 
			  									  SystemObject messQuerschnitt)
	throws DUAInitialisierungsException{
		if(MQ_ANALYSE == null){
			MQ_ANALYSE = analyseModul;
		}
		this.messQuerschnitt = messQuerschnitt;
		
		if(MessQuerschnitt.getInstanz(messQuerschnitt) != null){
			for(FahrStreifen fs:MessQuerschnitt.getInstanz(messQuerschnitt).getFahrStreifen()){
				this.aktuelleFSAnalysen.put(fs.getSystemObject(), null);
			}
		}else{
			throw new DUAInitialisierungsException("MQ-Konfiguration von " + messQuerschnitt +  //$NON-NLS-1$
					" konnte nicht vollständig ausgelesen werden"); //$NON-NLS-1$
		}

		if(this.aktuelleFSAnalysen.keySet().isEmpty()){
			LOGGER.warning("Der MQ " + this.messQuerschnitt + " hat keine Fahrstreifen");  //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}

		/**
		 * Anmeldung auf Parameter und alle Daten der assoziierten Messquerschnitte
		 */			
		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(MQ_ANALYSE.getDav(), messQuerschnitt);
		MQ_ANALYSE.getDav().subscribeReceiver(
				this,
				this.aktuelleFSAnalysen.keySet(), 
				new DataDescription(
						MQ_ANALYSE.getDav().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitFs"), //$NON-NLS-1$
						MQ_ANALYSE.getDav().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
						(short)0),
						ReceiveOptions.normal(),
						ReceiverRole.receiver());
	
		return this;
	}
	
	
	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem Messquerschnitt
	 * assoziierten Fahrstreifen übergeben werden. Ggf. wird dadurch dann eine Berechnung
	 * der Analysewerte dieses Messquerschnittes ausgelöst.
	 * 
	 * @param triggerDatum ein KZ-Datum eines assoziierten Fahrstreifens
	 * @return ein Analysedatum für diesen Messquerschnitt, wenn das <code>triggerDatum</code>
	 * eine Berechnung ausgelöst hat, oder <code>null</code> sonst
	 */
	protected ResultData trigger(ResultData triggerDatum){
		ResultData ergebnis = null;
		this.aktuelleFSAnalysen.put(triggerDatum.getObject(), triggerDatum);
		
		/**
		 * Zeigt an, ob ein Datum berechnet wurde. Sonst wurden die Daten nur begutachtet
		 * und für <b>noch</b> nicht vollständig erachtet 
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
			
			/**
			 * Führe also nur eine Berechnung durch, wenn
			 * für alle Fahrstreifen ein Fahrstreifendatum mit Nutzdaten und
			 * gleichem Zeitstempel im Puffer steht.
			 */
			if(analysiere){
				datumBerechnet = true;
				Data analyseDatum = MQ_ANALYSE.getDav().createData(MqAnalyseModul.PUB_BESCHREIBUNG.getAttributeGroup());
				
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
				
				/**
				 * Belegung B und BMax
				 */
				this.berechneBelegung(analyseDatum);
				
				/**
				 * Standardabweichung
				 */
				this.berechneStandardabweichung(analyseDatum);
				
				/**
				 * Berechne LKW-Anteil
				 */
				this.berechneLkwAnteil(analyseDatum);
				
				/**
				 * Berechne Fahrzeugdichten
				 */
				this.berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
				this.berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
				this.berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$
				
				/**
				 * Bemessungsverkehrsstärke 
				 */
				this.berechneBemessungsVerkehrsstaerke(analyseDatum);
					
				/**
				 * Bemessungsdichte
				 */
				this.berechneBemessungsdichte(analyseDatum);
				
				/**
				 * Berechne die gewichtete Differenzgeschwindigkeit im Messquerschnitt
				 */
				this.berechneVDifferenz(analyseDatum);
				
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
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					ResultData ergebnis = trigger(resultat);
					if(ergebnis != null){
						MQ_ANALYSE.sendeDaten(ergebnis);
					}
				}
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finalize()
	throws Throwable {
		LOGGER.warning("Der MQ " + this.messQuerschnitt +  //$NON-NLS-1$
				" wird nicht mehr analysiert"); //$NON-NLS-1$
	}

	
	
	/**********************************************************************************
	 *                                                                                *
	 *                           Berechnungs-Methoden                                 *
	 *                                                                                *
	 **********************************************************************************/
	
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
				gueteWerte.add(new GWert(fsDaten.getData(), "q" + attName)); //$NON-NLS-1$
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
					qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
					qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer Q" + attName + //$NON-NLS-1$ 
							" nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
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
									new GWert(fsDaten.getData(), "q" + attName), //$NON-NLS-1$
									new GWert(fsDaten.getData(), praefixKlein + attName)
							)
					);
				} catch (GueteException e) {
					gueteBerechenbar = false;
					LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
							" nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}				
			}
						
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
								
		if(nichtErmittelbarFehlerhaft){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			long ergebnis = Math.round((double)summe / (double)Q.getWertUnskaliert());
			
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(praefixGross + attName).getItem("Wert"), ergebnis)){ //$NON-NLS-1$
				qAnalyse.setWertUnskaliert(ergebnis);
				if(interpoliert){
					qAnalyse.setInterpoliert(true);
				}
				if(gueteBerechenbar){
					try {
						GWert gesamtGuete = GueteVerfahren.quotient(
								GueteVerfahren.summe(gueteProdukte.toArray(new GWert[0])),
								new GWert(analyseDatum, "Q" + attName) //$NON-NLS-1$
							);
						
						qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
						qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
					} catch (GueteException e) {
						LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
								" nicht berechenbar", e); //$NON-NLS-1$
						e.printStackTrace();
					}
				}				
			}else{
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}
		
		qAnalyse.kopiereInhaltNach(analyseDatum);
	}

	
	/**
	 * Berechnet <code>B</code> und <code>BMax</code> analog SE-02.00.00.00.00-AFo-4.0 S.118f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	private final void berechneBelegung(Data analyseDatum){
		MesswertUnskaliert BAnalyse = new MesswertUnskaliert("B"); //$NON-NLS-1$
		MesswertUnskaliert BMaxAnalyse = new MesswertUnskaliert("BMax"); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		long BMax = -1;
		GWert gueteBMax = null;
		double bSumme = 0;
		ArrayList<GWert> gueteWerte = new ArrayList<GWert>();
		for(ResultData fsDatum:this.aktuelleFSAnalysen.values()){
			DaMesswertUnskaliert bFs = new DaMesswertUnskaliert("b", fsDatum.getData()); //$NON-NLS-1$
			
			if(bFs.getWertUnskaliert() >= 0){
				bSumme += bFs.getWertUnskaliert();
								
				if(bFs.isPlausibilisiert()){
					interpoliert = true;
				}
				GWert guete = null;
				guete = new GWert(fsDatum.getData(), "b"); //$NON-NLS-1$
				gueteWerte.add(guete);

				/**
				 * BMax ermitteln
				 */
				if(bFs.getWertUnskaliert() > BMax){
					BMax = bFs.getWertUnskaliert();
					gueteBMax = guete;
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
			}				
		}
		
		/**
		 * B setzen
		 */
		long B = Math.round(bSumme / (double)this.aktuelleFSAnalysen.keySet().size());
		if(!nichtErmittelbarFehlerhaft &&	
		    DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("B").getItem("Wert"), B)){ //$NON-NLS-1$ //$NON-NLS-2$
			BAnalyse.setWertUnskaliert(B);
			if(interpoliert){
				BAnalyse.setInterpoliert(true);
			}
			if(gueteBerechenbar){
				try {
					GWert gesamtGuete = GueteVerfahren.summe(gueteWerte.toArray(new GWert[0]));
					BAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
					BAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer B nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}
			}				
		}else{
			BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		
		/**
		 * BMax setzen
		 */
		if(BMax >= 0 &&
		   DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("BMax").getItem("Wert"), BMax)){ //$NON-NLS-1$ //$NON-NLS-2$
			BMaxAnalyse.setWertUnskaliert(BMax);
			if(interpoliert){
				BMaxAnalyse.setInterpoliert(true);
			}
			if(gueteBMax != null){
				BMaxAnalyse.getGueteIndex().setWert(gueteBMax.getIndexUnskaliert());
				BMaxAnalyse.setVerfahren(gueteBMax.getVerfahren().getCode());
			}
		}else{
			BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		
		BAnalyse.kopiereInhaltNach(analyseDatum);
		BMaxAnalyse.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet <code>SKfz</code> analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	private final void berechneStandardabweichung(Data analyseDatum){
		MesswertUnskaliert SKfzAnalyse = new MesswertUnskaliert("SKfz"); //$NON-NLS-1$
		
		MesswertUnskaliert QKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$
		GWert QKfzGuete = new GWert(analyseDatum, "QKfz");  //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		if(QKfz.getWertUnskaliert() > 1){
			double sKfzWertOhneWurzel = 0;
			
			DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$
			GWert VKfzGuete = new GWert(analyseDatum, "VKfz"); //$NON-NLS-1$
			interpoliert = VKfz.isInterpoliert() || QKfz.isInterpoliert(); 
				
			if(VKfz.getWertUnskaliert() >= 0){
				double VKfzWert = VKfz.getWertUnskaliert();
				List<GWert> summanden = new ArrayList<GWert>();
				
				for(ResultData fsDatum:this.aktuelleFSAnalysen.values()){
					DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", fsDatum.getData()); //$NON-NLS-1$
					DaMesswertUnskaliert sKfz = new DaMesswertUnskaliert("sKfz", fsDatum.getData()); //$NON-NLS-1$
					DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("vKfz", fsDatum.getData()); //$NON-NLS-1$

					interpoliert |= qKfz.isPlausibilisiert() || 
								    sKfz.isPlausibilisiert() ||
								    vKfz.isPlausibilisiert();
					
					if(qKfz.getWertUnskaliert() >= 0 &&
					   sKfz.getWertUnskaliert() >= 0 &&
					   vKfz.getWertUnskaliert() >= 0){
						double qKfzWert = qKfz.getWertUnskaliert();
						double sKfzWert = sKfz.getWertUnskaliert();
						double vKfzWert = vKfz.getWertUnskaliert();
												
						/**
						 * Berechnung
						 */
						sKfzWertOhneWurzel += 
							(qKfzWert * Math.pow(sKfzWert, 2.0) + qKfzWert * Math.pow(vKfzWert - VKfzWert, 2.0)) / 
							((double)QKfz.getWertUnskaliert() - 1.0);
						
						/**
						 * Guete
						 */
						GWert qKfzGuete = new GWert(fsDatum.getData(), "qKfz"); //$NON-NLS-1$
						GWert sKfzGuete = new GWert(fsDatum.getData(), "sKfz"); //$NON-NLS-1$
						GWert vKfzGuete = new GWert(fsDatum.getData(), "vKfz"); //$NON-NLS-1$
						
						try {
							summanden.add(
								GueteVerfahren.quotient(
										GueteVerfahren.summe(
												GueteVerfahren.produkt(
														qKfzGuete,
														GueteVerfahren.exp(sKfzGuete, 2.0)
												),
												GueteVerfahren.produkt(
														qKfzGuete,
														GueteVerfahren.exp(
																GueteVerfahren.differenz(
																		vKfzGuete,
																		VKfzGuete
																), 2.0)
												)
										),
										QKfzGuete));
						} catch (GueteException e) {
							gueteBerechenbar = false;
							LOGGER.error("Guete-Index fuer SKfz nicht berechenbar", e); //$NON-NLS-1$
							e.printStackTrace();
						}						
						
					}else{
						nichtErmittelbarFehlerhaft = true;
						break;
					}
				}
				
				if(!nichtErmittelbarFehlerhaft){
					if(sKfzWertOhneWurzel >= 0){
						long SKfz = Math.round(Math.sqrt(sKfzWertOhneWurzel));
						
					    if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("SKfz").getItem("Wert"), SKfz)){ //$NON-NLS-1$ //$NON-NLS-2$
							SKfzAnalyse.setWertUnskaliert(SKfz);
							if(interpoliert){
								SKfzAnalyse.setInterpoliert(true);
							}
							if(gueteBerechenbar){
								try {
									GWert gesamtGuete = GueteVerfahren.exp(
											GueteVerfahren.summe(summanden.toArray(new GWert[0])), 0.5);
									
									SKfzAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
									SKfzAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
								} catch (GueteException e) {
									LOGGER.error("Guete-Index fuer SKfz nicht berechenbar", e); //$NON-NLS-1$
									e.printStackTrace();
								}
							}				
					    }else{
					    	nichtErmittelbarFehlerhaft = true;
					    }
					}else{
						nichtErmittelbarFehlerhaft = true;
					}
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else
		if(QKfz.getWertUnskaliert() >= 0){
			SKfzAnalyse.setWertUnskaliert(0);
			SKfzAnalyse.setInterpoliert(QKfz.isInterpoliert());
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
		
		if(nichtErmittelbarFehlerhaft){
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}

		SKfzAnalyse.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet (<code>ALkw</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneLkwAnteil(Data analyseDatum){
		MesswertUnskaliert ALkwAnalyse = new MesswertUnskaliert("ALkw"); //$NON-NLS-1$
		MesswertUnskaliert QLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert QKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
				
		long ALkwWert = -1;
		GWert ALkwGuete = null;
		if(QLkw.getWertUnskaliert() >= 0 && QKfz.getWertUnskaliert() > 0){
			ALkwWert = Math.round((double)QLkw.getWertUnskaliert() / (double)QKfz.getWertUnskaliert()) * 100;
			
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("ALkw").getItem("Wert"), ALkwWert)){ //$NON-NLS-1$ //$NON-NLS-2$
				try {
					ALkwGuete = GueteVerfahren.quotient(
							new GWert(analyseDatum, "QLkw"), //$NON-NLS-1$
							new GWert(analyseDatum, "QKfz") //$NON-NLS-1$
						);
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer ALkw nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}		
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
				
		if(nichtErmittelbarFehlerhaft){
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			ALkwAnalyse.setWertUnskaliert(ALkwWert);
			ALkwAnalyse.setInterpoliert(QLkw.isInterpoliert() || QKfz.isInterpoliert());
			if(ALkwGuete != null){
				ALkwAnalyse.getGueteIndex().setWert(ALkwGuete.getIndexUnskaliert());
				ALkwAnalyse.setVerfahren(ALkwGuete.getVerfahren().getCode());				
			}
		}
		
		ALkwAnalyse.kopiereInhaltNach(analyseDatum);
	}

	
	/**
	 * Berechnet die Verkehrsstärken (<code>Kxxx</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param attName der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	protected final void berechneDichte(Data analyseDatum, final String attName){
		MesswertUnskaliert KAnalyse = new MesswertUnskaliert("K" + attName); //$NON-NLS-1$
		MesswertUnskaliert Q = new MesswertUnskaliert("Q" + attName, analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert V = new MesswertUnskaliert("V" + attName, analyseDatum); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
				
		long KWert = -1;
		GWert KGuete = null;
		if(V.getWertUnskaliert() > 0){
			if(Q.getWertUnskaliert() >= 0){
				KWert = Math.round((double)Q.getWertUnskaliert() / (double)V.getWertUnskaliert());
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("K" + attName).getItem("Wert"), KWert)){  //$NON-NLS-1$//$NON-NLS-2$
					interpoliert = Q.isInterpoliert() || V.isInterpoliert();
					
					try {
						KGuete = GueteVerfahren.quotient(
									new GWert(analyseDatum, "Q" + attName), //$NON-NLS-1$
									new GWert(analyseDatum, "V" + attName) //$NON-NLS-1$
								 );
					} catch (GueteException e) {
						LOGGER.error("Guete-Index fuer K" + attName + " nicht berechenbar", e); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
				}else{
					nichtErmittelbarFehlerhaft = true;
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else
		if(V.getWertUnskaliert() <= 0 &&
		   this.parameter.isInitialisiert() &&
		   this.letztesErgebnis != null &&
		   this.letztesErgebnis.getData() != null){
			long grenz = -1;
			long max = -1;
			
			if(attName.startsWith("K")){	// Kfz //$NON-NLS-1$
				grenz = parameter.getKKfzGrenz();
				max = parameter.getKKfzMax();
			}else
			if(attName.startsWith("L")){	// Lkw //$NON-NLS-1$
				grenz = parameter.getKLkwGrenz();
				max = parameter.getKLkwMax();				
			}else{	// Pkw
				grenz = parameter.getKPkwGrenz();
				max = parameter.getKPkwMax();
			}
			
			MesswertUnskaliert KTMinus1 = new MesswertUnskaliert("K" + attName, this.letztesErgebnis.getData());  //$NON-NLS-1$
			if(KTMinus1.getWertUnskaliert() >= 0){
				if(KTMinus1.getWertUnskaliert() < grenz){
					KWert = 0;
				}else{
					KWert = max;
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
						
		if(nichtErmittelbarFehlerhaft){
			KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			KAnalyse.setWertUnskaliert(KWert);
			KAnalyse.setInterpoliert(interpoliert);
			if(KGuete != null){
				KAnalyse.getGueteIndex().setWert(KGuete.getIndexUnskaliert());
				KAnalyse.setVerfahren(KGuete.getVerfahren().getCode());				
			}
		}
		
		KAnalyse.kopiereInhaltNach(analyseDatum);
	}

	
	/**
	 * Berechnet die Bemessungsverkehrsstaerke (<code>QB</code>) analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsVerkehrsstaerke(Data analyseDatum){
		MesswertUnskaliert QBAnalyse = new MesswertUnskaliert("QB"); //$NON-NLS-1$
		MesswertUnskaliert VPkw = new MesswertUnskaliert("VPkw", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert VLkw = new MesswertUnskaliert("VLkw", analyseDatum); //$NON-NLS-1$	
		MesswertUnskaliert QPkw = new MesswertUnskaliert("QPkw", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert QLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		
		long QBWert = -1;
		GWert QBGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;
		
		if(VLkw.getWertUnskaliert() >= 0 && VPkw.getWertUnskaliert() >= 0 &&
		   QLkw.getWertUnskaliert() >= 0 && QPkw.getWertUnskaliert() >= 0 &&
		   this.parameter.isInitialisiert()){
			double k1 = this.parameter.getFlk1();
			double k2 = this.parameter.getFlk2();
						
			double fL;
			if(VPkw.getWertUnskaliert() <= VLkw.getWertUnskaliert()){
				fL = k1;
			}else{
				fL = k1 + k2 * (VPkw.getWertUnskaliert() - VLkw.getWertUnskaliert());
			}
			
			QBWert = QPkw.getWertUnskaliert() + Math.round(fL * (double)QLkw.getWertUnskaliert());
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("QB").getItem("Wert"), QBWert)){  //$NON-NLS-1$//$NON-NLS-2$
				GWert QPkwGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$
				GWert QLkwGuete = new GWert(analyseDatum, "QLkw"); //$NON-NLS-1$					
								
				try {
					QBGuete = GueteVerfahren.summe(QPkwGuete, GueteVerfahren.gewichte(QLkwGuete, fL));
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer QB nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}					
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
						
		if(nichtErmittelbarFehlerhaft){
			QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			QBAnalyse.setWertUnskaliert(QBWert);
			QBAnalyse.setInterpoliert(QPkw.isInterpoliert() || QLkw.isInterpoliert());
			if(QBGuete != null){
				QBAnalyse.getGueteIndex().setWert(QBGuete.getIndexUnskaliert());
				QBAnalyse.setVerfahren(QBGuete.getVerfahren().getCode());				
			}
		}
		
		QBAnalyse.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet die Bemessungsdichte (<code>KB</code>) analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsdichte(Data analyseDatum){
		MesswertUnskaliert KBAnalyse = new MesswertUnskaliert("KB"); //$NON-NLS-1$
		MesswertUnskaliert QB = new MesswertUnskaliert("QB", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert VKfz = new MesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$	
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
				
		long KBWert = -1;
		GWert KBGuete = null;
		if(VKfz.getWertUnskaliert() > 0){
			if(QB.getWertUnskaliert() >= 0){
				KBWert = Math.round((double)QB.getWertUnskaliert() / (double)VKfz.getWertUnskaliert());
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("KB").getItem("Wert"), KBWert)){  //$NON-NLS-1$//$NON-NLS-2$
					interpoliert = QB.isInterpoliert() || VKfz.isInterpoliert();
					try {
						KBGuete = GueteVerfahren.quotient(
										new GWert(analyseDatum, "QB"),  //$NON-NLS-1$
										new GWert(analyseDatum, "VKfz")  //$NON-NLS-1$
								  );
					} catch (GueteException e) {
						LOGGER.error("Guete-Index fuer KB nicht berechenbar", e); //$NON-NLS-1$
						e.printStackTrace();
					}
				}else{
					nichtErmittelbarFehlerhaft = true;
				}				
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else{
			if(this.parameter.isInitialisiert() && this.letztesErgebnis != null && this.letztesErgebnis.getData() != null){
				MesswertUnskaliert KBTMinus1 = new MesswertUnskaliert("KB", this.letztesErgebnis.getData()); //$NON-NLS-1$
				if(KBTMinus1.getWertUnskaliert() >= 0){
					if(KBTMinus1.getWertUnskaliert() >= this.parameter.getKBGrenz()){
						KBWert = this.parameter.getKBMax();
					}else{
						KBWert = 0;
					}
				}else{
					nichtErmittelbarFehlerhaft = true;
				}				
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}			
		
		if(nichtErmittelbarFehlerhaft){
			KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			KBAnalyse.setWertUnskaliert(KBWert);
			KBAnalyse.setInterpoliert(interpoliert);
			if(KBGuete != null){
				KBAnalyse.getGueteIndex().setWert(KBGuete.getIndexUnskaliert());
				KBAnalyse.setVerfahren(KBGuete.getVerfahren().getCode());				
			}
		}
		
		KBAnalyse.kopiereInhaltNach(analyseDatum);
	}

	
	/**
	 * Berechnet die gewichtete Differenzgeschwindigkeit (<code>VDelta</code>) im Messquerschnitt 
	 * analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	private final void berechneVDifferenz(Data analyseDatum){
		MesswertUnskaliert VDeltaAnalyse = new MesswertUnskaliert("VDelta"); //$NON-NLS-1$
		
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechnen = true;

		long VDeltaWert = 0;
		List<GWert> gueteSummanden = new ArrayList<GWert>();
		if(this.parameter.isInitialisiert()){
			MessQuerschnitt mq = MessQuerschnitt.getInstanz(this.messQuerschnitt);
			if(mq != null){
				for(int i = 0; i<mq.getFahrStreifen().size() - 1; i++){
					int w;
					if(this.parameter.getWichtung().length > i){
						w = this.parameter.getWichtung()[i];
					}else{
						nichtErmittelbarFehlerhaft = true;
						break;
					}
					ResultData fsResultI = this.aktuelleFSAnalysen.get(mq.getFahrStreifen().get(i).getSystemObject());
					ResultData fsResultIPlus1 = this.aktuelleFSAnalysen.get(mq.getFahrStreifen().get(i+1).getSystemObject());
					MesswertUnskaliert vKfzI = new MesswertUnskaliert("vKfz", fsResultI.getData()); //$NON-NLS-1$
					MesswertUnskaliert vKfzIPlus1 = new MesswertUnskaliert("vKfz", fsResultIPlus1.getData()); //$NON-NLS-1$
					
					if(vKfzI.getWertUnskaliert() >= 0 && vKfzIPlus1.getWertUnskaliert() >= 0){
						interpoliert = vKfzI.isInterpoliert() || vKfzIPlus1.isInterpoliert();
						VDeltaWert += w * Math.abs(vKfzI.getWertUnskaliert() - vKfzIPlus1.getWertUnskaliert());
						
						try {
							gueteSummanden.add(
									GueteVerfahren.gewichte(
									GueteVerfahren.differenz(
											new GWert(fsResultI.getData(), "vKfz"), //$NON-NLS-1$
											new GWert(fsResultIPlus1.getData(), "vKfz")), (double)w)); //$NON-NLS-1$
						} catch (GueteException e) {
							gueteBerechnen = false;
							LOGGER.error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
							e.printStackTrace();
						}
						
					}else{
						nichtErmittelbarFehlerhaft = true;
						break;
					}
				}	
				
				if(!DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("VDelta").getItem("Wert"), VDeltaWert)){  //$NON-NLS-1$//$NON-NLS-2$
					nichtErmittelbarFehlerhaft = true;
				}
			}else{
				nichtErmittelbarFehlerhaft = true;
			}
		}else{
			nichtErmittelbarFehlerhaft = true;
		}
		
		if(nichtErmittelbarFehlerhaft){
			VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			VDeltaAnalyse.setWertUnskaliert(VDeltaWert);
			VDeltaAnalyse.setInterpoliert(interpoliert);
			if(gueteBerechnen){
				try {
					GWert guete = GueteVerfahren.summe(gueteSummanden.toArray(new GWert[0]));
					VDeltaAnalyse.getGueteIndex().setWert(guete.getIndexUnskaliert());
					VDeltaAnalyse.setVerfahren(guete.getVerfahren().getCode());				
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
		
		VDeltaAnalyse.kopiereInhaltNach(analyseDatum);
	}

}
