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
package de.bsvrz.dua.dalve.stoerfall.fd;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.common.OneSubscriptionPerSendData;
import stauma.dav.configuration.interfaces.AttributeGroup;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallSystemObjekt;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.StoerfallSituation;

/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren Fundamentaldiagramm
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class FdStoerfallIndikator
extends AbstraktStoerfallIndikator{
		
	/**
	 * Faktor für die Ermittlung der Analysedichte
	 */
	private double fa = -1;
	
	/**
	 * Faktor für die Ermittlung der Prognosedichte
	 */
	private double fp = -1;

	/**
	 * Absoluter Wert für den Vergleich mit VKfzStörfall
	 */
	private double Vgrenz = -4;
	
	/**
	 * Hysteresewert für Vgrenz (VGrenz +/- VgrenzHysterese)
	 */
	private double VgrenzHysterese = -4;
	
	/**
	 * Faktor für den Vergleich von KKfzStörfallG mid K0
	 */
	private double fk = -1;
	
	/**
	 * Hysteresewert für fk(fk +/- fkHysterese)
	 */
	private double fkHysterese = -1;
	
	/**
	 * Faktor für den Vergleich von VKfzStörfallG mid V0
	 */
	private double fv = -1;
	
	/**
	 * Hysteresewert für fv(fv +/- fkHysterese)
	 */
	private double fvHysterese = -1;
	
	/**
	 * Verkehrsmenge des Fundamentaldiagramms
	 */
	private double Q0 = -4;
	
	/**
	 * Maximale Dichte des Fundamentaldiagramms
	 */
	private double K0 = -4;
	
	/**
	 * V0-Geschwindigkeit des Fundamentaldiagramms
	 */
	private double V0 = -4;
	
	/**
	 * Freie Geschwindigkeit des Fundamentaldiagramms
	 */
	private double VFrei = -4;

	/**
	 * Atg <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 */
	private AttributeGroup paraAtgLokal = null;
	
	/**
	 * Objekt, das die Prognosedichte ermittelt
	 */
	private KKfzStoerfallGErmittler prognoseDichteObj = null;
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(ClientDavInterface dav, 
							  StoerfallSystemObjekt objekt)
	throws DUAInitialisierungsException {
		if(DAV == null){
			DAV = dav;
		}
		this.objekt = objekt;
		
		this.paraAtg = dav.getDataModel().getAttributeGroup(this.getParameterAtgPid());
		dav.subscribeReceiver(this, objekt.getObjekt(),
				new DataDescription(this.paraAtg, dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL), (short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());
		this.paraAtgLokal = dav.getDataModel().getAttributeGroup("atg.lokaleStörfallErkennungFundamentalDiagramm"); //$NON-NLS-1$
		dav.subscribeReceiver(this, objekt.getObjekt(),
				new DataDescription(this.paraAtgLokal, dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL), (short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());		
		this.prognoseDichteObj = new KKfzStoerfallGErmittler(dav, objekt.getObjekt()); 
		
		this.pubBeschreibung = new DataDescription(
				dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_STOERFALL_ZUSTAND),
				dav.getDataModel().getAspect(this.getPubAspektPid()),
				(short)0);
		try {
			dav.subscribeSender(this, objekt.getObjekt(), this.pubBeschreibung, SenderRole.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
		}
		
		/**
		 * Anmeldung auf Daten (hier Analysedaten)
		 */
		dav.subscribeReceiver(this, objekt.getObjekt(),
				new DataDescription(this.objekt.getAnalyseAtg(),
						dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
						(short)0),
						ReceiveOptions.normal(), ReceiverRole.receiver());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.fundamentalDiagramm"; //$NON-NLS-1$
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenFD"; //$NON-NLS-1$
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(ResultData[] resultate) {
		if(resultate != null) {
			for(ResultData resultat:resultate){
				if(resultat != null){
					if(resultat.getDataDescription().getAttributeGroup().getId() == this.paraAtg.getId()){
						/**
						 * Parameter empfangen
						 */
						this.readParameter(resultat);
					}else
					if(resultat.getDataDescription().getAttributeGroup().getId() == this.paraAtgLokal.getId()){
						/**
						 * Parameter empfangen
						 */
						this.readParameterLokal(resultat);
					}else{
						/**
						 * Daten empfangen
						 */
						this.berechneStoerfallIndikator(resultat);							
					}
				}
			}
		}	
	}


	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Analysedaten
	 * analog SE-02.00.00.00.00-AFo-4.0 (S.160 f) - Fundamentaldiagramm
	 * 
	 * @param resultat ein empfangenes Analysedatum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat){
		Data data = null;
		
		if(resultat.getData() != null){						
			StoerfallSituation situation = StoerfallSituation.KEINE_AUSSAGE;
			
			if(this.alleParameterValide()){
				data = DAV.createData(this.pubBeschreibung.getAttributeGroup());
				
				double KKfzStoerfall = this.getAnalyseDichte(resultat);
				double KKfzStoerfallG = Double.NaN;
				try {
					KKfzStoerfallG = this.prognoseDichteObj.getKKfzStoerfallGAktuell(KKfzStoerfall);
				} catch (PrognoseParameterException e) {
					LOGGER.error(Konstante.LEERSTRING, e);
					e.printStackTrace();
				}
				
				if(KKfzStoerfallG == Double.NaN){
					double VKfzStoerfallG = 0;
					if(KKfzStoerfallG < this.fp * K0){
						if(K0 > 0){
							VKfzStoerfallG = VFrei - ( (VFrei - V0) / K0 ) * KKfzStoerfallG;
						}else{
							VKfzStoerfallG = VFrei;
						}					 
					}else{
						if(KKfzStoerfallG != 0){
							VKfzStoerfallG = Q0 * K0 / Math.pow(KKfzStoerfallG, 2.0);
						}		
					}
								
	//				Beantwortung der Frage vom 4.09. abwarten:
	//				
	//				if((KKfzStoerfallG > fk * K0) && (VKfzStoerfallG < fv * V0)){
	//					situation = StoerfallSituation.STAU;
	//				}
	//				if( Vgrenz != 0 &&	// Wenn Vgrenz == 0, dann soll kein Grenzwertvergleich durchgefuehrt werden 
	//					VKfzStoerfallG < Vgrenz ){
	//					
	//				}
					
					StoerfallZustand zustand = new StoerfallZustand(DAV);
					zustand.setInfrastrukturObjekt(this.objekt.getInfrastrukturObjekt());
					zustand.setHorizont(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
//					zustand.setSituation(stufe);
					data = zustand.getData();
				}
			}else{
				LOGGER.warning("Keine gueltigen Parameter fuer Stoerfallprognose: " + this.objekt); //$NON-NLS-1$
			}
			
			// TODO Daten in Stoerfalldatum einfuegen
		}
		
		ResultData ergebnis = new ResultData(this.objekt.getObjekt(), 
				this.pubBeschreibung, resultat.getDataTime(), data);
		this.sendeErgebnis(ergebnis);
	}

	
	/**
	 * Erfragt die Analysedichte zur Störfallerkennung <code>KKfzStoerfall</code>.
	 * Die Berechnung erfolgt analog SE-02.00.00.00.00-AFo-4.0 (siehe 6.6.4.3.2.1.2)
	 * 
	 * @param resultat ein Analysedatum des MQs (muss <code> != null</code> sein und
	 * Nutzdaten enthalten)
	 * @return die Analysedichte zur Störfallerkennung <code>KKfzStoerfall</code>
	 */
	private final double getAnalyseDichte(ResultData resultat){
		double KKfzStoerfall;
		
		double QKfz = resultat.getData().getItem("QKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		double VKfz = resultat.getData().getItem("VKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		double KKfz = resultat.getData().getItem("KKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		
		if(QKfz == 0){
			KKfzStoerfall = 0;
		}else{
			if(VKfz == 0 || VKfz == DUAKonstanten.NICHT_ERMITTELBAR){
				KKfzStoerfall = this.K0;
			}else
			if(VKfz >= this.fa * this.V0){
				KKfzStoerfall = KKfz;
			}else{
				if(QKfz > 0){
					KKfzStoerfall = Math.min(K0 * Q0 / QKfz, 2.0 * K0);	
				}else{
					KKfzStoerfall = 2.0 * K0;
				}			
			}			
		}
		
		return KKfzStoerfall;
	}
	
	
	/**
	 * Erfragt, ob bereits alle Parameter initialisiert wurden und sie auf 
	 * gültigen (verarbeitbaren) Werten stehen
	 * 
	 * @return ob bereits alle Parameter initialisiert wurden und sie auf 
	 * gültigen (verarbeitbaren) Werten stehen
	 */
	private final boolean alleParameterValide(){
		return this.fa >= 0 && this.fp >= 0 && this.Vgrenz >= 0 && this.VgrenzHysterese >= 0 &&
		       this.fk >= 0 && this.fkHysterese >= 0 && this.fv >= 0 &&	this.fvHysterese >= 0 && 
		       this.Q0 >= 0 && this.K0 >= 0 && this.V0 >= 0 && this.VFrei >= 0;
	}
	
	
	/**
	 * Liest einen Parametersatz der Atg <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 * 
	 * @param parameter einen Parametersatz
	 */
	private void readParameterLokal(ResultData parameter) {
		if(parameter.getData() != null){			
			this.fa = parameter.getData().getScaledValue("fa").doubleValue(); //$NON-NLS-1$
			this.fp = parameter.getData().getScaledValue("fp").doubleValue(); //$NON-NLS-1$
			this.Vgrenz = parameter.getData().getUnscaledValue("Vgrenz").longValue(); //$NON-NLS-1$
			this.VgrenzHysterese = parameter.getData().getUnscaledValue("VgrenzHysterese").longValue(); //$NON-NLS-1$
			this.fk = parameter.getData().getScaledValue("fk").doubleValue(); //$NON-NLS-1$
			this.fkHysterese = parameter.getData().getScaledValue("fkHysterese").doubleValue(); //$NON-NLS-1$
			this.fv = parameter.getData().getScaledValue("fv").doubleValue(); //$NON-NLS-1$
			this.fvHysterese = parameter.getData().getScaledValue("fvHysterese").doubleValue(); //$NON-NLS-1$
		}else{
			this.fa = -1;
			this.fp = -1;
			this.Vgrenz = -4;
			this.VgrenzHysterese = -4;			
			this.fk = -1;
			this.fkHysterese = -1;
			this.fv = -1;
			this.fvHysterese = -1;
		} 
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readParameter(ResultData parameter) {
		if(parameter.getData() != null){
			this.Q0 = parameter.getData().getUnscaledValue("Q0").longValue(); //$NON-NLS-1$
			this.K0 = parameter.getData().getUnscaledValue("K0").longValue(); //$NON-NLS-1$
			this.V0 = parameter.getData().getUnscaledValue("V0").longValue(); //$NON-NLS-1$
			this.VFrei = parameter.getData().getUnscaledValue("VFrei").longValue(); //$NON-NLS-1$
		}else{
			this.Q0 = -4;
			this.K0 = -4;
			this.V0 = -4;
			this.VFrei = -4;
		} 
	}
}
