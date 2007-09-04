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
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.AbstraktAttributPrognoseObjekt;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer die
 * Ermittlung des Störfallindikators <code>Fundamentaldiagramm</code> fuer 
 * <code>KKfzStoerfall</code> durch und ermittlt so <code>KKfzStoerfallG</code> 
 * (Prognosedichte) 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class KKfzStoerfallGErmittler 
extends AbstraktAttributPrognoseObjekt
implements ClientReceiverInterface{

	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param obj das Objekt, dessen Daten hier betrachtet werden
	 */
	protected KKfzStoerfallGErmittler(ClientDavInterface dav,
									  SystemObject obj){		
		dav.subscribeReceiver(this, obj,
				new DataDescription(
						dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalMq"), //$NON-NLS-1$
						dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL),
						(short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}
	
	
	/**
	 * Erfragt das geglaettete Attribut <code>KKfzStoerfallG</code>
	 *
	 * @param KKfzStoerfall zu glaettendes Attribut <code>KKfzStoerfall</code>
	 * @return das geglaettete Attribut <code>KKfzStoerfallG</code>
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzt wurden
	 */
	public final double getKKfzStoerfallGAktuell(double KKfzStoerfall)
	throws PrognoseParameterException{
		this.berechneGlaettungsParameterUndStart(Math.round(KKfzStoerfall), false);
		
		return this.ZG;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] parameterSaetze) {
		if(parameterSaetze != null){
			for(ResultData parameter:parameterSaetze){
				if(parameter != null){
					if(parameter.getData() != null){			
						this.ZAlt =  parameter.getData().getUnscaledValue("KKfzStart").longValue(); //$NON-NLS-1$
						this.alpha1 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("alpha1").doubleValue(); //$NON-NLS-1$
						this.alpha2 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("alpha2").doubleValue(); //$NON-NLS-1$
						this.beta1 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("beta1").doubleValue(); //$NON-NLS-1$
						this.beta2 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("beta2").doubleValue(); //$NON-NLS-1$
					}else{
						this.ZAlt = -4;
						this.alpha1 = -1;
						this.alpha2 = -1;
						this.beta1 = -1;
						this.beta2 = -1;
					} 					
				}
			}
		}
	}

}
