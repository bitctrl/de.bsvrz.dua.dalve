/**
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

import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
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
import de.bsvrz.sys.funclib.debug.Debug;

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
	 * alle Attributnamen der Atg <code>atg.verkehrsDatenKurzZeitMq</code>
	 */
	private static final String[] ATTS = new String[]{
			"QKfz", //$NON-NLS-1$
			"VKfz", //$NON-NLS-1$
			"QLkw", //$NON-NLS-1$
			"VLkw", //$NON-NLS-1$
			"QPkw", //$NON-NLS-1$
			"VPkw", //$NON-NLS-1$
			"B", //$NON-NLS-1$
			"BMax", //$NON-NLS-1$
			"SKfz", //$NON-NLS-1$
			"VgKfz", //$NON-NLS-1$
			"ALkw", //$NON-NLS-1$
			"KKfz", //$NON-NLS-1$
			"KLkw", //$NON-NLS-1$
			"KPkw", //$NON-NLS-1$
			"QB", //$NON-NLS-1$
			"KB", //$NON-NLS-1$
			"VDelta"}; //$NON-NLS-1$
		
	/**
	 * Verbindung zum Analysemodul
	 */
	protected static MqAnalyseModul MQ_ANALYSE = null;
	
	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt
	 */
	protected SystemObject messQuerschnitt = null;
	
	/**
	 * letztes für diesen Messquerschnitt errechnetes (veröffentlichtes) Ergebnis
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
	 * Alle aktuellen Fahrstreifenanalysen mit Nutzdaten
	 */
	private Map<SystemObject, ResultData> aktuelleFSAnalysenNutz = new HashMap<SystemObject, ResultData>();
	
	
	
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
					" konnte nicht vollstaendig ausgelesen werden"); //$NON-NLS-1$
		}

		if(this.aktuelleFSAnalysen.keySet().isEmpty()){
			Debug.getLogger().warning("Der MQ " + this.messQuerschnitt + " hat keine Fahrstreifen");  //$NON-NLS-1$//$NON-NLS-2$
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
						MQ_ANALYSE.getDav().getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
						MQ_ANALYSE.getDav().getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
						(short)0),
						ReceiveOptions.normal(),
						ReceiverRole.receiver());
	
		return this;
	}
	
	
	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem Messquerschnitt
	 * assoziierten Fahrstreifen uebergeben werden. Ggf. wird dadurch dann eine Berechnung
	 * der Analysewerte dieses Messquerschnittes ausgeluest.
	 * 
	 * @param triggerDatum ein KZ-Datum eines assoziierten Fahrstreifens
	 * @return ein Analysedatum fuer diesen Messquerschnitt, wenn das <code>triggerDatum</code>
	 * eine Berechnung ausgeloest hat, oder <code>null</code> sonst
	 */
	protected ResultData trigger(ResultData triggerDatum){
		ResultData ergebnis = null;
		this.aktuelleFSAnalysen.put(triggerDatum.getObject(), triggerDatum);
		
		/**
		 * Ein Analysedatum fuer den Fahrstreifen soll dann berechnet werden,
		 * wenn fuer alle Fahrstreifen, welche Nutzdaten haben (aber mindestenes einer)
		 * ein Datum mit dem gleichen Zeitstempel gekommen ist.
		 */
		this.aktuelleFSAnalysenNutz.clear();
		boolean berechne = false;
		long zeitStempel = -1;
		for(SystemObject fs:this.aktuelleFSAnalysen.keySet()){
			ResultData fsDatum = this.aktuelleFSAnalysen.get(fs);
			
			if(fsDatum != null){
				if(fsDatum.getData() != null){
					this.aktuelleFSAnalysenNutz.put(fsDatum.getObject(), fsDatum);
					if(zeitStempel == -1){
						/**
						 * erstes Datum
						 */
						zeitStempel = fsDatum.getDataTime();
						berechne = true;
					}else{
						/**
						 * Fuer den Fall, dass die Zeitstempel der Daten nicht uebereinstimmen,
						 * wird keine Daten veroeffentlicht
						 */
						if(fsDatum.getDataTime() != zeitStempel){
							ergebnis = new ResultData(
									this.messQuerschnitt,
									MqAnalyseModul.PUB_BESCHREIBUNG,
									System.currentTimeMillis(),
									null);
							berechne = false;
							break;
						}
					}
				}else{
					/**
					 * Wenn fuer mindestens einen Fahrstreifen keine Nutzdaten vorliegen,
					 * dann veroeffentliche <code>keine Daten</code> fuer den Messquerschnitt 
					 */
					ergebnis = new ResultData(
							this.messQuerschnitt,
							MqAnalyseModul.PUB_BESCHREIBUNG,
							System.currentTimeMillis(),
							null);
					berechne = false;
					break;					
				}
			}else{
				/**
				 * Wenn nicht fuer ALLE Fahrstreifen des Messquerschnittes 
				 * ein Datensatz vorliegt, dann mache nichts
				 */
				berechne = false;
				break;
			}
		}


		if(berechne){
			final long datenZeit = zeitStempel;
			Data analyseDatum = MQ_ANALYSE.getDav().createData(MqAnalyseModul.PUB_BESCHREIBUNG.getAttributeGroup());
			
			if(this.isErfassungsZyklenGleich(analyseDatum)){

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
			}

			ergebnis = new ResultData(this.messQuerschnitt, MqAnalyseModul.PUB_BESCHREIBUNG,
					datenZeit, analyseDatum);
			
			/**
			 * Puffer wieder zuruecksetzen
			 */
			for(SystemObject obj:this.aktuelleFSAnalysenNutz.keySet()){
				this.aktuelleFSAnalysen.put(obj, null);	
			}
			this.aktuelleFSAnalysenNutz.keySet().clear();
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
						if(ergebnis.getData() != null){
							MQ_ANALYSE.sendeDaten(ergebnis);
							this.letztesErgebnis = ergebnis;
						}else{
							if(this.letztesErgebnis != null && this.letztesErgebnis.getData() != null){
								MQ_ANALYSE.sendeDaten(ergebnis);
								this.letztesErgebnis = ergebnis;
							}
						}			
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
		Debug.getLogger().warning("Der MQ " + this.messQuerschnitt +  //$NON-NLS-1$
				" wird nicht mehr analysiert"); //$NON-NLS-1$
	}

	
	
	/**********************************************************************************
	 *                                                                                *
	 *                           Berechnungs-Methoden                                 *
	 *                                                                                *
	 **********************************************************************************/
	
	/**
	 * Erfragt, ob alle zur Verrechnung vorgesehenen Fahrstreifendatensaetze die gleiche
	 * Erfassungsintervalldauer haben
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @return ob alle zur Verrechnung vorgesehenen Fahrstreifendatensaetze die gleiche
	 * Erfassungsintervalldauer haben
	 */
	private final boolean isErfassungsZyklenGleich(Data analyseDatum){
		boolean gleich = false;
		String referenz = null;
		
		long T = -1;
		for(ResultData fsDaten:this.aktuelleFSAnalysenNutz.values()){
			if(T == -1){
				T = fsDaten.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
				gleich = true;
				referenz = fsDaten.toString(); 
			}else{
				if(T != fsDaten.getData().getTimeValue("T").getMillis()){ //$NON-NLS-1$
					gleich = false;
					Debug.getLogger().warning("Erfassungsintervalldauer nicht gleich:\n" +  //$NON-NLS-1$
							referenz + "\n" + fsDaten); //$NON-NLS-1$
					break;
				}
			}
		}
		
		/**
		 * Setzte alle Werte auf nicht ermittelbar bzw. fehlerhaft
		 */
		if(!gleich){
			for(String attribut:ATTS){
				MesswertUnskaliert mw = new MesswertUnskaliert(attribut);
				mw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				if(attribut.equals("VKfz") || attribut.equals("QPkw")){  //$NON-NLS-1$//$NON-NLS-2$
					mw.setNichtErfasst(true);
				}
				mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
			}
		}
		
		return gleich;
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
		/**
		 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet, so
		 * geht sie nicht in die jeweilige Berechnung des Zielwerts ein. Sind alle der bei der Berechnung
		 * beteiligten Größen als nicht ermittelbar gekennzeichnet, so wird der
		 * Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
		 */
		int istNichtVorhanden = 0;
		for(ResultData fsDaten:this.aktuelleFSAnalysenNutz.values()){
			MesswertUnskaliert fsWert = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$

			if(fsWert.isNichtErfasst()){
				qAnalyse.setNichtErfasst(true);
			}
			if(fsWert.isFehlerhaftBzwImplausibel()){
				nichtErmittelbarFehlerhaft = true;
				break;
			}else{
				if(fsWert.getWertUnskaliert() >= 0){
					summe += fsWert.getWertUnskaliert();
					interpoliert |= fsWert.isInterpoliert();
					gueteWerte.add(new GWert(fsDaten.getData(), "q" + attName)); //$NON-NLS-1$
				}else{
					istNichtVorhanden++;
				}
			}
		}
							
		if(nichtErmittelbarFehlerhaft){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			if(aktuelleFSAnalysenNutz.size() == istNichtVorhanden){
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}else{
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("Q" + attName).getItem("Wert"), summe)){  //$NON-NLS-1$//$NON-NLS-2$
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
							Debug.getLogger().error("Guete-Index fuer Q" + attName + //$NON-NLS-1$ 
									" nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}					
				}else{
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		}
		
		qAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
				
		MesswertUnskaliert Q = new MesswertUnskaliert("Q" + attName, analyseDatum); //$NON-NLS-1$
		
		if(Q.isFehlerhaftBzwImplausibel()){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else
		if(Q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		}else{
			boolean nichtErmittelbarFehlerhaft = false;
			boolean interpoliert = false;
			boolean gueteBerechenbar = true;
			long summe = 0;
			ArrayList<GWert> gueteProdukte = new ArrayList<GWert>();

			int istNichtVorhanden = 0;
			for(ResultData fsDaten:this.aktuelleFSAnalysenNutz.values()){
				MesswertUnskaliert q = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$
				MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName, fsDaten.getData());

				if(q.isFehlerhaftBzwImplausibel() ||
					v.isFehlerhaftBzwImplausibel()){
					nichtErmittelbarFehlerhaft = true;
					break;
				}else
				if(q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ||
					v.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
					istNichtVorhanden++;
				}else{				
					interpoliert |= q.isInterpoliert() || v.isInterpoliert();
					summe += q.getWertUnskaliert() * v.getWertUnskaliert();
					try {
						gueteProdukte.add(
								GueteVerfahren.produkt(
										new GWert(fsDaten.getData(), "q" + attName), //$NON-NLS-1$
										new GWert(fsDaten.getData(), praefixKlein + attName)
								)
						);
					} catch (GueteException e) {
						gueteBerechenbar = false;
						Debug.getLogger().error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
								" nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
						e.printStackTrace();
					}
				}
			}
			
			if(nichtErmittelbarFehlerhaft){
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}else{
				if(this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden){
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
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
								Debug.getLogger().error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$ 
										" nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}				
					}else{
						qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		for(ResultData fsDaten:this.aktuelleFSAnalysenNutz.values()){
			MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName, fsDaten.getData());
			if(v.isNichtErfasst()){
				qAnalyse.setNichtErfasst(true);
				break;
			}
		}

		qAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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

		long BMax = DUAKonstanten.NICHT_ERMITTELBAR;
		GWert gueteBMax = null;
		double bSumme = 0;
		ArrayList<GWert> gueteWerte = new ArrayList<GWert>();
		
		int istNichtVorhanden = 0;
		for(ResultData fsDatum:this.aktuelleFSAnalysenNutz.values()){
			DaMesswertUnskaliert bFs = new DaMesswertUnskaliert("b", fsDatum.getData()); //$NON-NLS-1$
			
			if(bFs.isFehlerhaftBzwImplausibel()){
				nichtErmittelbarFehlerhaft = true;
				break;
			}else
			if(bFs.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				istNichtVorhanden++;
			}else{
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
			}			
		}
		
		if(nichtErmittelbarFehlerhaft){
			BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			if(this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden){
				BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);				
			}else{
				/**
				 * B setzen
				 */
				long B = Math.round(bSumme / (double)this.aktuelleFSAnalysenNutz.keySet().size());
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("B").getItem("Wert"), B)){ //$NON-NLS-1$ //$NON-NLS-2$
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
							Debug.getLogger().error("Guete-Index fuer B nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}				
				}else{
					BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
				
				/**
				 * BMax setzen
				 */
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("BMax").getItem("Wert"), BMax)){ //$NON-NLS-1$ //$NON-NLS-2$
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
			}
		}
		
		BAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
		BMaxAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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

		boolean interpoliert = false;
		boolean gueteBerechenbar = true;
		
		if(QKfz.isFehlerhaftBzwImplausibel()){
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else
		if(QKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		}else{
			if(QKfz.getWertUnskaliert() > 1){
				double sKfzWertOhneWurzel = 0;
				
				DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$
				if(VKfz.isFehlerhaftBzwImplausibel()){
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}else
				if(VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				}else{
					GWert VKfzGuete = new GWert(analyseDatum, "VKfz"); //$NON-NLS-1$
					
					interpoliert = VKfz.isInterpoliert() || QKfz.isInterpoliert(); 
						
					double VKfzWert = VKfz.getWertUnskaliert();
					List<GWert> summanden = new ArrayList<GWert>();

					int istNichtVorhanden = 0;
					for(ResultData fsDatum:this.aktuelleFSAnalysenNutz.values()){
						DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", fsDatum.getData()); //$NON-NLS-1$
						DaMesswertUnskaliert sKfz = new DaMesswertUnskaliert("sKfz", fsDatum.getData()); //$NON-NLS-1$
						DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("vKfz", fsDatum.getData()); //$NON-NLS-1$
						
						if(qKfz.isFehlerhaftBzwImplausibel() || 
						   vKfz.isFehlerhaftBzwImplausibel() ||
						   sKfz.isFehlerhaftBzwImplausibel()){
							SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						}else
						if(qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || 
							vKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || 
							sKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
							istNichtVorhanden++;
						}else{
							interpoliert |= qKfz.isPlausibilisiert() || 
											sKfz.isPlausibilisiert() ||
											vKfz.isPlausibilisiert();

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
								Debug.getLogger().error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					if(this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden){
						SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					}else
					if(SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT){
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
										Debug.getLogger().error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
										e.printStackTrace();
									}
								}				
							}else{
								SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							}
						}else{
							SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						}
					}
				}				
			}else{
				SKfzAnalyse.setWertUnskaliert(0);
				SKfzAnalyse.setInterpoliert(QKfz.isInterpoliert());
			}	
		}		

		if(SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT){
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}

		SKfzAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
				
		if(QLkw.isFehlerhaftBzwImplausibel() || 
		   QKfz.isFehlerhaftBzwImplausibel()){
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else
		if(QLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ||
		   QKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		}else{
			GWert ALkwGuete = null;
			long ALkwWert = Math.round((double)QLkw.getWertUnskaliert() / (double)QKfz.getWertUnskaliert()) * 100;
				
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("ALkw").getItem("Wert"), ALkwWert)){ //$NON-NLS-1$ //$NON-NLS-2$
				try {
					ALkwGuete = GueteVerfahren.quotient(
							new GWert(analyseDatum, "QLkw"), //$NON-NLS-1$
							new GWert(analyseDatum, "QKfz") //$NON-NLS-1$
					);
				} catch (GueteException e) {
					Debug.getLogger().error("Guete-Index fuer ALkw nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
					e.printStackTrace();
				}
				
				ALkwAnalyse.setWertUnskaliert(ALkwWert);
				ALkwAnalyse.setInterpoliert(QLkw.isInterpoliert() || QKfz.isInterpoliert());
				if(ALkwGuete != null){
					ALkwAnalyse.getGueteIndex().setWert(ALkwGuete.getIndexUnskaliert());
					ALkwAnalyse.setVerfahren(ALkwGuete.getVerfahren().getCode());				
				}
			}else{
				ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}				
		
		ALkwAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
		
		if(Q.isFehlerhaftBzwImplausibel() ||
		   V.isFehlerhaftBzwImplausibel()){
			KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			if(V.getWertUnskaliert() == 0 ||
				V.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ){
				if(this.parameter.isInitialisiert() &&
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
					if(KTMinus1.isFehlerhaftBzwImplausibel()){
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}else
					if(KTMinus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					}else{
						if(KTMinus1.getWertUnskaliert() < grenz){
							KAnalyse.setWertUnskaliert(0);
						}else{
							KAnalyse.setWertUnskaliert(max);
						}
					}
				}else{
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			}else{
				if(Q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				}else{
					long KWert = Math.round((double)Q.getWertUnskaliert() / (double)V.getWertUnskaliert());
					if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("K" + attName).getItem("Wert"), KWert)){  //$NON-NLS-1$//$NON-NLS-2$
						boolean interpoliert = Q.isInterpoliert() || V.isInterpoliert();
						GWert KGuete = null;
						
						try {
							KGuete = GueteVerfahren.quotient(
										new GWert(analyseDatum, "Q" + attName), //$NON-NLS-1$
										new GWert(analyseDatum, "V" + attName) //$NON-NLS-1$
									 );
						} catch (GueteException e) {
							Debug.getLogger().error("Guete-Index fuer K" + attName + " nicht berechenbar", e); //$NON-NLS-1$ //$NON-NLS-2$
							e.printStackTrace();
						}
						
						KAnalyse.setWertUnskaliert(KWert);
						KAnalyse.setInterpoliert(interpoliert);
						if(KGuete != null){
							KAnalyse.getGueteIndex().setWert(KGuete.getIndexUnskaliert());
							KAnalyse.setVerfahren(KGuete.getVerfahren().getCode());	
						}
					}else{
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		KAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
		
		if(VPkw.isFehlerhaftBzwImplausibel() ||
			VLkw.isFehlerhaftBzwImplausibel() ||
			QPkw.isFehlerhaftBzwImplausibel() ||
			QLkw.isFehlerhaftBzwImplausibel()){
			QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else
		if(VPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ||
				VLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ||
				QPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR ||
				QLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		}else{
			if(this.parameter.isInitialisiert()){
				double k1 = this.parameter.getFlk1();
				double k2 = this.parameter.getFlk2();
							
				double fL;
				if(VPkw.getWertUnskaliert() <= VLkw.getWertUnskaliert()){
					fL = k1;
				}else{
					fL = k1 + k2 * (VPkw.getWertUnskaliert() - VLkw.getWertUnskaliert());
				}

				long QBWert = DUAKonstanten.NICHT_ERMITTELBAR;
				GWert QBGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;
				
				QBWert = QPkw.getWertUnskaliert() + Math.round(fL * (double)QLkw.getWertUnskaliert());
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("QB").getItem("Wert"), QBWert)){  //$NON-NLS-1$//$NON-NLS-2$
					GWert QPkwGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$
					GWert QLkwGuete = new GWert(analyseDatum, "QLkw"); //$NON-NLS-1$					
									
					try {
						QBGuete = GueteVerfahren.summe(QPkwGuete, GueteVerfahren.gewichte(QLkwGuete, fL));
					} catch (GueteException e) {
						Debug.getLogger().error("Guete-Index fuer QB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
						e.printStackTrace();
					}
					
					QBAnalyse.setWertUnskaliert(QBWert);
					QBAnalyse.setInterpoliert(QPkw.isInterpoliert() || QLkw.isInterpoliert());
					if(QBGuete != null){
						QBAnalyse.getGueteIndex().setWert(QBGuete.getIndexUnskaliert());
						QBAnalyse.setVerfahren(QBGuete.getVerfahren().getCode());				
					}
				}else{
					QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			}else{
				QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}							
		}
		
		QBAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
		
		if(QB.isFehlerhaftBzwImplausibel() || 
				VKfz.isFehlerhaftBzwImplausibel()){
			KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			if(VKfz.getWertUnskaliert() == 0 || 
			   VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				
				if(this.parameter.isInitialisiert() &&
					this.letztesErgebnis != null && 
					this.letztesErgebnis.getData() != null){
					
					MesswertUnskaliert KBTMinus1 = new MesswertUnskaliert("KB", this.letztesErgebnis.getData()); //$NON-NLS-1$
					if(KBTMinus1.getWertUnskaliert() >= 0){
						if(KBTMinus1.getWertUnskaliert() >= this.parameter.getKBGrenz()){
							KBAnalyse.setWertUnskaliert(this.parameter.getKBMax());
						}else{
							KBAnalyse.setWertUnskaliert(0);
						}
					}else{
						KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}					
				}else{
					KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
				
			}else{
				// normal berechnen
				if(QB.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
					KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				}else{
					long KBWert = Math.round((double)QB.getWertUnskaliert() / (double)VKfz.getWertUnskaliert());
					
					if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("KB").getItem("Wert"), KBWert)){  //$NON-NLS-1$//$NON-NLS-2$
						boolean interpoliert = QB.isInterpoliert() || VKfz.isInterpoliert();
						GWert KBGuete = null;
						try {
							KBGuete = GueteVerfahren.quotient(
											new GWert(analyseDatum, "QB"),  //$NON-NLS-1$
											new GWert(analyseDatum, "VKfz")  //$NON-NLS-1$
									  );
						} catch (GueteException e) {
							Debug.getLogger().error("Guete-Index fuer KB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
						
						KBAnalyse.setWertUnskaliert(KBWert);
						KBAnalyse.setInterpoliert(interpoliert);
						if(KBGuete != null){
							KBAnalyse.getGueteIndex().setWert(KBGuete.getIndexUnskaliert());
							KBAnalyse.setVerfahren(KBGuete.getVerfahren().getCode());				
						}
					}else{
						KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}					
				}
			}
		}
		
		KBAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	
	/**
	 * Berechnet die gewichtete Differenzgeschwindigkeit (<code>VDelta</code>) im Messquerschnitt 
	 * analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 */
	private final void berechneVDifferenz(Data analyseDatum){
		MesswertUnskaliert VDeltaAnalyse = new MesswertUnskaliert("VDelta"); //$NON-NLS-1$

		if(this.aktuelleFSAnalysen.size() <= 1){
			DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz"); //$NON-NLS-1$
			if(VKfz.isFehlerhaftBzwImplausibel()){
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}else
			if(VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}else{
				/**
				 * kopiere Wert
				 */
				VDeltaAnalyse.setWertUnskaliert(VKfz.getWertUnskaliert());
				VDeltaAnalyse.setFormalMax(VKfz.isFormalMax());
				VDeltaAnalyse.setFormalMin(VKfz.isFormalMin());
				VDeltaAnalyse.setGueteIndex(VKfz.getGueteIndex());
				VDeltaAnalyse.setVerfahren(VKfz.getVerfahren());
				VDeltaAnalyse.setLogischMax(VKfz.isLogischMax());				
				VDeltaAnalyse.setLogischMin(VKfz.isLogischMin());
				VDeltaAnalyse.setInterpoliert(VKfz.isInterpoliert());				
				VDeltaAnalyse.setNichtErfasst(VKfz.isNichtErfasst());
			}
		}else{
			boolean interpoliert = false;
			boolean gueteBerechnen = true;

			long VDeltaWert = 0;
			List<GWert> gueteSummanden = new ArrayList<GWert>();

			if(this.aktuelleFSAnalysen.keySet().size() == this.aktuelleFSAnalysenNutz.keySet().size() 
					&& this.parameter.isInitialisiert()){				
				MessQuerschnitt mq = MessQuerschnitt.getInstanz(this.messQuerschnitt);
				if(mq != null){

					int istNichtVorhanden = 0;
					for(int i = 0; i<mq.getFahrStreifen().size() - 1; i++){
						int w;
						if(this.parameter.getWichtung().length > i){
							w = this.parameter.getWichtung()[i];
						}else{
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						}
						ResultData fsResultI = this.aktuelleFSAnalysen.get(mq.getFahrStreifen().get(i).getSystemObject());
						ResultData fsResultIPlus1 = this.aktuelleFSAnalysen.get(mq.getFahrStreifen().get(i+1).getSystemObject());
						MesswertUnskaliert vKfzI = new MesswertUnskaliert("vKfz", fsResultI.getData()); //$NON-NLS-1$
						MesswertUnskaliert vKfzIPlus1 = new MesswertUnskaliert("vKfz", fsResultIPlus1.getData()); //$NON-NLS-1$

						if(vKfzI.isFehlerhaftBzwImplausibel() || 
							vKfzIPlus1.isFehlerhaftBzwImplausibel()){
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;								
						}else
							if(vKfzI.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || 
									vKfzIPlus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
								istNichtVorhanden++;
							}else{
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
									Debug.getLogger().error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
									e.printStackTrace();
								}
							}
					}	

					if(VDeltaAnalyse.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT){
						if(mq.getFahrStreifen().size() == istNichtVorhanden){
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						}else
							if(!DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("VDelta").getItem("Wert"), VDeltaWert)){  //$NON-NLS-1$//$NON-NLS-2$
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
										Debug.getLogger().error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
										e.printStackTrace();
									}
								}								
							}
					}						
				}else{
					VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}					
			}else{
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}						
		}
		
		VDeltaAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

}
