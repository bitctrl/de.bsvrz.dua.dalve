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
package de.bsvrz.dua.dalve.mq;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.mq.atg.AtgVerkehrsDatenKurzZeitAnalyseMq;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.ObjektWecker;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IObjektWeckerListener;

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der Analysewerte
 * eines virtuellen Messquerschnitts notwendig sind gespeichert. 
 * 
 * 
 * TODO: Jedes mit dem MQ assoziierte 
 * Fahrstreifendatum muss durch dieses Objekt (Methode <code>trigger(..)</code>) geleitet
 * werden um ggf. auch eine neue Berechnung von Analysewerten auszul�sen.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DaAnalyseMessQuerschnittVirtuell 
extends DaAnalyseMessQuerschnitt
implements IObjektWeckerListener{
	
	/**
	 * Informiert dieses Objekt dar�ber, dass das Timeout
	 * f�r die Berechnung der Analysedaten abgelaufen ist
	 */
	private static final ObjektWecker WECKER = new ObjektWecker();
	
	/**
	 * Mapt alle hier betrachteten Messquerschnitte auf das letzte
	 * von ihnen empfangene Analysedatum
	 */
	private Map<SystemObject, ResultData> aktuelleMQAnalysen = new HashMap<SystemObject, ResultData>();
	
	/**
	 * zeigt an, ob der Wecker zur Alarmierung bei Timeout eingeschaltet ist
	 */
	private boolean weckerGestellt = false;
	
	/**
	 * Alle MQ, die auf der Hauptfahrbahn liegen
	 */
	private Collection<SystemObject> mqAufHauptfahrbahn = new HashSet<SystemObject>();
		
	/**
	 * der aufgel��te virtuelle Messquerschnitt
	 */
	private MessQuerschnittVirtuell mqv = null;
	
	/**
	 * Zeigt an, ob der Messquerschnitt <b>vor</b> der Anschlussstelle, die durch diesen 
	 * virtuellen MQ repr�sentiert wird, direkt erfasst ist
	 */
	private boolean mqVorErfasst = false;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>nach</b> der Anschlussstelle, die durch diesen 
	 * virtuellen MQ repr�sentiert wird, direkt erfasst ist
	 */
	private boolean mqNachErfasst = false;
	
	/**
	 * Zeigt an, ob der Messquerschnitt <b>mittig</b> der Anschlussstelle, die durch diesen 
	 * virtuellen MQ repr�sentiert wird, direkt erfasst ist
	 */
	private boolean mqMitteErfasst = false;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Einfahrt</b> der Anschlussstelle, die durch diesen 
	 * virtuellen MQ repr�sentiert wird, direkt erfasst ist
	 */
	private boolean mqEinfahrtErfasst = false;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Ausfahrt</b> der Anschlussstelle, die durch diesen 
	 * virtuellen MQ repr�sentiert wird, direkt erfasst ist
	 */
	private boolean mqAusfahrtErfasst = false;
	
	
	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zur�ck.
	 * Nach dieser Initialisierung ist das Objekt auf alle Daten (seiner assoziierten
	 * Messquerschnitte) angemeldet und analysiert ggf. Daten 
	 * 
	 * @param analyseModul Verbindung zum Analysemodul (zum Publizieren)
	 * @param messQuerschnittVirtuell der virtuelle Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException wenn die Konfigurationsdaten des virtuellen 
	 * MQs nicht vollst�ndig ausgelesen werden konnte, oder die Analysedaten f�r den
	 * virtuellen MQ nicht errechnet werden k�nnten (aufgrund fehlender MQ-Referenzen) 
	 */
	public DaAnalyseMessQuerschnittVirtuell initialisiere(MqAnalyseModul analyseModul, 
														  SystemObject messQuerschnittVirtuell)
	throws DUAInitialisierungsException{
		if(MQ_ANALYSE == null){
			MQ_ANALYSE = analyseModul;
		}
		this.messQuerschnitt = messQuerschnittVirtuell;
		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(MQ_ANALYSE.getDav(), messQuerschnitt);

		mqv = MessQuerschnittVirtuell.getInstanz(messQuerschnitt);
		if(mqv != null){
			MessQuerschnitt mqVor = mqv.getMQVor();
			if(mqVor != null){
				this.mqVorErfasst = true;
				this.aktuelleMQAnalysen.put(mqVor.getSystemObject(), null);
				this.mqAufHauptfahrbahn.add(mqVor.getSystemObject());
			}
			
			MessQuerschnitt mqMitte = mqv.getMQMitte();
			if(mqMitte != null){
				this.mqMitteErfasst = true;
				this.aktuelleMQAnalysen.put(mqMitte.getSystemObject(), null);
				this.mqAufHauptfahrbahn.add(mqMitte.getSystemObject());
			}

			MessQuerschnitt mqNach = mqv.getMQNach();
			if(mqNach != null){
				this.mqNachErfasst = true;
				this.aktuelleMQAnalysen.put(mqNach.getSystemObject(), null);
				this.mqAufHauptfahrbahn.add(mqNach.getSystemObject());
			}

			if(this.mqAufHauptfahrbahn.size() == 0){
				throw new DUAInitialisierungsException("Auf der Hauptfahrbahn des " + //$NON-NLS-1$
						"virtuellen MQ sind keine MQ referenziert"); //$NON-NLS-1$
			}
			
			MessQuerschnitt mqEin = mqv.getMQEinfahrt();
			if(mqEin != null){
				this.mqEinfahrtErfasst = true;
				this.aktuelleMQAnalysen.put(mqEin.getSystemObject(), null);
			}
			
			MessQuerschnitt mqAus = mqv.getMQAusfahrt();
			if(mqAus != null){
				this.mqAusfahrtErfasst = true;
				this.aktuelleMQAnalysen.put(mqAus.getSystemObject(), null);
			}
			
			MQ_ANALYSE.getDav().subscribeReceiver(
					this,
					this.aktuelleMQAnalysen.keySet(), 
					new DataDescription(
							MQ_ANALYSE.getDav().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
							MQ_ANALYSE.getDav().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
							(short)0),
					ReceiveOptions.normal(),
					ReceiverRole.receiver());			
		}else{
			throw new DUAInitialisierungsException("MQV-Konfiguration von " + messQuerschnittVirtuell +  //$NON-NLS-1$
				" konnte nicht vollst�ndig ausgelesen werden"); //$NON-NLS-1$
		}
			
		return this;
	}
	
	
	/**
	 * Dieser Methode sollten alle aktuellen Daten f�r alle mit diesem Messquerschnitt
	 * assoziierten Fahrstreifen �bergeben werden. Ggf. wird dadurch dann eine Berechnung
	 * der Analysewerte dieses Messquerschnittes ausgel�st.
	 * 
	 * @param triggerDatum ein Analyse-Datum eines assoziierten Messquerschnitts
	 * @return ein Analysedatum f�r diesen virtuellen Messquerschnitt, wenn das <code>triggerDatum</code>
	 * eine Berechnung ausgel�st hat, oder <code>null</code> sonst
	 */
	@Override
	public ResultData trigger(ResultData triggerDatum){
		ResultData ergebnis = null;
		this.aktuelleMQAnalysen.put(triggerDatum.getObject(), triggerDatum);
						
		if(this.isKeineDaten()){
			ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG,
							triggerDatum.getDataTime(), null);
		}else{	
			if(triggerDatum.getData() != null){
				if(this.isAlleDatenVollstaendig()){
					ergebnis = this.getErgebnisAufBasisAktuellerDaten();
				}else{
					/**
					 * Ggf. Timeout einstellen
					 */
					if(!this.weckerGestellt){
						long T = triggerDatum.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
						long timeoutZeitStempel = triggerDatum.getDataTime() + T + T/2;
						WECKER.setWecker(this, timeoutZeitStempel);
						this.weckerGestellt = true;
					}
				}
			}
		}
				
		return ergebnis;
	}
	
	
	/**
	 * Ermittelt, ob dieser virtuelle Messquerschnitt zur Zeit auf <code>keine Daten</code>
	 * stehen sollte.<br>
	 * Dies ist dann der Fall, wenn f�r keinen Messquerschnitt auf der Hauptfahrbahn Nutzdaten
	 * zur Verf�gung stehen oder wenn die Nutzdaten mit unterschiedlichen Intervallenl�ngen eintreffen
	 * 
	 * @return  ob dieser virtuelle Messquerschnitt zur Zeit auf <code>keine Daten</code>
	 * stehen sollte
	 */
	private final boolean isKeineDaten(){
		/**
		 * Zaehlt die Anzahl der Nutz-Datensaetze fuer MQ auf der Hauptfahrbahn
		 */
		int datenZaehlerFuerHauptFahrbahn = 0;
		long intervallLaenge = -1;
		
		for(SystemObject mq:this.aktuelleMQAnalysen.keySet()){
			ResultData aktuellesMQDatum = this.aktuelleMQAnalysen.get(mq);
			if(aktuellesMQDatum != null && aktuellesMQDatum.getData() != null){
				if(this.mqAufHauptfahrbahn.contains(aktuellesMQDatum.getObject())){
					datenZaehlerFuerHauptFahrbahn++;
				}
				long T = aktuellesMQDatum.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
				if(intervallLaenge < 0){
					intervallLaenge = T;					
				}else{
					if(intervallLaenge != T){
						datenZaehlerFuerHauptFahrbahn = 0;
						break;
					}
				}
			}
		}
		
		return datenZaehlerFuerHauptFahrbahn > 0;
	}
	
	
	/**
	 * Erfragt, ob von den MQ, die an diesem virtuellen MQ erfasst sind, alle
	 * ein Datum mit Nutzdaten geliefert haben, dessen Zeitstempel sp�ter als
	 * der des letzten hier errechneten Analysedatums ist
	 * 
	 * @return ob alle Daten zur Berechnung eines neuen Intervalls da sind
	 */
	protected final boolean isAlleDatenVollstaendig(){
		boolean alleDatenVollstaendig = true;
		
		long letzteBerechnungsZeit = this.letztesErgebnis == null?-1:this.letztesErgebnis.getDataTime();
		
		if(this.mqVorErfasst){
			ResultData vorData = this.aktuelleMQAnalysen.get(this.mqv.getMQVor().getSystemObject());
			alleDatenVollstaendig &= vorData != null &&
									 vorData.getData() != null &&
									 vorData.getDataTime() > letzteBerechnungsZeit;
		}
		if(alleDatenVollstaendig && this.mqNachErfasst){
			ResultData nachData = this.aktuelleMQAnalysen.get(this.mqv.getMQNach().getSystemObject());
			alleDatenVollstaendig &= nachData != null &&
									 nachData.getData() != null &&
									 nachData.getDataTime() > letzteBerechnungsZeit;
		}
		if(alleDatenVollstaendig && this.mqMitteErfasst){
			ResultData mitteData = this.aktuelleMQAnalysen.get(this.mqv.getMQMitte().getSystemObject());
			alleDatenVollstaendig &= mitteData != null &&
									 mitteData.getData() != null &&
									 mitteData.getDataTime() > letzteBerechnungsZeit;
		}

		if(alleDatenVollstaendig && this.mqEinfahrtErfasst){
			ResultData einData = this.aktuelleMQAnalysen.get(this.mqv.getMQEinfahrt().getSystemObject());
			alleDatenVollstaendig &= einData != null &&
									 einData.getData() != null &&
									 einData.getDataTime() > letzteBerechnungsZeit;				
		}		
		if(alleDatenVollstaendig && this.mqAusfahrtErfasst){
			ResultData ausData = this.aktuelleMQAnalysen.get(this.mqv.getMQAusfahrt().getSystemObject());
			alleDatenVollstaendig &= ausData != null &&
									 ausData.getData() != null &&
									 ausData.getDataTime() > letzteBerechnungsZeit;				
		}		
				
		return alleDatenVollstaendig;
	}

	
	/**
	 * Diese Methode geht davon aus, dass keine weiteren Werte zur Berechnung des
	 * Analysedatums eintreffen werden und berechnet mit allen im Moment gepufferten
	 * Daten das Analysedatum 
	 *  
	 * @return ein Analysedatum 
	 */
	private final ResultData getErgebnisAufBasisAktuellerDaten(){
		ResultData ergebnis = null;
		
		Data analyseDatum = MQ_ANALYSE.getDav().createData(MqAnalyseModul.PUB_BESCHREIBUNG.getAttributeGroup());

		/**
		 * von diesem Datum wird der Zeitstempel und die Intervall�nge �bernommen
		 */
		ResultData referenzDatum = null;
		
		/**
		 * Ermittle Werte f�r <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
		 * <code>VDelta</code> via Ersetzung
		 */
		ResultData ersetzung = getErsatzDatum();
		referenzDatum = ersetzung;
		final String[] attErsetzung = new String[] {"VKfz", "VLkw", "VPkw", "VgKfz", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
													"B", "BMax", "SKfz", "VDelta"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		if(ersetzung == null || ersetzung.getData() == null){
			LOGGER.error("Es konnte kein Ersetzungsdatum fuer " + this.messQuerschnitt + //$NON-NLS-1$
					" in den Attributen (VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz und VDelta)" + //$NON-NLS-1$
					" ermittelt werden"); //$NON-NLS-1$
			for(String attName:attErsetzung){
				MesswertUnskaliert mw = new MesswertUnskaliert(attName);
				mw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				mw.kopiereInhaltNach(analyseDatum);
			}
		}else{
			for(String attName:attErsetzung){
				new MesswertUnskaliert(attName, ersetzung.getData()).kopiereInhaltNach(analyseDatum);
			}			
		}
		
		/**
		 * Ermittle Werte f�r <code>QKfz, QLkw</code> und <code>QPkw</code>
		 */
		
		
		/**
		 * Berechne Werte f�r <code>ALkw, KKfz, KPkw, KLkw, QB</code> und <code>KB</code>
		 */
		this.berechneLkwAnteil(analyseDatum);
		this.berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$
		this.berechneBemessungsVerkehrsstaerke(analyseDatum);
		this.berechneBemessungsdichte(analyseDatum);
		
		if(referenzDatum == null || referenzDatum.getData() == null){
			analyseDatum.getTimeValue("T").setMillis(referenzDatum.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$ //$NON-NLS-2$
			ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG, 
					referenzDatum.getDataTime(), analyseDatum);			
		}else{
			/**
			 * Notbremse
			 */
			ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG, 
					System.currentTimeMillis(), null);
		}		
		
		return ergebnis;
	}
	
	
	/**
	 * Erfragt das Ersatzdatum f�r diesen virtuellen Messquerschnitt in
	 * den Attributen <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
	 * <code>VDelta</code>
	 * 
	 * @return das Ersatzdatum f�r diesen virtuellen Messquerschnitt in
	 * den Attributen <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
	 * <code>VDelta</code> oder <code>null</code>, wenn dieses nicht ermittelt
	 * werden konnte, weil z.B. alle MQs erfasst sind (w�re ein Konfigurationsfehler)
	 */
	private final ResultData getErsatzDatum(){
		ResultData ersatzDatum = null;
		
		/**
		 * Es darf kein Datum zur�ckgegeben werden, dass �lter ist,
		 * als Das letzte von hier publizierte Analysedatum
		 */
		final long letzterAnalyseZeitStempel =
					this.letztesErgebnis == null?-1:this.letztesErgebnis.getDataTime();
		
		/**
		 * 1. MQVor nicht direkt erfasst
		 */
		if(!this.mqVorErfasst){
			ResultData mqDataMitte = this.aktuelleMQAnalysen.get(this.mqv.getMQMitte());
			
			if(mqDataMitte != null && mqDataMitte.getData() != null &&
			   mqDataMitte.getDataTime() > letzterAnalyseZeitStempel){
				ersatzDatum = mqDataMitte;
			}else{
				ersatzDatum = this.aktuelleMQAnalysen.get(this.mqv.getMQNach());
			}
		}
		
		/**
		 * Wenn Ersetzungsdatum noch nicht gefunden ist, mache weiter
		 */
		if(ersatzDatum == null || ersatzDatum.getData() == null || 
		   ersatzDatum.getDataTime() <= letzterAnalyseZeitStempel){
			
			/**
			 * 2. MQMitte nicht direkt erfasst
			 */
			if(!this.mqMitteErfasst){
				ResultData mqDataVor = this.aktuelleMQAnalysen.get(this.mqv.getMQVor());

				if(mqDataVor != null && mqDataVor.getData() != null &&
				   mqDataVor.getDataTime() > letzterAnalyseZeitStempel){
					ersatzDatum = mqDataVor;
				}else{
					ersatzDatum = this.aktuelleMQAnalysen.get(this.mqv.getMQNach());
				}			
			}
			
			if(ersatzDatum == null || ersatzDatum.getData() == null || 
			   ersatzDatum.getDataTime() <= letzterAnalyseZeitStempel){
				if(!this.mqNachErfasst){
					ResultData mqDataMitte = this.aktuelleMQAnalysen.get(this.mqv.getMQMitte());

					if(mqDataMitte != null && mqDataMitte.getData() != null &&
					   mqDataMitte.getDataTime() > letzterAnalyseZeitStempel){
						ersatzDatum = mqDataMitte;
					}else{
						ersatzDatum = this.aktuelleMQAnalysen.get(this.mqv.getMQVor());
					}			
				}
			}
		}
		
		assert(ersatzDatum != null &&
			   ersatzDatum.getData() != null &&
			   ersatzDatum.getDataTime() > letzterAnalyseZeitStempel);
		
		return ersatzDatum;
	}
	
	
	
	/**
	 * Erfragt den Zeitstempel, den ein Analysedatum bekommen muss, das
	 * auf Basis der jetzt im Puffer stehenden Ausgangsdatan berechnet wird
	 * 
	 * @return der Zeitstempel, den ein Analysedatum bekommen muss, das
	 * auf Basis der jetzt im Puffer stehenden Ausgangsdatan berechnet wird
	 */
	private final long getZeitStempelVonAktuellenDaten(){
		long zeitStempel = -1;
		
		for(ResultData aktuellesDatum:this.aktuelleMQAnalysen.values()){
			if(aktuellesDatum != null && aktuellesDatum.getData() != null){
				/**
				 * Dieses Datum wird zur Berechnung des Ausgangsdatums herangezogen
				 */
				long datenZeitStempel = aktuellesDatum.getDataTime();
				if(datenZeitStempel > zeitStempel){
					zeitStempel = datenZeitStempel;
				}
			}
		}
		
		assert(zeitStempel > 0);
		
		return zeitStempel;
	}


	
	
	/**
	 * {@inheritDoc}
	 */
	public void alarm() {
		this.publiziere(this.getErgebnisAufBasisAktuellerDaten());
	}
	
	
	/**
	 * Publiziert eine Analysedatum
	 * 
	 * @param ergebnis ein neu berechntes Analysedatum
	 */
	private final void publiziere(final ResultData ergebnis){
		if(ergebnis != null){
			if(ergebnis.getData() == null){
				/**
				 * Das folgende Flag zeigt an, ob dieser MQ zur Zeit auf
				 * "keine Daten" steht. Dies ist der Fall,<br>
				 * 1. wenn noch nie ein Datum f�r diesen MQ berechnet (versendet) wurde, oder<br>
				 * 2. wenn das letzte f�r diesen MQ berechnete (versendete) Datum keine Daten hatte.
				 */
				boolean aktuellKeineDaten = this.letztesErgebnis == null || this.letztesErgebnis.getData() == null; 
				
				if(!aktuellKeineDaten){
					MQ_ANALYSE.sendeDaten(ergebnis);
				}				
			}else{
				MQ_ANALYSE.sendeDaten(ergebnis);
			}
			
			WECKER.setWecker(this, ObjektWecker.AUS);
			this.weckerGestellt = false;
			this.letztesErgebnis = ergebnis;
		}		
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					this.publiziere(this.trigger(resultat));
				}
			}
		}
	}

}
