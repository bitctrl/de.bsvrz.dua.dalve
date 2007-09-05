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
package de.bsvrz.dua.dalve.stoerfall.fd4;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.common.OneSubscriptionPerSendData;
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
	 * Objekt, das die Prognosedichte ermittelt
	 */
	private KKfzStoerfallGErmittler prognoseDichteObj = null;
	
	/**
	 * Parameter der Attributgruppe <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 */
	private AtgLokaleStoerfallErkennungFundamentalDiagramm parameterLokal = null;
	
	/**
	 * der Zuatsnd, der zum Zeitpunkt t-T errechnet wurde
	 */
	private StoerfallSituation alterZustand = StoerfallSituation.KEINE_AUSSAGE;
	
	
	
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
				new DataDescription(
						this.paraAtg,
						dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL),
						(short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());
		
		this.prognoseDichteObj = new KKfzStoerfallGErmittler(dav, objekt.getObjekt()); 
		this.parameterLokal = new AtgLokaleStoerfallErkennungFundamentalDiagramm(dav, objekt.getObjekt());
		
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
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Analysedaten
	 * analog SE-02.00.00.00.00-AFo-4.0 (S.160 f) - Fundamentaldiagramm
	 * 
	 * @param resultat ein empfangenes Analysedatum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat){
		Data data = null;
		
		if(resultat.getData() != null){						
			StoerfallSituation stufe = StoerfallSituation.KEINE_AUSSAGE;
			
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
					stufe = StoerfallSituation.FREIER_VERKEHR;
					stufe = berechneStufe(StoerfallSituation.ZAEHER_VERKEHR, KKfzStoerfallG, stufe);
					stufe = berechneStufe(StoerfallSituation.STAU, KKfzStoerfallG, stufe);
										
					StoerfallZustand zustand = new StoerfallZustand(DAV);
					zustand.setInfrastrukturObjekt(this.objekt.getInfrastrukturObjekt());
					zustand.setHorizont(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
					zustand.setSituation(stufe);
					data = zustand.getData();
				}
			}else{
				LOGGER.warning("Keine gueltigen Parameter fuer Stoerfallprognose: " + this.objekt); //$NON-NLS-1$
			}
			
			this.alterZustand = stufe;
		}
		
		ResultData ergebnis = new ResultData(this.objekt.getObjekt(), 
				this.pubBeschreibung, resultat.getDataTime(), data);
		this.sendeErgebnis(ergebnis);
	}

	
	/**
	 * Berechnet, ob die uebergebene Stoerfallsituation gerade anliegt
	 * 
	 * @param stufe die Stoerfallsituation, deren Existenz zu ueberpruefen ist
	 * @param KKfzStoerfallG das geglaettete Attribut <code>KKfzStoerfall</code>
	 * @param stufeAlt die Stoerfallsituation die bereits detektiert wurde
	 * @return die Stoerfallsituation deren Anliegen ueberprueft werden sollte, wenn
	 * diese tatsaechlich anliegt, oder die Stoerfallsituation die bereits detektiert 
	 * wurde, sonst
	 */
	private final StoerfallSituation berechneStufe(final StoerfallSituation stufe,
												   final double KKfzStoerfallG,
												   final StoerfallSituation stufeAlt){
		StoerfallSituation ergebnis = stufeAlt;
		
		ParameterFuerStoerfall parameter = this.parameterLokal.getParameterFuerStoerfall(stufe);
		if(parameter.isInitialisiert()){
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
			
			boolean fkVergleichMachen = parameter.getFk() != 0;
			boolean fvVergleichMachen = parameter.getFv() != 0;			
			boolean vGrenzVergleichMachen = parameter.getVgrenz() != 0;

			boolean fkVergleichsErgebnis;
			boolean fvVergleichsErgebnis;			
			boolean vGrenzVergleichsErgebnis;

			if(this.alterZustand.equals(stufe)){
				/**
				 * Ausschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > (parameter.getFk() - parameter.getFkHysterese()) * K0;
				fvVergleichsErgebnis = VKfzStoerfallG < (parameter.getFv() + parameter.getFvHysterese()) * V0;
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter.getVgrenz() + parameter.getVgrenzHysterese());				
			}else{
				/**
				 * Einschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > (parameter.getFk() + parameter.getFkHysterese()) * K0;
				fvVergleichsErgebnis = VKfzStoerfallG < (parameter.getFv() - parameter.getFvHysterese()) * V0;
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter.getVgrenz() - parameter.getVgrenzHysterese());				
			}
			
			if(this.getErgebnisAusBoolscherFormel(
					fkVergleichMachen, fvVergleichMachen, vGrenzVergleichMachen,
					fkVergleichsErgebnis, fvVergleichsErgebnis, vGrenzVergleichsErgebnis)){
				ergebnis = stufe;
			}
		}
		
		return ergebnis;
	}
	
	
	/**
	 * Berechnet die boolesche Formel:<br><code>
	 * ergebnis := fkVergleichsErgebnis & fvVergleichsErgebnis | vGrenzVergleichsErgebnis
	 * </code><br>
	 * wobei jeweils nur die Teile in der Formel verbleiben, die als "zu machen" uebergeben
	 * wurden
	 *  
	 * @param fkVergleichMachen Indikator fuer die Existenz des 1. Terms
	 * @param fvVergleichMachen Indikator fuer die Existenz des 2. Terms
	 * @param vGrenzVergleichMachen Indikator fuer die Existenz des 3. Terms
	 * @param fkVergleichsErgebnis Wert des 1. Terms
	 * @param fvVergleichsErgebnis Wert des 2. Terms
	 * @param vGrenzVergleichsErgebnis Wert des 3. Terms
	 * @return Ergebnis der Verknuepfung der drei Werte ueber die manipulierte Formel
	 */
	private final boolean getErgebnisAusBoolscherFormel(boolean fkVergleichMachen,
														boolean fvVergleichMachen,
														boolean vGrenzVergleichMachen,
														boolean fkVergleichsErgebnis,
														boolean fvVergleichsErgebnis,
														boolean vGrenzVergleichsErgebnis){
		boolean ergebnis;
		
		if(!fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen){
			ergebnis = false; 
		}else
		if(!fkVergleichMachen && !fvVergleichMachen && vGrenzVergleichMachen){
			ergebnis = vGrenzVergleichsErgebnis;
		}else
		if(!fkVergleichMachen && fvVergleichMachen && !vGrenzVergleichMachen){
			ergebnis = fvVergleichsErgebnis;
		}else
		if(!fkVergleichMachen && fvVergleichMachen && vGrenzVergleichMachen){
			ergebnis = fvVergleichsErgebnis || vGrenzVergleichsErgebnis;	
		}else

		if(fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen){
			ergebnis = fkVergleichsErgebnis;
		}else
		if(fkVergleichMachen && !fvVergleichMachen && vGrenzVergleichMachen){
			ergebnis = fkVergleichsErgebnis || vGrenzVergleichsErgebnis;
		}else
		if(fkVergleichMachen && fvVergleichMachen && !vGrenzVergleichMachen){
			ergebnis = fkVergleichsErgebnis && fvVergleichsErgebnis;
		}else{
			ergebnis = fkVergleichsErgebnis && fvVergleichsErgebnis || vGrenzVergleichsErgebnis;
		}

		return ergebnis;
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
		return this.fa >= 0 && this.fp >= 0 && 
		       this.Q0 >= 0 && this.K0 >= 0 &&
		       this.V0 >= 0 && this.VFrei >= 0;
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
