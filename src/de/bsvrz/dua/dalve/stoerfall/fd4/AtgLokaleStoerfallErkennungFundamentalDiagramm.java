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

import java.util.HashMap;
import java.util.Map;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.StoerfallSituation;

/**
 * Stellt die aktuellen Informationen der Attributgruppe 
 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
 * fuer einen bestimmten Messquerschnitt zur Verfuegung 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgLokaleStoerfallErkennungFundamentalDiagramm 
extends AllgemeinerDatenContainer
implements ClientReceiverInterface{

	/**
	 * Mappt eine Stoerfallsituation auf ihre Parameter
	 */
	private Map<StoerfallSituation, ParameterFuerStoerfall> parameter =
							new HashMap<StoerfallSituation, ParameterFuerStoerfall>();
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekt Systemobjekt des betrachteten Messquerschnittes 
	 */
	protected AtgLokaleStoerfallErkennungFundamentalDiagramm(final ClientDavInterface dav, 
															final SystemObject objekt){
		this.parameter.put(StoerfallSituation.STAU,
				new ParameterFuerStoerfall(StoerfallSituation.STAU));
		this.parameter.put(StoerfallSituation.ZAEHER_VERKEHR,
				new ParameterFuerStoerfall(StoerfallSituation.ZAEHER_VERKEHR));
		
		dav.subscribeReceiver(this,
				objekt,
				new DataDescription(
						dav.getDataModel().getAttributeGroup("atg.lokaleStörfallErkennungFundamentalDiagramm"), //$NON-NLS-1$
						dav.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_SOLL),
						(short)0),
				ReceiveOptions.normal(),
				ReceiverRole.receiver());		
	}
	
	
	/**
	 * Erfragt die aktuellen Parameter der Attributgruppe 
	 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 * fuer einen bestimmten Stoerfall
	 * 
	 * @param situation ein bestimmter Stoerfall
	 * @return die aktuellen Parameter der Attributgruppe 
	 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 * fuer den Stoerfall
	 */
	protected final ParameterFuerStoerfall getParameterFuerStoerfall(
			final StoerfallSituation situation){
		return this.parameter.get(situation);
	}
	

	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					for(ParameterFuerStoerfall parameterPuffer:this.parameter.values()){
						parameterPuffer.importiere(resultat.getData());
					}
				}
			}
		}
	}
	
}
