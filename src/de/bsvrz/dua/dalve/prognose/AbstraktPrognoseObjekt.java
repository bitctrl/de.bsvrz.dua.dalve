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
package de.bsvrz.dua.dalve.prognose;

import java.util.HashSet;
import java.util.Set;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Ueber dieses Objekt werden die Prognosedaten fuer <b>einen</b>
 * Fahrstreifen oder einen Messquerschnitt erstellt/publiziert
 * bzw. deren Erstellung verwaltet
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public abstract class AbstraktPrognoseObjekt 
implements ClientReceiverInterface,
		   ClientSenderInterface{

	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	private static ClientDavInterface DAV = null;
	
	/**
	 * Die Parameter, die die Erstellung der Daten steuern (alpha, beta, etc.)
	 */
	private AtgPrognoseParameter parameter = null; 
	
	/**
	 * Das Objekt, fuer das Prognosedaten und geglaettete Daten erstellt
	 * werden sollen (Fahrstreifen oder Messquerschnitt)
	 */
	private PrognoseSystemObjekt prognoseObjekt = null;
	
	/**
	 * Publikationsbeschreibung der geglaetteten Daten
	 */
	private DataDescription pubBeschreibungGlatt = null;
		
	/**
	 * Publikationsbeschreibung der Prognosedaten
	 */
	private DataDescription pubBeschreibungPrognose = null;
	
	/**
	 * zeigt an, ob dieses Objekt im Moment auf keine Daten steht
	 */
	private boolean aktuellKeineDaten = true;
	
	/**
	 * Sendesteuerung fuer Prognosedaten
	 */
	private boolean sendePrognoseDaten = false;
	
	/**
	 * Sendesteuerung fuer geglaettete Daten
	 */
	private boolean sendeGeglaetteteDaten = false;
	
	/**
	 * Fuer jedes zu berechnende Attribut (des Zieldatums) ein Prognoseobjekt
	 */
	private Set<DavAttributPrognoseObjekt> attributePuffer = 
									new HashSet<DavAttributPrognoseObjekt>();
	
	
	/**
	 * Initialisiert dieses Objekt. Nach Beendigung dieser Methode 
	 * empfängt und publiziert dieses Objekt Daten
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param prognoseObjekt das Prognoseobjekt, für das prognostiziert werden soll
	 * @throws DUAInitialisierungsException wenn die Sendeanmeldung fehlschlaegt
	 */
	public final void initialisiere(final ClientDavInterface dav, 
								    final PrognoseSystemObjekt prognoseObjekt)
	throws DUAInitialisierungsException{
		if(DAV == null){
			DAV = dav;			
		}
		this.prognoseObjekt = prognoseObjekt;
				
		/**
		 * Auf Parameter anmelden
		 */
		this.parameter = new AtgPrognoseParameter(DAV, prognoseObjekt, this.getPrognoseTyp());

		/**
		 * Alle Prognoseattribute initialisieren
		 */
		for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
			DavAttributPrognoseObjekt attributPrognose = new DavAttributPrognoseObjekt(prognoseObjekt, attribut);
			this.parameter.addListener(attributPrognose, attribut);
			this.attributePuffer.add(attributPrognose);
		}
		
		/**
		 * Sendeanmeldungen fuer Prognosewerte und geglaetttete Werte
		 */
		this.pubBeschreibungPrognose = new DataDescription(prognoseObjekt.getPubAtgPrognose(), 
														   this.getPrognoseTyp().getAspekt(), 
														   (short)0);
		this.pubBeschreibungGlatt = new DataDescription(prognoseObjekt.getPubAtgGlatt(), 
														this.getPrognoseTyp().getAspekt(), 
														(short)0);
		try {
			DAV.subscribeSender(this, prognoseObjekt.getObjekt(), this.pubBeschreibungPrognose, SenderRole.source());
			DAV.subscribeSender(this, prognoseObjekt.getObjekt(), this.pubBeschreibungGlatt, SenderRole.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
		}
		
		/**
		 * Impliziter Start des Objektes: Anmeldung auf Daten
		 */
		DAV.subscribeReceiver(this, prognoseObjekt.getObjekt(), 
				new DataDescription(prognoseObjekt.getAnalyseAtg(), 
									DAV.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
									(short)0),
									ReceiveOptions.normal(), ReceiverRole.receiver());
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					boolean datenSenden = true;
					Data glaettungNutzdaten = null;
					Data prognoseNutzdaten = null;

					try{
						for(DavAttributPrognoseObjekt attribut:this.attributePuffer){
							attribut.aktualisiere(resultat);
						}
						
						if(resultat.getData() != null){
							aktuellKeineDaten = false;
													
							/**
							 * Baue geglaetteten Ergebniswert zusammen:
							 */
							glaettungNutzdaten = DAV.createData(this.prognoseObjekt.getPubAtgGlatt());
							for(DavAttributPrognoseObjekt attribut:this.attributePuffer){
								attribut.exportiereDatenGlatt(glaettungNutzdaten);
							}
							this.fuegeKBPHinzu(glaettungNutzdaten, false);
							
							/**
							 * Baue Prognosewert zusammen:
							 */
							prognoseNutzdaten = DAV.createData(this.prognoseObjekt.getPubAtgPrognose());
							for(DavAttributPrognoseObjekt attribut:this.attributePuffer){
								attribut.exportiereDatenPrognose(prognoseNutzdaten);
							}
							this.fuegeKBPHinzu(prognoseNutzdaten, true);
							
							if(this.prognoseObjekt.isFahrStreifen()){
								long T = resultat.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
								glaettungNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
								prognoseNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
							}

						}else{						
							if(aktuellKeineDaten){
								datenSenden = false;
							}
							aktuellKeineDaten = true;
						}
					}catch(PrognoseParameterException e){
						LOGGER.error("Prognosedaten koennen fuer " + this.prognoseObjekt.getObjekt() //$NON-NLS-1$ 
								+ " nicht berechnet werden", e); //$NON-NLS-1$
						e.printStackTrace();
						
						if(aktuellKeineDaten){
							datenSenden = false;
						}						
						aktuellKeineDaten = true;
					}

					if(datenSenden){
						ResultData glaettungsDatum = new ResultData(this.prognoseObjekt.getObjekt(),
																	this.pubBeschreibungGlatt, 
																	resultat.getDataTime(),
																	glaettungNutzdaten);
						ResultData prognoseDatum = new ResultData(this.prognoseObjekt.getObjekt(),
																	this.pubBeschreibungPrognose, 
																	resultat.getDataTime(),
																	prognoseNutzdaten);

						try {
							if(this.sendeGeglaetteteDaten){
								DAV.sendData(glaettungsDatum);
							}else{
								LOGGER.fine("Geglaettete Daten fuer " + this.prognoseObjekt +  //$NON-NLS-1$
										" koennen nicht versendet werden (Kein Abnehmer)"); //$NON-NLS-1$
							}
						} catch (Exception e) {
							LOGGER.error("Geglaettete Daten konnten nicht gesendet werden: " + glaettungsDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
						
						try{
							if(this.sendePrognoseDaten){
								DAV.sendData(prognoseDatum);
							}else{
								LOGGER.fine("Prognosedaten fuer " + this.prognoseObjekt +  //$NON-NLS-1$
										" koennen nicht versendet werden (Kein Abnehmer)"); //$NON-NLS-1$								
							}
						} catch (Exception e) {
							LOGGER.error("Prognosedaten konnten nicht gesendet werden: " + prognoseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Fuegt einem errechneten Prognosedatum bzw. einem geglaetteten Datum den
	 * Wert <code>k(K)BP</code> bzw. <code>k(K)BG</code> hinzu 
	 * 
	 * @param zielDatum ein veraenderbares Zieldatum der Attributgruppe
	 * @param prognose ob ein Prognosewert veraendert werden soll
	 */
	private final void fuegeKBPHinzu(Data zielDatum, final boolean prognose){
		DaMesswertUnskaliert qb = null;
		DaMesswertUnskaliert vKfz = null;
		MesswertUnskaliert kb = null;
		GWert gueteVKfz = null;
		GWert guetekb = null;
		
		if(prognose){
			qb = new DaMesswertUnskaliert(PrognoseAttribut.QB.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen()), zielDatum);
			vKfz = new DaMesswertUnskaliert(PrognoseAttribut.V_KFZ.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen()), zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen()));
			guetekb = new GWert(zielDatum, PrognoseAttribut.QB.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen()));
			gueteVKfz = new GWert(zielDatum, PrognoseAttribut.V_KFZ.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen()));
		}else{
			qb = new DaMesswertUnskaliert(PrognoseAttribut.QB.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen()), zielDatum);
			vKfz = new DaMesswertUnskaliert(PrognoseAttribut.V_KFZ.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen()), zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen()));
			guetekb = new GWert(zielDatum, PrognoseAttribut.QB.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen()));
			gueteVKfz = new GWert(zielDatum, PrognoseAttribut.V_KFZ.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen()));
		}

		kb.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		if(vKfz.getWertUnskaliert() > 0){
			if(qb.getWertUnskaliert() >= 0){
				kb.setWertUnskaliert(Math.round( (double)qb.getWertUnskaliert() /
												 (double)vKfz.getWertUnskaliert() ));
				if(DUAUtensilien.isWertInWerteBereich(
						zielDatum.getItem(kb.getName()).getItem("Wert"), kb.getWertUnskaliert())){ //$NON-NLS-1$
					kb.setWertUnskaliert(kb.getWertUnskaliert());
					kb.setInterpoliert(qb.isPlausibilisiert() || vKfz.isPlausibilisiert());
				}
			}			
		}

		GWert guete = GWert.getNichtErmittelbareGuete(gueteVKfz.getVerfahren());
		try {
			guete = GueteVerfahren.quotient(guetekb, gueteVKfz);
		} catch (GueteException e) {
			LOGGER.error("Guete von " + qb.getName() + " fuer " +  //$NON-NLS-1$ //$NON-NLS-2$
					this.prognoseObjekt + " konnte nicht berechnet werden", e); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		kb.getGueteIndex().setWert(guete.getIndexUnskaliert());
		
		kb.kopiereInhaltNach(zielDatum);		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object,
							DataDescription dataDescription,
							byte state) {
		if(dataDescription.getAttributeGroup().equals(this.pubBeschreibungGlatt.getAttributeGroup())){
			this.sendeGeglaetteteDaten = state == ClientSenderInterface.START_SENDING;
		}else
		if(dataDescription.getAttributeGroup().equals(this.pubBeschreibungPrognose.getAttributeGroup())){
			this.sendePrognoseDaten = state == ClientSenderInterface.START_SENDING;
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object,
									  DataDescription dataDescription) {
		return true;
	}
	
	
	/**
	 * Abstrakte Methoden
	 */
	
	/**
	 * Erfragt den Typ dieses Prognoseobjektes (über diesen ist definiert,
	 * welche Parameter-Attributgruppen zur Anwendung kommen)
	 * 
	 * @return der Typ dieses Prognoseobjektes
	 */
	protected abstract PrognoseTyp getPrognoseTyp();
	
}
