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
package de.bsvrz.dua.dalve.stoerfall.rds3;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dua.dalve.prognose.PrognoseSystemObjekt;
import de.bsvrz.dua.dalve.stoerfall.nrw2.NrwStoerfallIndikatorMq;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;

/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren RDS (nur fuer Messquerschnitte)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class RdsStoerfallIndikator 
extends NrwStoerfallIndikatorMq{
	
	/**
	 * Hysteregeschwindigkeit für die Ermittlung von zähfließendem Verkehr
	 */
	protected long VST5Hysterese = -4;

	/**
	 * Hysteregeschwindigkeit für die Ermittlung von Stau
	 */
	protected long VST6Hysterese = -4;
	
	/**
	 * letzter empfangener Analysedatensatz
	 */
	protected ResultData analyseDatensatz = null;
	
	/**
	 * letzter empfangener geglaetteter Datensatz
	 */
	protected ResultData geglaettetDatensatz = null;
	
	/**
	 * letzte ermittelte Verkehrsstufe
	 */
	protected StoerfallSituation letzteStufe = StoerfallSituation.KEINE_AUSSAGE;



	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(ClientDavInterface dav, 
							  PrognoseSystemObjekt objekt)
	throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);
		
		dav.subscribeReceiver(this, this.objekt.getObjekt(),
				new DataDescription(objekt.getAnalyseAtg(),
									dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
									(short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.verkehrsLageVerfahren3"; //$NON-NLS-1$
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenRDS"; //$NON-NLS-1$
	}

	
	/**
	 * Errechnet die aktuelle Verkehrsstufe anhand der Parameter Verkehrsdichte und
	 * KFZ-Geschwindigkeit 
	 * 
	 * @param kvst Verkehrsdichte
	 * @param vvst KFZ-Geschwindigkeit
	 * @return die aktuelle Verkehrsstufe 
	 */
	@Override
	protected StoerfallSituation getVerkehrsStufe(double kvst, double vvst){
		StoerfallSituation verkehrsStufe = StoerfallSituation.KEINE_AUSSAGE;
		
		if(vvst > 0 && vvst <= v1 - VST6Hysterese){
			if(kvst > 0 && kvst <= k3){
				if(this.letzteStufe.equals(StoerfallSituation.KEINE_AUSSAGE) ||
				   this.letzteStufe.equals(StoerfallSituation.STOERUNG)) {
					verkehrsStufe = StoerfallSituation.STAU;
				}else{
					verkehrsStufe = this.letzteStufe;
				}
			}else
			if(kvst > k3){
				verkehrsStufe = StoerfallSituation.STAU;
			}						
		}else
		if(vvst > v1 - VST6Hysterese && vvst <= v1 + VST6Hysterese){
			if(this.letzteStufe.equals(StoerfallSituation.STAU)){
				verkehrsStufe = StoerfallSituation.STAU;
			}else{
				verkehrsStufe = this.getVerkehrsStufe(kvst, v1 + VST6Hysterese + 1);
			}
		}else
		if(vvst > v1 + VST6Hysterese && vvst <= v2 - VST5Hysterese){
			if(kvst > 0 && kvst <= kT){
				if(this.letzteStufe.equals(StoerfallSituation.KEINE_AUSSAGE) ||
				   this.letzteStufe.equals(StoerfallSituation.STAU) ||
				   this.letzteStufe.equals(StoerfallSituation.STOERUNG) ){
					verkehrsStufe = StoerfallSituation.ZAEHER_VERKEHR;
				}else{
					verkehrsStufe = this.letzteStufe;
				}
			}else
			if(kvst > kT){
				verkehrsStufe = StoerfallSituation.ZAEHER_VERKEHR;
			}			
		}else
		if(vvst > v2 - VST5Hysterese && vvst <= v2 + VST5Hysterese){
			if(this.letzteStufe.equals(StoerfallSituation.ZAEHER_VERKEHR)){
				verkehrsStufe = StoerfallSituation.ZAEHER_VERKEHR;
			}else{
				verkehrsStufe = this.getVerkehrsStufe(kvst, v2 + VST5Hysterese + 1);
			}			
		}else
		if(vvst > v2 + VST5Hysterese){
			if(kvst > 0 && kvst <= k1){
				verkehrsStufe = StoerfallSituation.FREIER_VERKEHR;
			}else
			if(kvst > k1 && kvst <= k2){
				verkehrsStufe = StoerfallSituation.LEBHAFTER_VERKEHR;
			}else
			if(kvst > k2){
				verkehrsStufe = StoerfallSituation.DICHTER_VERKEHR;
			}
		}
		
		return verkehrsStufe;
	}
	

	/**
	 * {@inheritDoc}
	 */
	protected void readParameter(ResultData parameter) {
		if(parameter.getData() != null){
			this.v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			this.v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			this.k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			this.k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$
			this.k3 = parameter.getData().getUnscaledValue("k3").longValue(); //$NON-NLS-1$
			this.kT = parameter.getData().getUnscaledValue("kT").longValue(); //$NON-NLS-1$
			this.VST5Hysterese = parameter.getData().getUnscaledValue("VST5Hysterese").longValue(); //$NON-NLS-1$
			this.VST6Hysterese = parameter.getData().getUnscaledValue("VST6Hysterese").longValue(); //$NON-NLS-1$
			
			/**
			 * Konsitenz-Check
			 */
			if( !(v1 > 0 && v1 < v2) ){
				LOGGER.warning("Fehlerhafte Parameter (0<v1<v2) empfangen fuer " + //$NON-NLS-1$
						this.objekt + ": v1 = " + v1 + ", v2 = " + v2);  //$NON-NLS-1$//$NON-NLS-2$
			}
			if( !(k1 > 0 && k1 < k2 && k2 < k3) ){
				LOGGER.warning("Fehlerhafte Parameter (0<k1<k2<k3) empfangen fuer " + //$NON-NLS-1$
						this.objekt + ": k1 = " + k1 + ", k2 = " + k2 + ", k3 = " + k3);  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
			if( !(kT > 0 && kT < k3) ){
				LOGGER.warning("Fehlerhafte Parameter (0<kT<k3) empfangen fuer " + //$NON-NLS-1$
						this.objekt + ": kT = " + kT + ", k3 = " + k3);  //$NON-NLS-1$//$NON-NLS-2$
			}							
		}else{
			this.v1 = -4;
			this.v2 = -4;
			this.k1 = -4;
			this.k2 = -4;
			this.k3 = -4;
			this.kT = -4;
			this.VST5Hysterese = -4;
			this.VST6Hysterese = -4;
		} 
	}
}