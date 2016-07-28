/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * Copyright 2015 by Kappich Systemberatung Aachen
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.
 * 
 * de.bsvrz.dua.dalve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */
package de.bsvrz.dua.dalve.stoerfall.fd4;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.AbstraktAttributPrognoseObjekt;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;

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
						dav.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_SOLL)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}
	
	
	/**
	 * Erfragt das geglaettete Attribut <code>KKfzStoerfallG</code>
	 *
	 * @param KKfzStoerfall zu glaettendes Attribut <code>KKfzStoerfall</code>
	 * @param implausibel ob das zu glaettende Attribut <code>KKfzStoerfall</code> als implausibel
	 * gekennzeichnet wird
	 * @return das geglaettete Attribut <code>KKfzStoerfallG</code>
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzt wurden
	 */
	public final double getKKfzStoerfallGAktuell(double KKfzStoerfall, boolean implausibel)
	throws PrognoseParameterException{
		this.berechneGlaettungsParameterUndStart(Math.round(KKfzStoerfall), implausibel, false, null);
		
		return this.getZG();
	}

	
	public void update(ResultData[] parameterSaetze) {
		if(parameterSaetze != null){
			for(ResultData parameter:parameterSaetze){
				if(parameter != null){
					if(parameter.getData() != null){			
						this.ZAltInit =  parameter.getData().getUnscaledValue("KKfzStart").longValue(); //$NON-NLS-1$
						this.alpha1 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("alpha1").doubleValue(); //$NON-NLS-1$
						this.alpha2 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("alpha2").doubleValue(); //$NON-NLS-1$
						this.beta1 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("beta1").doubleValue(); //$NON-NLS-1$
						this.beta2 = parameter.getData().getItem("KKfz"). //$NON-NLS-1$
										getScaledValue("beta2").doubleValue(); //$NON-NLS-1$
					}else{
						this.ZAltInit = -4;
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
